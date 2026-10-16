package de.aivot.prosuna.backend.process.workers;

import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessWorkRequestEntity;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.enums.ProcessWorkRequestKind;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessWorkRequestRepository;
import de.aivot.prosuna.backend.process.services.ProcessTaskExecutionService;
import de.aivot.prosuna.backend.process.services.ProcessWorkRequestService;
import de.aivot.prosuna.backend.utils.DatabaseFailureUtils;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Publishes and runs the pending process work stored in {@code process_work_requests}.
 *
 * <p>The database rows are the only record of the work; RabbitMQ only carries their IDs. The consumer never hands
 * a failure back to the broker: a request whose work cannot run yet, e.g. because the database is temporarily
 * unavailable, stays pending and is published again by the dispatcher, and any other failure fails the instance, so
 * staff decide whether to restart it.
 */
@Service
public class ProcessWorkRequestWorker {
    private static final Logger logger = LoggerFactory.getLogger(ProcessWorkRequestWorker.class);
    // Paused and failed instances only hold their requests, so they are published once the instance runs again.
    // Completed and aborted instances are still published, so the consumer discards their requests.
    static final List<ProcessInstanceStatus> DISPATCHABLE_INSTANCE_STATUSES = List.of(ProcessInstanceStatus.Running,
            ProcessInstanceStatus.Completed, ProcessInstanceStatus.Aborted);
    // A paused task cannot be resumed, so its requests are held until it runs again instead of being declined
    // after every redispatch interval.
    static final List<ProcessTaskStatus> HELD_TASK_STATUSES = List.of(ProcessTaskStatus.Paused);

    private final ProcessWorkRequestRepository requests;
    private final ProcessWorkRequestService requestService;
    private final ProcessInstanceRepository instances;
    private final ProcessInstanceTaskRepository tasks;
    private final ProcessTaskExecutionService execution;
    private final ProcessWorker worker;
    private final BusinessTime time;
    private final int batchSize;
    private final Duration redispatchInterval;
    // A new broker connection may lead to a broker that has lost all messages, e.g. after the broker was recreated.
    private final AtomicBoolean publishAllPending = new AtomicBoolean(true);

    public ProcessWorkRequestWorker(@Nonnull ProcessWorkRequestRepository requests,
                                    @Nonnull ProcessWorkRequestService requestService,
                                    @Nonnull ProcessInstanceRepository instances,
                                    @Nonnull ProcessInstanceTaskRepository tasks,
                                    @Nonnull ProcessTaskExecutionService execution, @Nonnull ProcessWorker worker,
                                    @Nonnull BusinessTime time, @Nonnull ConnectionFactory connectionFactory,
                                    @Value("${prosuna.process-work.batch-size:100}") int batchSize,
                                    @Value("${prosuna.process-work.redispatch-interval:PT5M}") Duration redispatchInterval) {
        this.requests = requests;
        this.requestService = requestService;
        this.instances = instances;
        this.tasks = tasks;
        this.execution = execution;
        this.worker = worker;
        this.time = time;
        this.batchSize = Math.max(1, batchSize);
        this.redispatchInterval = redispatchInterval;
        connectionFactory.addConnectionListener(connection -> publishAllPending.set(true));
    }

    @Bean
    @Nonnull
    public Queue processWorkRequestQueue() {
        return new Queue(ProcessWorkRequestService.QUEUE, true);
    }

    /**
     * Each consumer prefetches a single request, so a consumer that runs a slow step, e.g. a call to an external
     * service, does not hold back requests that the other consumers could run.
     */
    @Bean
    @Nonnull
    public static SimpleRabbitListenerContainerFactory processWorkListenerContainerFactory(
            @Nonnull SimpleRabbitListenerContainerFactoryConfigurer configurer,
            @Nonnull ConnectionFactory connectionFactory) {
        var factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setPrefetchCount(1);
        return factory;
    }

