package de.aivot.prosuna.backend.process.workers;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessNodeRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessRepository;
import de.aivot.prosuna.backend.process.services.ProcessNodeDefinitionService;
import de.aivot.prosuna.backend.process.services.ProcessNodeExecutionLoggerFactory;
import de.aivot.prosuna.backend.process.services.ProcessTaskExecutionService;
import de.aivot.prosuna.backend.process.services.ProcessWorkRequestService;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@EnableScheduling
public class ProcessStarter {
    private final Logger logger = LoggerFactory.getLogger(ProcessStarter.class);

    private final ProcessInstanceRepository processInstanceRepository;
    private final ProcessRepository processDefinitionRepository;
    private final ProcessNodeRepository processDefinitionNodeRepository;
    private final ProcessNodeDefinitionService processNodeProviderService;
    private final ProcessNodeExecutionLoggerFactory processNodeExecutionLoggerFactory;
    private final ProcessTaskExecutionService execution;
    private final ProcessWorkRequestService workRequests;

    @Autowired
    public ProcessStarter(ProcessInstanceRepository processInstanceRepository,
                          ProcessRepository processDefinitionRepository,
                          ProcessNodeRepository processDefinitionNodeRepository,
                          ProcessNodeDefinitionService processNodeProviderService,
                          ProcessNodeExecutionLoggerFactory processNodeExecutionLoggerFactory,
                          ProcessTaskExecutionService execution,
                          ProcessWorkRequestService workRequests) {
        this.processInstanceRepository = processInstanceRepository;
        this.processDefinitionRepository = processDefinitionRepository;
        this.processDefinitionNodeRepository = processDefinitionNodeRepository;
        this.processNodeProviderService = processNodeProviderService;
        this.processNodeExecutionLoggerFactory = processNodeExecutionLoggerFactory;
        this.execution = execution;
        this.workRequests = workRequests;
    }

    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.SECONDS) // every 5 seconds
    public void startProcesses() {
        logger.info("Suche nach neuen Prozessen zum Starten...");

        var allUnstartedProcesses = processInstanceRepository
                .findAllByStatus(ProcessInstanceStatus.Created);

        for (var processInstance : allUnstartedProcesses) {
            try {
                execution.inInstance(processInstance.getId(), () -> {
                    startLocked(processInstance.getId());
                    return null;
                });
            } catch (ResponseException exception) {
                // The instance was deleted before it could be started.
            } catch (Exception exception) {
                // The instance remains created, so the next run tries to start it again.
                logger.error("Could not start process instance {}", processInstance.getId(), exception);
            }
        }
    }

    /**
     * Starting the instance and requesting its first task commit together, so a lost message cannot strand a
     * running instance without tasks.
     */
    private void startLocked(@Nonnull Long processInstanceId) {
        var processInstance = processInstanceRepository
                .findById(processInstanceId)
                .orElse(null);
        // Another application instance may have started it while this one waited for the lock.
        if (processInstance == null || processInstance.getStatus() != ProcessInstanceStatus.Created) {
            return;
        }

        var logger = processNodeExecutionLoggerFactory
                .create(processInstance.getId(), null, null, null);

        try {
            var initialNode = processDefinitionNodeRepository
                    .findById(processInstance.getInitialNodeId())
                    .orElseThrow(RuntimeException::new);

            var process = processDefinitionRepository
                    .findById(processInstance.getProcessId())
                    .orElseThrow(RuntimeException::new);

            var provider = processNodeProviderService
                    .getProcessNodeDefinition(initialNode.getProcessNodeDefinitionKey(), initialNode.getProcessNodeDefinitionVersion())
                    .orElseThrow(RuntimeException::new);

            processInstance.setStatus(ProcessInstanceStatus.Running);
            processInstanceRepository.save(processInstance);
            workRequests.enqueueStart(processInstance.getId());

            logger.logf(
                    ProcessNodeExecutionLogLevel.Info,
                    true,
                    true,
                    "Neuer Vorgang " + StringUtils.quote(process.getInternalTitle()) + " gestartet",
                    "Ein neuer Vorgang für den Prozess %s wurde gestartet. Die erste Aufgabe wird für das Prozesselement %s gestartet.",
                    StringUtils.quote(process.getInternalTitle()),
                    StringUtils.quote(initialNode.resolveName(provider))
            );
        } catch (Exception e) {
            logger.logException(e);
            processInstance.setStatus(ProcessInstanceStatus.Failed);
            processInstanceRepository.save(processInstance);
        }
    }
}
