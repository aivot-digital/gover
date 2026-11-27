package de.aivot.prosuna.backend.process.workers;

import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.prosuna.backend.process.services.ProcessTaskDeadlineService;
import de.aivot.prosuna.backend.process.services.ProcessTaskExecutionService;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Expires tasks whose hard deadline has passed. Expiry requests the continuation as process work, which
 * {@link ProcessWorkRequestWorker} runs.
 */
@Service
public class ProcessTaskDeadlineWorker {
    private static final Logger logger = LoggerFactory.getLogger(ProcessTaskDeadlineWorker.class);
    private final ProcessInstanceTaskRepository tasks;
    private final ProcessTaskExecutionService execution;
    private final BusinessTime time;
    private final int batchSize;

    public ProcessTaskDeadlineWorker(@Nonnull ProcessInstanceTaskRepository tasks,
                                     @Nonnull ProcessTaskExecutionService execution, @Nonnull BusinessTime time,
                                     @Value("${prosuna.process-task-deadlines.batch-size:100}") int batchSize) {
        this.tasks = tasks;
        this.execution = execution;
        this.time = time;
        this.batchSize = Math.max(1, batchSize);
    }

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
            // Keyset pagination also advances past tasks that turned out not to be due and isolated failures.
            afterId = ids.getLast();
        }
    }
}