    @Scheduled(fixedDelayString = "${prosuna.process-work.dispatch-interval:PT30S}")
    public void dispatch() {
        var publishAll = publishAllPending.getAndSet(false);
        var now = time.now();
        // A published ID stays queued until it is consumed. Publishing it again is only a fallback for requests that
        // remain pending, e.g. after a lost message or a held request, and must not pile up duplicates.
        var dispatchedBefore = publishAll ? now : now.minus(redispatchInterval);
        long afterId = 0;
        try {
            while (true) {
                var ids = requests.findDispatchableIds(afterId, dispatchedBefore, DISPATCHABLE_INSTANCE_STATUSES,
                        HELD_TASK_STATUSES, PageRequest.of(0, batchSize));
                if (ids.isEmpty()) return;
                for (var id : ids) {
                    // The claim keeps other application instances from publishing the same request again.
                    if (requests.claimDispatch(id, dispatchedBefore, now) != 1) continue;
                    if (!requestService.publish(id)) {
                        // The broker is most likely unavailable, so this pass stops. The next pass publishes the
                        // request again instead of waiting for the redispatch interval.
                        requests.releaseDispatch(id);
                        if (publishAll) publishAllPending.set(true);
                        return;
                    }
                }
                afterId = ids.getLast();
            }
        } catch (RuntimeException exception) {
            // Publish everything again once the database can be read, because this pass did not complete.
            if (publishAll) publishAllPending.set(true);
            throw exception;
        }
    }

    @RabbitListener(queues = ProcessWorkRequestService.QUEUE, concurrency = "${prosuna.process-work.concurrency:4}",
            containerFactory = "processWorkListenerContainerFactory")
    public void consume(@Nonnull Long requestId) {
        ProcessWorkRequestEntity request = null;
        try {
            request = requests.findById(requestId).orElse(null);
            if (request == null) return;
            if (request.getKind() == ProcessWorkRequestKind.Resume) {
                consumeResume(request);
            } else {
                consumeLocked(requestId, request.getProcessInstanceId());
            }
        } catch (ResponseException exception) {
            // The instance or task no longer exists; its requests were deleted with it.
        } catch (RuntimeException exception) {
            // The work rolled back, so the request remains pending. A temporarily unavailable database, e.g. a lost
            // connection or a failed commit, only delays the work until the request is published again.
            if (request == null || DatabaseFailureUtils.isTransient(exception)) {
                logger.warn("Could not run process work request {}; it remains pending", requestId, exception);
                return;
            }
            // Running the work again would fail again, so the failed instance holds the request until it is restarted.
            var taskId = request.getKind() == ProcessWorkRequestKind.Resume ? request.getTaskId() : null;
            try {
                worker.recordFailedExecution(request.getProcessInstanceId(), taskId, exception);
            } catch (RuntimeException recordingException) {
                exception.addSuppressed(recordingException);
                logger.error("Could not record the failure of process work request {}", requestId, exception);
            }
        }
    }

    private void consumeLocked(long requestId, long instanceId) throws ResponseException {
        execution.inInstance(instanceId, () -> {
            // A concurrent consumer may have run this request while this consumer waited for the lock.
            var request = requests.findById(requestId).orElse(null);
            if (request == null) return null;
            var instance = instances.findById(instanceId).orElseThrow(ResponseException::notFound);
            if (instance.getStatus() == ProcessInstanceStatus.Completed || instance.getStatus() == ProcessInstanceStatus.Aborted) {
                requests.delete(request);
                return null;
            }
            if (instance.getStatus() != ProcessInstanceStatus.Running) return null;
            var done = switch (request.getKind()) {
                case Start -> start(instance);
                case Transition -> transition(instance, request);
                case Restart -> restart(instance, request);
                case Resume -> throw new IllegalStateException("Resume requests are run as task executions.");
            };
            // Keep the request if the target definition failed before a task could be created; the failed instance
            // holds it until it is restarted.
            if (done) requests.delete(request);
            return null;
        });
    }

    private boolean start(@Nonnull ProcessInstanceEntity instance) {
        if (!tasks.existsByProcessInstanceId(instance.getId())) {
            worker.workOnNodeLocked(new ProcessWorker.DoWorkWorkerPayload(instance.getId(), null, null, null,
                    instance.getInitialNodeId(), null));
        }
        return tasks.existsByProcessInstanceId(instance.getId());
    }

