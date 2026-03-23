package de.aivot.GoverBackend.process.workers;

import de.aivot.GoverBackend.process.entities.ProcessInstanceEventEntity;
import de.aivot.GoverBackend.process.enums.ProcessHistoryEventType;
import de.aivot.GoverBackend.process.enums.ProcessInstanceStatus;
import de.aivot.GoverBackend.process.repositories.ProcessNodeRepository;
import de.aivot.GoverBackend.process.repositories.ProcessRepository;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceHistoryEventRepository;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceRepository;
import de.aivot.GoverBackend.process.services.ProcessNodeDefinitionService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@EnableScheduling
public class ProcessStarter {
    private final ProcessInstanceRepository processInstanceRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ProcessInstanceHistoryEventRepository processInstanceHistoryEventRepository;
    private final ProcessRepository processDefinitionRepository;
    private final ProcessNodeRepository processDefinitionNodeRepository;
    private final ProcessNodeDefinitionService processNodeProviderService;

    @Autowired
    public ProcessStarter(ProcessInstanceRepository processInstanceRepository,
                          RabbitTemplate rabbitTemplate,
                          ProcessInstanceHistoryEventRepository processInstanceHistoryEventRepository,
                          ProcessRepository processDefinitionRepository,
                          ProcessNodeRepository processDefinitionNodeRepository,
                          ProcessNodeDefinitionService processNodeProviderService) {
        this.processInstanceRepository = processInstanceRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.processInstanceHistoryEventRepository = processInstanceHistoryEventRepository;
        this.processDefinitionRepository = processDefinitionRepository;
        this.processDefinitionNodeRepository = processDefinitionNodeRepository;
        this.processNodeProviderService = processNodeProviderService;
    }

    @Scheduled(fixedRate = 1000 * 10) // every 10 seconds
    public void startProcesses() {
        var allUnstartedProcesses = processInstanceRepository
                .findAllByStatus(ProcessInstanceStatus.Created);

        for (var processInstance : allUnstartedProcesses) {
            var initialNode = processDefinitionNodeRepository
                    .findById(processInstance.getInitialNodeId())
                    .orElseThrow(RuntimeException::new);

            var payload = new ProcessWorker.WorkerPayload(
                    processInstance.getId(),
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

            /*
            processInstanceHistoryEventRepository.save(new ProcessInstanceEventEntity(
                    null,
                    ProcessHistoryEventType.Start,
                    "Vorgang gestartet",
                    "Der Vorgang den Prozess „%s“ wurde gestartet und das erste Prozesselement „%s“ wird ausgeführt."
                            .formatted(process.getName(), initialNode.resolveName(provider)),
                    Map.of(),
                    LocalDateTime.now(),
                    null,
                    processInstance.getId(),
                    null
            ));

             */
        }
    }
}
