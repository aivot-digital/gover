package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.mail.services.ExceptionMailService;
import de.aivot.GoverBackend.process.enums.ProcessInstanceStatus;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceRepository;
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

    private final ProcessInstanceRepository processInstanceRepository;
    private final ProcessInstanceService processInstanceService;
    private final ExceptionMailService exceptionMailService;
    private final AtomicBoolean cleanupRunning = new AtomicBoolean(false);

    public ProcessInstanceRetentionCleanupService(ProcessInstanceRepository processInstanceRepository,
                                                  ProcessInstanceService processInstanceService,
                                                  ExceptionMailService exceptionMailService) {
        this.processInstanceRepository = processInstanceRepository;
        this.processInstanceService = processInstanceService;
        this.exceptionMailService = exceptionMailService;
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
            var dueProcessInstances = processInstanceRepository
                    .findAllByStatusAndKeepUntilLessThanEqual(
                            ProcessInstanceStatus.Completed,
                            LocalDateTime.now()
                    );

            if (dueProcessInstances.isEmpty()) {
                logger.debug("No due process instances found for retention cleanup (trigger={}).", trigger);
                return;
            }

            logger.info(
                    "Starting process instance retention cleanup for {} due instance(s) (trigger={}).",
                    dueProcessInstances.size(),
                    trigger
            );

            for (var processInstance : dueProcessInstances) {
                try {
                    processInstanceService.deleteEntity(processInstance);

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
        } finally {
            cleanupRunning.set(false);
        }
    }
}
