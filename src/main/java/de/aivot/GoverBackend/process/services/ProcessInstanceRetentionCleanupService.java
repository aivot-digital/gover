package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.mail.services.ExceptionMailService;
import de.aivot.GoverBackend.process.enums.ProcessInstanceStatus;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@EnableScheduling
public class ProcessInstanceRetentionCleanupService {
    private static final Logger logger = LoggerFactory.getLogger(ProcessInstanceRetentionCleanupService.class);
    private static final int DEFAULT_CLEANUP_BATCH_SIZE = 500;

    private final ProcessInstanceRepository processInstanceRepository;
    private final ProcessInstanceService processInstanceService;
    private final ExceptionMailService exceptionMailService;
    private final int cleanupBatchSize;
    private final AtomicBoolean cleanupRunning = new AtomicBoolean(false);

    public ProcessInstanceRetentionCleanupService(ProcessInstanceRepository processInstanceRepository,
                                                  ProcessInstanceService processInstanceService,
                                                  ExceptionMailService exceptionMailService,
                                                  @Value("${gover.process-instance-retention-cleanup.batch-size:500}") Integer cleanupBatchSize) {
        this.processInstanceRepository = processInstanceRepository;
        this.processInstanceService = processInstanceService;
        this.exceptionMailService = exceptionMailService;
        this.cleanupBatchSize = cleanupBatchSize != null && cleanupBatchSize > 0
                ? cleanupBatchSize
                : DEFAULT_CLEANUP_BATCH_SIZE;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void cleanDueProcessInstancesOnStartup() {
        cleanDueProcessInstances("startup");
    }

    @Scheduled(cron = "0 0 10 * * *", zone = "Europe/Berlin")
    public void cleanDueProcessInstancesNightly() {
        cleanDueProcessInstances("daily-schedule");
    }

    private void cleanDueProcessInstances(String trigger) {
        if (!cleanupRunning.compareAndSet(false, true)) {
            logger.info("Skipping process instance retention cleanup because another run is still active.");
            return;
        }

        try {
            var now = LocalDateTime.now();
            var dueProcessInstanceCount = processInstanceRepository
                    .countByStatusAndKeepUntilLessThanEqual(
                            ProcessInstanceStatus.Completed,
                            now
                    );

            if (dueProcessInstanceCount == 0) {
                logger.debug("No due process instances found for retention cleanup (trigger={}).", trigger);
                return;
            }

            var dueProcessInstances = processInstanceRepository
                    .findAllByStatusAndKeepUntilLessThanEqual(
                            ProcessInstanceStatus.Completed,
                            now,
                            PageRequest.of(
                                    0,
                                    cleanupBatchSize,
                                    Sort.by(Sort.Order.asc("keepUntil"), Sort.Order.asc("id"))
                            )
                    );

            if (dueProcessInstances.isEmpty()) {
                logger.info(
                        "Skipping process instance retention cleanup because no due instances were loaded after counting {} candidate(s) (trigger={}).",
                        dueProcessInstanceCount,
                        trigger
                );
                return;
            }

            logger.info(
                    "Starting process instance retention cleanup for {} due instance(s); deleting up to {} in this run (trigger={}).",
                    dueProcessInstanceCount,
                    dueProcessInstances.size(),
                    trigger
            );

            var deletedProcessInstanceCount = 0;
            for (var processInstance : dueProcessInstances) {
                try {
                    processInstanceService.deleteEntity(processInstance);
                    deletedProcessInstanceCount++;

                    logger.info(
                            "Deleted retained process instance {} with keep-until timestamp {}.",
                            processInstance.getId(),
                            processInstance.getKeepUntil()
                    );
                } catch (Exception e) {
                    logger.error(
                            "Failed to delete retained process instance {}.",
                            processInstance.getId(),
                            e
                    );
                    exceptionMailService.send(e);
                }
            }

            var remainingDueProcessInstanceCount = Math.max(dueProcessInstanceCount - deletedProcessInstanceCount, 0);
            logger.info(
                    "Finished process instance retention cleanup: deleted {} due instance(s), {} due instance(s) remain for future runs (trigger={}, batchSize={}).",
                    deletedProcessInstanceCount,
                    remainingDueProcessInstanceCount,
                    trigger,
                    cleanupBatchSize
            );
        } finally {
            cleanupRunning.set(false);
        }
    }
}
