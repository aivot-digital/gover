package de.aivot.GoverBackend.process.workers;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceHistoryEventEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.enums.ProcessHistoryEventType;
import de.aivot.GoverBackend.process.enums.ProcessInstanceStatus;
import de.aivot.GoverBackend.process.enums.ProcessTaskStatus;
import de.aivot.GoverBackend.process.models.*;
import de.aivot.GoverBackend.process.repositories.ProcessEdgeRepository;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceHistoryEventRepository;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceRepository;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class ProcessNodeExecutionResultHandler {
    private final RabbitTemplate rabbitTemplate;
    private final ProcessInstanceRepository processInstanceRepository;
    private final ProcessInstanceTaskRepository processInstanceTaskRepository;
    private final ProcessEdgeRepository processDefinitionEdgeRepository;
    private final ProcessInstanceHistoryEventRepository processInstanceHistoryEventRepository;
    private final UserService userService;

    @Autowired
    public ProcessNodeExecutionResultHandler(RabbitTemplate rabbitTemplate,
                                             ProcessInstanceRepository processInstanceRepository,
                                             ProcessInstanceTaskRepository processInstanceTaskRepository,
                                             ProcessEdgeRepository processDefinitionEdgeRepository,
                                             ProcessInstanceHistoryEventRepository processInstanceHistoryEventRepository, UserService userService) {
        this.rabbitTemplate = rabbitTemplate;
        this.processInstanceRepository = processInstanceRepository;
        this.processInstanceTaskRepository = processInstanceTaskRepository;
        this.processDefinitionEdgeRepository = processDefinitionEdgeRepository;
        this.processInstanceHistoryEventRepository = processInstanceHistoryEventRepository;
        this.userService = userService;
    }

    public void handleResult(@Nullable UserEntity triggeringUser,
                             @Nonnull ProcessNodeDefinition provider,
                             @Nonnull ProcessNodeEntity currentNode,
                             @Nonnull ProcessInstanceEntity processInstance,
                             @Nonnull ProcessInstanceTaskEntity processInstanceTask,
                             @Nullable ProcessInstanceTaskEntity previousTask,
                             @Nullable ProcessNodeExecutionResult executionResult) {
        if (executionResult == null) {
            handleError(
                    triggeringUser,
                    provider,
                    currentNode,
                    processInstance,
                    processInstanceTask,
                    ProcessNodeExecutionResultError.of(
                            "Der Prozesselement-Funktionsanbieter „%s“ des Prozesselementes „%s“ hat ein null-Ergebnis zurückgegeben. Bitte überprüfen Sie die Implementierung des Prozesselement-Funktionsanbieters!"
                                    .formatted(provider.getName(), currentNode.resolveName(provider))
                    )
            );
            return;
        }

        switch (executionResult) {
            case ProcessNodeExecutionResultError err -> handleError(
                    triggeringUser,
                    provider,
                    currentNode,
                    processInstance,
                    processInstanceTask,
                    err
            );
            case ProcessNodeExecutionResultTaskCompleted taskCompleted -> handleTaskComplete(
                    triggeringUser,
                    provider,
                    currentNode,
                    processInstance,
                    processInstanceTask,
                    previousTask,
                    taskCompleted
            );
            case ProcessNodeExecutionResultInstanceCompleted instanceCompleted -> handleInstanceComplete(
                    triggeringUser,
                    provider,
                    currentNode,
                    processInstance,
                    processInstanceTask,
                    previousTask,
                    instanceCompleted
            );
            case ProcessNodeExecutionResultTaskAssigned assigned -> handleAssigned(
                    triggeringUser,
                    provider,
                    currentNode,
                    processInstance,
                    processInstanceTask,
                    previousTask,
                    assigned
            );
            default -> handleError(
                    triggeringUser,
                    provider,
                    currentNode,
                    processInstance,
                    processInstanceTask,
                    ProcessNodeExecutionResultError.of(
                            "Der Prozesselement-Funktionsanbieter „%s“ des Prozesselementes „%s“ hat eine unbekanntes Ergebnisklasse erzeugt: „%s“. Bitte überprüfen Sie die Implementierung des Prozesselement-Funktionsanbieters!"
                                    .formatted(provider.getName(), currentNode.resolveName(provider), executionResult.getClass().getName())
                    )
            );
        }
    }

    private void handleAssigned(@Nullable UserEntity triggeringUser,
                                @Nonnull ProcessNodeDefinition provider,
                                @Nonnull ProcessNodeEntity currentNode,
                                @Nonnull ProcessInstanceEntity processInstance,
                                @Nonnull ProcessInstanceTaskEntity processInstanceTask,
                                @Nullable ProcessInstanceTaskEntity previousTask,
                                @Nonnull ProcessNodeExecutionResultTaskAssigned assigned) {
        UserEntity assignedUser;
        try {
            assignedUser = userService
                    .retrieve(assigned.getAssignedUserId())
                    .orElse(null);
        } catch (ResponseException e) {
            handleError(
                    triggeringUser,
                    provider,
                    currentNode,
                    processInstance,
                    processInstanceTask,
                    ProcessNodeExecutionResultError.of(e)
            );
            return;
        }

        if (assignedUser == null) {
            handleError(
                    triggeringUser,
                    provider,
                    currentNode,
                    processInstance,
                    processInstanceTask,
                    ProcessNodeExecutionResultError.of(
                            "Die der Aufgabe zugewiesene Mitarbeiter:in mit der ID „%s“ konnte nicht gefunden werden."
                                    .formatted(assigned.getAssignedUserId())
                    )
            );
            return;
        }

        var newRuntimeData = assigned.getRuntimeData();
        if (newRuntimeData == null) {
            newRuntimeData = new HashMap<>();
        }
        processInstanceTask.setRuntimeData(newRuntimeData);

        var newMetadata = assigned.getNodeData();
        if (newMetadata == null) {
            newMetadata = new HashMap<>();
        }
        processInstanceTask.setNodeData(newMetadata);

        var newWorkingData = assigned.getProcessData();
        if (newWorkingData == null) {
            newWorkingData = previousTask != null ?
                    previousTask.getProcessData() :
                    processInstance.getInitialPayload();
        }
        processInstanceTask.setProcessData(newWorkingData);
        processInstanceTask.setAssignedUserId(assigned.getAssignedUserId());

        if (assigned.getTaskStatusOverride() != null) {
            processInstanceTask.setStatusOverride(assigned.getTaskStatusOverride());
        }
        if (Boolean.TRUE.equals(assigned.getClearTaskStatusOverride())) {
            processInstanceTask.setStatusOverride(null);
        }

        processInstanceTaskRepository.save(processInstanceTask);

        if (triggeringUser != null) {
            processInstanceHistoryEventRepository
                    .save(new ProcessInstanceHistoryEventEntity(
                            null,
                            ProcessHistoryEventType.Update,
                            "Zuordnung einer Aufgabenbearbeiter:in",
                            "Die Aufgabe wurde durch „%s“ der Mitarbeiter:in „%s“ zugewiesen".formatted(
                                    triggeringUser.getFullName(),
                                    assignedUser.getFullName()
                            ),
                            Map.of(),
                            LocalDateTime.now(),
                            triggeringUser.getId(),
                            processInstance.getId(),
                            processInstanceTask.getId()
                    ));
        } else {
            processInstanceHistoryEventRepository
                    .save(new ProcessInstanceHistoryEventEntity(
                            null,
                            ProcessHistoryEventType.Update,
                            "Zuordnung einer Aufgabenbearbeiter:in",
                            "Die Aufgabe wurde automatisch der Mitarbeiter:in „%s“ zugewiesen".formatted(
                                    assignedUser.getFullName()
                            ),
                            Map.of(),
                            LocalDateTime.now(),
                            null,
                            processInstance.getId(),
                            processInstanceTask.getId()
                    ));
        }

        // TODO: send notification
    }

    private void handleTaskComplete(@Nullable UserEntity triggeringUser,
                                    @Nonnull ProcessNodeDefinition provider,
                                    @Nonnull ProcessNodeEntity currentNode,
                                    @Nonnull ProcessInstanceEntity processInstance,
                                    @Nonnull ProcessInstanceTaskEntity processInstanceTask,
                                    @Nullable ProcessInstanceTaskEntity previousTask,
                                    ProcessNodeExecutionResultTaskCompleted taskCompleted) {
        var outEdge = processDefinitionEdgeRepository
                .findByFromNodeIdAndViaPort(
                        currentNode.getId(),
                        taskCompleted.getViaPort()
                );

        if (outEdge.isEmpty()) {
            var port = provider
                    .getPorts()
                    .stream()
                    .filter(processNodePort -> processNodePort.key().equals(taskCompleted.getViaPort()))
                    .findFirst();

            if (port.isEmpty()) {
                handleError(
                        triggeringUser,
                        provider,
                        currentNode,
                        processInstance,
                        processInstanceTask,
                        ProcessNodeExecutionResultError.of(
                                "Für das Prozesselement „%s“ wird durch den Prozesselement-Funktionsanbieter „%s“ kein ausgehender Port mit dem Schlüssel „%s“ bereitgestellt. Der Vorgang kann nicht fortgeführt werden. Bitte überprüfen Sie die Implementierung des Prozesselement-Funktionsanbieters."
                                        .formatted(currentNode.resolveName(provider), provider.getName(), taskCompleted.getViaPort())
                        )
                );
            } else {
                handleError(
                        triggeringUser,
                        provider,
                        currentNode,
                        processInstance,
                        processInstanceTask,
                        ProcessNodeExecutionResultError.of(
                                "Für das Prozesselement „%s“ wurde kein ausgehender Pfad für den Port „%s“ definiert. Der Vorgang kann nicht fortgeführt werden. Bitte überprüfen Sie die Implementierung des Prozesselement-Funktionsanbieters."
                                        .formatted(currentNode.resolveName(provider), port.get().label())
                        )
                );
            }

            return;
        }

        var newRuntimeData = taskCompleted.getRuntimeData();
        if (newRuntimeData == null) {
            newRuntimeData = new HashMap<>();
        }
        processInstanceTask.setRuntimeData(newRuntimeData);

        var newNodeData = taskCompleted.getNodeData();
        if (newNodeData == null) {
            newNodeData = new HashMap<>();
        }
        processInstanceTask.setNodeData(newNodeData);

        var newProcessData = taskCompleted.getProcessData();
        if (newProcessData == null) {
            newProcessData = previousTask != null ?
                    previousTask.getProcessData() :
                    processInstance.getInitialPayload();
        }
        processInstanceTask.setProcessData(applyOutputMappings(
                provider,
                currentNode.getOutputMappings(),
                newNodeData,
                newProcessData
        ));

        processInstanceTask.setStatus(ProcessTaskStatus.Completed);
        processInstanceTask.setFinished(LocalDateTime.now());

        if (taskCompleted.getTaskStatusOverride() != null) {
            processInstanceTask.setStatusOverride(taskCompleted.getTaskStatusOverride());
        }
        if (Boolean.TRUE.equals(taskCompleted.getClearTaskStatusOverride())) {
            processInstanceTask.setStatusOverride(null);
        }

        processInstanceTaskRepository.save(processInstanceTask);

        var nextPayload = new ProcessWorker.WorkerPayload(
                processInstance.getId(),
                currentNode.getId(),
                outEdge.get().getToNodeId()
        );

        rabbitTemplate.convertAndSend(ProcessWorker.DO_WORK_ON_INSTANCE_QUEUE, nextPayload);

        processInstanceHistoryEventRepository.save(new ProcessInstanceHistoryEventEntity(
                null,
                ProcessHistoryEventType.Complete,
                "Abschluss des Prozesselements",
                "Das Prozesselement „%s“ wurde abgeschlossen.".formatted(currentNode.resolveName(provider)),
                Map.of(),
                LocalDateTime.now(),
                triggeringUser != null ? triggeringUser.getId() : null,
                processInstance.getId(),
                processInstanceTask.getId()
        ));
    }

    private void handleInstanceComplete(@Nullable UserEntity triggeringUser,
                                        @Nonnull ProcessNodeDefinition provider,
                                        @Nonnull ProcessNodeEntity currentNode,
                                        @Nonnull ProcessInstanceEntity processInstance,
                                        @Nonnull ProcessInstanceTaskEntity processInstanceTask,
                                        @Nullable ProcessInstanceTaskEntity previousTask,
                                        @Nonnull ProcessNodeExecutionResultInstanceCompleted instanceCompleted) {
        var newRuntimeData = instanceCompleted.getRuntimeData();
        if (newRuntimeData == null) {
            newRuntimeData = new HashMap<>();
        }
        processInstanceTask.setRuntimeData(newRuntimeData);

        var newMetadata = instanceCompleted.getNodeData();
        if (newMetadata == null) {
            newMetadata = new HashMap<>();
        }
        processInstanceTask.setNodeData(newMetadata);

        var newWorkingData = instanceCompleted.getProcessData();
        if (newWorkingData == null) {
            newWorkingData = previousTask != null ?
                    previousTask.getProcessData() :
                    processInstance.getInitialPayload();
        }
        processInstanceTask.setProcessData(applyOutputMappings(
                provider,
                currentNode.getOutputMappings(),
                newMetadata,
                newWorkingData
        ));

        processInstanceTask.setStatus(ProcessTaskStatus.Completed);
        processInstanceTask.setFinished(LocalDateTime.now());

        if (instanceCompleted.getTaskStatusOverride() != null) {
            processInstanceTask.setStatusOverride(instanceCompleted.getTaskStatusOverride());
        }
        if (Boolean.TRUE.equals(instanceCompleted.getClearTaskStatusOverride())) {
            processInstanceTask.setStatusOverride(null);
        }

        processInstanceTaskRepository.save(processInstanceTask);

        processInstance.setStatus(ProcessInstanceStatus.Completed);
        processInstance.setFinished(LocalDateTime.now());
        processInstanceRepository.save(processInstance);

        processInstanceHistoryEventRepository.save(new ProcessInstanceHistoryEventEntity(
                null,
                ProcessHistoryEventType.Complete,
                "Abschluss des Vorgangs",
                "Der Vorgang wurde abgeschlossen.",
                Map.of(),
                LocalDateTime.now(),
                triggeringUser != null ? triggeringUser.getId() : null,
                processInstance.getId(),
                null
        ));
    }

    private void handleError(@Nullable UserEntity triggeringUser,
                             @Nonnull ProcessNodeDefinition provider,
                             @Nonnull ProcessNodeEntity currentNode,
                             @Nonnull ProcessInstanceEntity processInstance,
                             @Nonnull ProcessInstanceTaskEntity processInstanceTask,
                             @Nonnull ProcessNodeExecutionResultError err) {
        var triggeringUserId = triggeringUser != null ? triggeringUser.getId() : null;

        if (StringUtils.isNotNullOrEmpty(err.getMessage())) {
            processInstanceHistoryEventRepository
                    .save(new ProcessInstanceHistoryEventEntity(
                            null,
                            ProcessHistoryEventType.Error,
                            "Fehler im Prozesselement",
                            err.getMessage(),
                            Map.of(),
                            LocalDateTime.now(),
                            triggeringUserId,
                            processInstance.getId(),
                            processInstanceTask.getId()
                    ));
        }

        if (err.getThrowable() != null) {
            processInstanceHistoryEventRepository
                    .save(ProcessInstanceHistoryEventEntity
                            .from(
                                    processInstance.getId(),
                                    processInstanceTask.getId(),
                                    triggeringUserId,
                                    err.getThrowable()
                            ));
        }


        processEvents(
                processInstance,
                processInstanceTask,
                err,
                triggeringUserId
        );

        processInstanceTask.setStatus(ProcessTaskStatus.Failed);
        processInstanceTask.setFinished(LocalDateTime.now());

        if (err.getTaskStatusOverride() != null) {
            processInstanceTask.setStatusOverride(err.getTaskStatusOverride());
        }
        if (Boolean.TRUE.equals(err.getClearTaskStatusOverride())) {
            processInstanceTask.setStatusOverride(null);
        }

        processInstanceTaskRepository.save(processInstanceTask);

        processInstance.setStatus(ProcessInstanceStatus.Failed);
        processInstance.setFinished(LocalDateTime.now());
        processInstanceRepository.save(processInstance);
    }

    private void processEvents(@Nonnull ProcessInstanceEntity processInstance,
                               @Nonnull ProcessInstanceTaskEntity processInstanceTask,
                               @Nonnull ProcessNodeExecutionResult res,
                               @Nullable String triggeringUserId) {
        if (res.getEvents() == null) {
            return;
        }

        for (var event : res.getEvents()) {
            processInstanceHistoryEventRepository
                    .save(
                            ProcessInstanceHistoryEventEntity
                                    .from(
                                            processInstance.getId(),
                                            processInstanceTask.getId(),
                                            triggeringUserId,
                                            event
                                    )
                    );
        }
    }

    private static Map<String, Object> applyOutputMappings(@Nonnull ProcessNodeDefinition provider,
                                                           @Nonnull Map<String, String> outputMappings,
                                                           @Nonnull Map<String, Object> nodeData,
                                                           @Nonnull Map<String, Object> processData) {
        var updatedProcessData = new HashMap<>(processData);

        for (var nodeProviderOutput : provider.getOutputs()) {
            var targetFieldPath = outputMappings.get(nodeProviderOutput.key());
            if (targetFieldPath == null) {
                continue;
            }

            var pathParts = targetFieldPath.split("\\.");

            if (pathParts.length == 0) {
                continue;
            }

            if (pathParts.length == 1) {
                var outputValue = nodeData.get(nodeProviderOutput.key());
                updatedProcessData.put(pathParts[0], outputValue);
                continue;
            }

            Map<String, Object> targetObject = updatedProcessData;

            for (int i = 0; i < pathParts.length - 1; i++) {
                var pathPart = pathParts[i];

                if (targetObject.containsKey(pathPart)) {
                    if (targetObject.get(pathPart) instanceof Map<?,?> map) {
                        targetObject = (Map<String, Object>) map;
                    } else {
                        throw new IllegalStateException("Der Pfadteil '%s' im Ausgabe-Mapping '%s' verweist auf ein existierendes Feld, das kein Objekt ist."
                                .formatted(pathPart, targetFieldPath));
                    }
                } else {
                    var newObject = new HashMap<String, Object>();
                    targetObject.put(pathPart, newObject);
                    targetObject = newObject;
                }
            }

            var targetField = pathParts[pathParts.length - 1];

            var outputValue = nodeData.get(nodeProviderOutput.key());

            targetObject.put(targetField, outputValue);
        }

        return updatedProcessData;
    }
}
