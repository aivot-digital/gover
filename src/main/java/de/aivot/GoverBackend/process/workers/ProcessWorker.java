package de.aivot.GoverBackend.process.workers;

import de.aivot.GoverBackend.process.entities.ProcessInstanceHistoryEventEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.enums.ProcessHistoryEventType;
import de.aivot.GoverBackend.process.enums.ProcessTaskStatus;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionResult;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionResultError;
import de.aivot.GoverBackend.process.repositories.ProcessNodeRepository;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceHistoryEventRepository;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceRepository;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.GoverBackend.process.services.ProcessDataService;
import de.aivot.GoverBackend.process.services.ProcessNodeDefinitionService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@Service
public class ProcessWorker {
    public static final String DO_WORK_ON_INSTANCE_QUEUE = "do-work-on-instance-queue";

    private final ProcessInstanceRepository processInstanceRepository;
    private final ProcessNodeRepository processDefinitionNodeRepository;
    private final ProcessNodeDefinitionService processNodeProviderService;
    private final ProcessInstanceTaskRepository processInstanceTaskRepository;
    private final ProcessNodeExecutionResultHandler processNodeExecutionResultHandler;
    private final ProcessDataService processDataService;
    private final ProcessInstanceHistoryEventRepository processInstanceHistoryEventRepository;

    @Autowired
    public ProcessWorker(ProcessInstanceRepository processInstanceRepository,
                         ProcessNodeRepository processDefinitionNodeRepository,
                         ProcessNodeDefinitionService processNodeProviderService,
                         ProcessInstanceTaskRepository processInstanceTaskRepository,
                         ProcessNodeExecutionResultHandler processNodeExecutionResultHandler,
                         ProcessDataService processDataService,
                         ProcessInstanceHistoryEventRepository processInstanceHistoryEventRepository) {
        this.processInstanceRepository = processInstanceRepository;
        this.processDefinitionNodeRepository = processDefinitionNodeRepository;
        this.processNodeProviderService = processNodeProviderService;
        this.processInstanceTaskRepository = processInstanceTaskRepository;
        this.processNodeExecutionResultHandler = processNodeExecutionResultHandler;
        this.processDataService = processDataService;
        this.processInstanceHistoryEventRepository = processInstanceHistoryEventRepository;
    }

    @Bean
    public Queue doWorkOnInstanceQueue() {
        return new Queue(DO_WORK_ON_INSTANCE_QUEUE, true);
    }

    @RabbitListener(queues = DO_WORK_ON_INSTANCE_QUEUE)
    public void listen(WorkerPayload payload) {
        try {
            process(
                    payload.processInstanceId(),
                    payload.previousNodeId(),
                    payload.nextNodeId()
            );
        } catch (Exception error) {
            processInstanceHistoryEventRepository.save(ProcessInstanceHistoryEventEntity.from(
                    payload.processInstanceId(),
                    null,
                    null,
                    error
            ));
            // TODO: Create notification for failed process execution
        }
    }

    private void process(@Nonnull Long processInstanceId, @Nullable Integer previousNodeId, @Nonnull Integer nodeId) {
        var processInstance = processInstanceRepository
                .findById(processInstanceId)
                .orElseThrow(() -> new RuntimeException(
                        "Der Vorgang mit der ID „%d“ wurde nicht gefunden.".formatted(processInstanceId)
                ));

        var currentNode = processDefinitionNodeRepository
                .findById(nodeId)
                .orElseThrow(() -> new RuntimeException(
                        "Das Prozesselement mit der ID „%d“ wurde nicht gefunden.".formatted(nodeId)
                ));

        var currentNodeProvider = processNodeProviderService
                .getProcessNodeDefinition(currentNode.getProcessNodeDefinitionKey(), currentNode.getProcessNodeDefinitionVersion())
                .orElseThrow(() -> new RuntimeException(
                        "Die Prozesselementdefinition mit dem Schlüssel „%s“ und der Version „%d“ wurde nicht gefunden."
                                .formatted(currentNode.getProcessNodeDefinitionKey(), currentNode.getProcessNodeDefinitionVersion())
                ));

        var deadline = currentNode.getTimeLimitDays() != null ?
                LocalDateTime.now().plusDays(currentNode.getTimeLimitDays()) :
                null;

        var taskEntity = processInstanceTaskRepository.save(
                new ProcessInstanceTaskEntity(
                        null,
                        UUID.randomUUID(),
                        processInstance.getId(),
                        processInstance.getProcessId(),
                        processInstance.getProcessVersion(),
                        currentNode.getId(),
                        previousNodeId,
                        ProcessTaskStatus.Running,
                        null,
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        null,
                        null,
                        new HashMap<>(),
                        new HashMap<>(),
                        new HashMap<>(),
                        null,
                        deadline
                )
        );

        processInstanceHistoryEventRepository.save(new ProcessInstanceHistoryEventEntity(
                null,
                ProcessHistoryEventType.Start,
                "Aufgabe gestartet",
                "Die Aufgabe für das Prozesselement „%s“ wurde gestartet.".formatted(currentNode.resolveName(currentNodeProvider)),
                Map.of(),
                LocalDateTime.now(),
                null,
                processInstance.getId(),
                taskEntity.getId()
        ));

        try {
            var processData = processDataService
                    .foldProcessInstanceData(
                            processInstance,
                            previousNodeId
                    );

            ProcessNodeExecutionResult res;
            try {
                res = currentNodeProvider
                        .init(
                                processInstance,
                                currentNode,
                                processData
                        );
            } catch (Exception e) {
                throw e;
            }

            if (res == null) {
                throw new RuntimeException(
                        "Der Prozessknoten-Funktionsanbieter „%s“ für das Prozesselement „%s“ lieferte kein Ergebnis zurück."
                                .formatted(currentNodeProvider.getName(), currentNode.resolveName(currentNodeProvider))
                );
            }

            ProcessInstanceTaskEntity previousTask;
            if (previousNodeId != null) {
                previousTask = processInstanceTaskRepository
                        .findFirstByProcessInstanceIdAndProcessNodeIdOrderByStartedDesc(
                                processInstance.getId(),
                                previousNodeId
                        );
            } else {
                previousTask = null;
            }

            processNodeExecutionResultHandler
                    .handleResult(
                            null,
                            currentNodeProvider,
                            currentNode,
                            processInstance,
                            taskEntity,
                            previousTask,
                            res
                    );
        } catch (Exception e) {
            processNodeExecutionResultHandler
                    .handleResult(
                            null,
                            currentNodeProvider,
                            currentNode,
                            processInstance,
                            taskEntity,
                            null,
                            ProcessNodeExecutionResultError.of(e)
                    );
        }
    }

    public record WorkerPayload(
            @Nonnull Long processInstanceId,
            @Nullable Integer previousNodeId,
            @Nonnull Integer nextNodeId
    ) implements Serializable {

    }
}
