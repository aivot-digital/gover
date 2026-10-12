package de.aivot.prosuna.backend.process.workers;

import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessTaskDeadlineContinuationRepository;
import de.aivot.prosuna.backend.process.services.ProcessTaskDeadlineService;
import de.aivot.prosuna.backend.process.services.ProcessTaskExecutionService;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.UnexpectedRollbackException;

@Service
public class ProcessTaskDeadlineWorker {
    public static final String CONTINUATION_QUEUE = "process-task-deadline-continuation-queue";
    private static final Logger logger = LoggerFactory.getLogger(ProcessTaskDeadlineWorker.class);
    private final ProcessInstanceTaskRepository tasks;
    private final ProcessInstanceRepository instances;
    private final ProcessTaskDeadlineContinuationRepository continuations;
    private final ProcessTaskExecutionService execution;
    private final ProcessWorker worker;
    private final RabbitTemplate rabbit;
    private final BusinessTime time;
    private final int batchSize;

    public ProcessTaskDeadlineWorker(@Nonnull ProcessInstanceTaskRepository tasks, @Nonnull ProcessInstanceRepository instances,
                                      @Nonnull ProcessTaskDeadlineContinuationRepository continuations,
                                      @Nonnull ProcessTaskExecutionService execution, @Nonnull ProcessWorker worker,
                                      @Nonnull RabbitTemplate rabbit, @Nonnull BusinessTime time,
                                      @Value("${prosuna.process-task-deadlines.batch-size:100}") int batchSize) {
        this.tasks = tasks;
        this.instances = instances;
        this.continuations = continuations;
        this.execution = execution;
        this.worker = worker;
        this.rabbit = rabbit;
        this.time = time;
        this.batchSize = Math.max(1, batchSize);
    }

    @Bean
    @Nonnull
    public Queue deadlineContinuationQueue() { return new Queue(CONTINUATION_QUEUE, true); }

    @Scheduled(fixedDelayString = "${prosuna.process-task-deadlines.check-interval:PT30S}")
    public void checkDeadlines() {
        var now = time.now();
        long afterId = 0;
        while (true) {
            var ids = tasks.findDueDeadlineIds(afterId, now, ProcessTaskDeadlineService.EXPIRABLE_STATUSES, PageRequest.of(0, batchSize));
            if (ids.isEmpty()) return;
            for (var id : ids) {
                try {
                    var instanceId = tasks.findInstanceIdById(id);
                    if (instanceId.isPresent()) execution.executeBackground(instanceId.get(), id, () -> null);
                } catch (Exception exception) {
                    logger.error("Could not process deadline of task {}", id, exception);
                }
            }
            // Keyset pagination also advances past soft deadlines and isolated failures.
            afterId = ids.getLast();
        }
    }

    @Scheduled(fixedDelayString = "${prosuna.process-task-deadlines.check-interval:PT30S}")
    public void dispatchContinuations() {
        long afterId = 0;
        while (true) {
            var pending = continuations.findByIdGreaterThanOrderByIdAsc(afterId, PageRequest.of(0, batchSize));
            if (pending.isEmpty()) return;
            for (var continuation : pending) {
                try {
                    rabbit.convertAndSend(CONTINUATION_QUEUE, continuation.getId());
                } catch (Exception exception) {
                    logger.warn("Could not dispatch deadline continuation {}", continuation.getId(), exception);
                }
            }
            afterId = pending.getLast().getId();
        }
    }

    @RabbitListener(queues = CONTINUATION_QUEUE)
    public void continueAfterDeadline(@Nonnull Long continuationId) throws Exception {
        var pending = continuations.findById(continuationId);
        if (pending.isEmpty()) return;
        var instanceId = pending.get().getProcessInstanceId();
        try {
            continueAfterDeadlineLocked(instanceId, continuationId);
        } catch (UnexpectedRollbackException exception) {
            // The continuation stays pending; the failed instance stops repeating the same rolled-back work.
            worker.recordRolledBackExecution(instanceId, null, exception);
        }
    }

    private void continueAfterDeadlineLocked(@Nonnull Long instanceId, @Nonnull Long continuationId) throws ResponseException {
        execution.inInstance(instanceId, () -> {
            // A concurrent consumer may have consumed this message while this consumer waited for the lock.
            var current = continuations.findById(continuationId);
            if (current.isEmpty()) return null;
            var continuation = current.get();
            var instance = instances.findById(continuation.getProcessInstanceId()).orElseThrow();
            if (instance.getStatus() == ProcessInstanceStatus.Completed || instance.getStatus() == ProcessInstanceStatus.Aborted) {
                continuations.delete(continuation);
                return null;
            }
            if (instance.getStatus() != ProcessInstanceStatus.Running) return null;
            if (!tasks.existsByPreviousProcessInstanceTaskId(continuation.getTaskId())) {
                worker.doWorkAfterDeadline(new ProcessWorker.DoWorkWorkerPayload(instance.getId(), continuation.getTaskId(),
                        continuation.getFromNodeId(), continuation.getViaPort(), continuation.getToNodeId(), null));
            }
            // Keep the request if the target definition failed before a child task could be created.
            if (tasks.existsByPreviousProcessInstanceTaskId(continuation.getTaskId())) continuations.delete(continuation);
            return null;
        });
    }
}
