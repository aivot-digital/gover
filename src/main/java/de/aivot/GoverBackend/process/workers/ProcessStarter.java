package de.aivot.GoverBackend.process.workers;

import de.aivot.GoverBackend.process.enums.ProcessInstanceStatus;
import de.aivot.GoverBackend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.GoverBackend.process.repositories.ProcessNodeRepository;
import de.aivot.GoverBackend.process.repositories.ProcessRepository;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceRepository;
import de.aivot.GoverBackend.process.services.ProcessNodeDefinitionService;
import de.aivot.GoverBackend.process.services.ProcessNodeExecutionLoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@EnableScheduling
public class ProcessStarter {
    private final ProcessInstanceRepository processInstanceRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ProcessRepository processDefinitionRepository;
    private final ProcessNodeRepository processDefinitionNodeRepository;
    private final ProcessNodeDefinitionService processNodeProviderService;
    private final ProcessNodeExecutionLoggerFactory processNodeExecutionLoggerFactory;

    @Autowired
    public ProcessStarter(ProcessInstanceRepository processInstanceRepository,
                          RabbitTemplate rabbitTemplate,
                          ProcessRepository processDefinitionRepository,
                          ProcessNodeRepository processDefinitionNodeRepository,
                          ProcessNodeDefinitionService processNodeProviderService,
                          ProcessNodeExecutionLoggerFactory processNodeExecutionLoggerFactory) {
        this.processInstanceRepository = processInstanceRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.processDefinitionRepository = processDefinitionRepository;
        this.processDefinitionNodeRepository = processDefinitionNodeRepository;
        this.processNodeProviderService = processNodeProviderService;
        this.processNodeExecutionLoggerFactory = processNodeExecutionLoggerFactory;
    }

    @Scheduled(fixedRate = 1000 * 10) // every 10 seconds
    public void startProcesses() {
        var allUnstartedProcesses = processInstanceRepository
                .findAllByStatus(ProcessInstanceStatus.Created);

        for (var processInstance : allUnstartedProcesses) {
            var logger = processNodeExecutionLoggerFactory
                    .create(processInstance.getId(), null, null, null);

            try {
                var initialNode = processDefinitionNodeRepository
                        .findById(processInstance.getInitialNodeId())
                        .orElseThrow(RuntimeException::new);

                var payload = new ProcessWorker.WorkerPayload(
                        processInstance.getId(),
                        null,
                        null,
                        null,
                        initialNode.getId()
                );

                processInstance.setStatus(ProcessInstanceStatus.Running);
                processInstanceRepository.save(processInstance);

                rabbitTemplate.convertAndSend(
                        ProcessWorker.DO_WORK_ON_INSTANCE_QUEUE,
                        payload
                );

                var process = processDefinitionRepository
                        .findById(processInstance.getProcessId())
                        .orElseThrow(RuntimeException::new);

                var provider = processNodeProviderService
                        .getProcessNodeDefinition(initialNode.getProcessNodeDefinitionKey(), initialNode.getProcessNodeDefinitionVersion())
                        .orElseThrow(RuntimeException::new);

                logger.logf(
                        ProcessNodeExecutionLogLevel.Info,
                        true,
                        true,
                        "Vorgang für Prozess '%s' gestartet. Erstes Prozesselement: '%s'.",
                        process.getInternalTitle(),
                        initialNode.resolveName(provider)
                );
            } catch (Exception e) {
                logger.logException(e);
                processInstance.setStatus(ProcessInstanceStatus.Failed);
                processInstanceRepository.save(processInstance);
            }
        }
    }
}