    private boolean transition(@Nonnull ProcessInstanceEntity instance, @Nonnull ProcessWorkRequestEntity request) {
        var predecessor = tasks.findById(request.getTaskId()).orElse(null);
        // The request is committed with the predecessor's final status, so a mismatch indicates inconsistent data.
        if (predecessor == null || !instance.getId().equals(predecessor.getProcessInstanceId())
                || (predecessor.getStatus() != ProcessTaskStatus.Completed
                && predecessor.getStatus() != ProcessTaskStatus.DeadlineExceeded)) {
            logger.warn("Discarding process work request {}: task {} cannot be continued", request.getId(), request.getTaskId());
            return true;
        }
        if (!tasks.existsByPreviousProcessInstanceTaskId(predecessor.getId())) {
            worker.workOnNodeLocked(new ProcessWorker.DoWorkWorkerPayload(instance.getId(), predecessor.getId(),
                    predecessor.getProcessNodeId(), request.getViaPort(), request.getToNodeId(), null));
        }
        return tasks.existsByPreviousProcessInstanceTaskId(predecessor.getId());
    }

    private boolean restart(@Nonnull ProcessInstanceEntity instance, @Nonnull ProcessWorkRequestEntity request) {
        var previousAttempt = tasks.findById(request.getTaskId()).orElse(null);
        // The request is committed with the task's Restarted status, so a mismatch indicates inconsistent data.
        if (previousAttempt == null || !instance.getId().equals(previousAttempt.getProcessInstanceId())
                || previousAttempt.getStatus() != ProcessTaskStatus.Restarted) {
            logger.warn("Discarding process work request {}: task {} cannot be restarted", request.getId(), request.getTaskId());
            return true;
        }
        if (!tasks.existsByRestartForTaskId(previousAttempt.getId())) {
            worker.workOnNodeLocked(new ProcessWorker.DoWorkWorkerPayload(instance.getId(),
                    previousAttempt.getPreviousProcessInstanceTaskId(), previousAttempt.getPreviousProcessNodeId(),
                    previousAttempt.getPreviousProcessNodePortKey(), previousAttempt.getProcessNodeId(),
                    previousAttempt.getId()));
        }
        return tasks.existsByRestartForTaskId(previousAttempt.getId());
    }

    /**
     * Resumes as a task execution, which applies due deadlines first. It is not nested in another instance
     * transaction, because a failing participant would mark that outer transaction rollback-only.
     */
    private void consumeResume(@Nonnull ProcessWorkRequestEntity request) throws ResponseException {
        var requestId = request.getId();
        var instanceId = request.getProcessInstanceId();
        var taskId = request.getTaskId();
        var resumed = execution.executeBackground(instanceId, taskId, () -> {
            // A concurrent consumer may have run this request while this consumer waited for the lock.
            if (!requests.existsById(requestId)) return null;
            worker.resumeLocked(instanceId, taskId);
            requests.deleteById(requestId);
            return null;
        });
        if (!resumed) settleDeclinedResume(requestId, instanceId, taskId);
    }

    private void settleDeclinedResume(@Nonnull Long requestId, @Nonnull Long instanceId, @Nonnull Long taskId)
            throws ResponseException {
        execution.inInstance(instanceId, () -> {
            var request = requests.findById(requestId).orElse(null);
            if (request == null) return null;
            var instance = instances.findById(instanceId).orElseThrow(ResponseException::notFound);
            var task = tasks.findById(taskId).orElse(null);
            if (task != null && task.getStatus() == ProcessTaskStatus.DeadlineExceeded) {
                worker.recordResumeAfterDeadline(instanceId, taskId);
                requests.delete(request);
            } else if (task == null || task.getStatus().isTerminal()
                    || instance.getStatus() == ProcessInstanceStatus.Completed
                    || instance.getStatus() == ProcessInstanceStatus.Aborted) {
                requests.delete(request);
            }
            // Otherwise the instance does not run or the task is paused; the dispatcher holds the request until both
            // can run again.
            return null;
        });
    }
}
