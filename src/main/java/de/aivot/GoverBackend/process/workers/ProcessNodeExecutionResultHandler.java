package de.aivot.GoverBackend.process.workers;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEventEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.enums.ProcessHistoryEventType;
import de.aivot.GoverBackend.process.enums.ProcessInstanceStatus;
import de.aivot.GoverBackend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.GoverBackend.process.enums.ProcessTaskStatus;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionBrokenImplementation;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionInvalidAssignment;
import de.aivot.GoverBackend.process.models.*;
import de.aivot.GoverBackend.process.repositories.ProcessEdgeRepository;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceHistoryEventRepository;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceRepository;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.services.UserService;
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

    public void handleResult(@Nonnull ProcessNodeExecutionLogger logger,
                             @Nullable UserEntity triggeringUser,
                             @Nonnull ProcessNodeDefinition provider,
                             @Nonnull ProcessNodeEntity currentNode,
                             @Nonnull ProcessInstanceEntity processInstance,
                             @Nonnull ProcessInstanceTaskEntity processInstanceTask,
                             @Nullable ProcessInstanceTaskEntity previousTask,
                             @Nullable ProcessNodeExecutionResult executionResult) throws ProcessNodeExecutionException {
        if (executionResult == null) {
            throw new ProcessNodeExecutionExceptionBrokenImplementation(
                    """
                            Der Prozesselement-Funktionsanbieter „%s“ des Prozesselementes „%s“ hat ein null-Ergebnis zurückgegeben.
                            Bitte überprüfen Sie die Implementierung des Prozesselement-Funktionsanbieters!
                            """,
                    provider.getName(),
                    currentNode.resolveName(provider)
            );
        }

        switch (executionResult) {
            case ProcessNodeExecutionResultTaskCompleted taskCompleted -> handleTaskComplete(
                    logger,
                    triggeringUser,
                    provider,
                    currentNode,
                    processInstance,
                    processInstanceTask,
                    previousTask,
                    taskCompleted
            );
            case ProcessNodeExecutionResultInstanceCompleted instanceCompleted -> handleInstanceComplete(
                    logger,
                    triggeringUser,
                    provider,
                    currentNode,
                    processInstance,
                    processInstanceTask,
                    previousTask,
                    instanceCompleted
            );
            case ProcessNodeExecutionResultTaskAssigned assigned -> handleAssigned(
                    logger,
                    triggeringUser,
                    provider,
                    currentNode,
                    processInstance,
                    processInstanceTask,
                    previousTask,
                    assigned
            );
            default -> throw new ProcessNodeExecutionExceptionBrokenImplementation(
                    """
                            Der Prozesselement-Funktionsanbieter „%s“ des Prozesselementes „%s“ hat eine unbekanntes Ergebnisklasse erzeugt: „%s“.
                            Bitte überprüfen Sie die Implementierung des Prozesselement-Funktionsanbieters!
                            """,
                    provider.getName(),
                    currentNode.resolveName(provider),
                    executionResult.getClass().getName()
            );
        }
    }

    private void handleAssigned(@Nonnull ProcessNodeExecutionLogger logger,
                                @Nullable UserEntity triggeringUser,
                                @Nonnull ProcessNodeDefinition provider,
                                @Nonnull ProcessNodeEntity currentNode,
                                @Nonnull ProcessInstanceEntity processInstance,
                                @Nonnull ProcessInstanceTaskEntity processInstanceTask,
                                @Nullable ProcessInstanceTaskEntity previousTask,
                                @Nonnull ProcessNodeExecutionResultTaskAssigned assigned) throws ProcessNodeExecutionException {
        UserEntity assignedUser;
        try {
            assignedUser = userService
                    .retrieve(assigned.getAssignedUserId())
                    .orElse(null);
        } catch (ResponseException e) {
            throw new ProcessNodeExecutionExceptionInvalidAssignment(
                    e,
                    """
                            Der Prozesselement-Funktionsanbieter „%s“ des Prozesselementes „%s“ hat eine ungültige Mitarbeiter:in-ID „%s“ zurückgegeben.
                            Bitte überprüfen Sie die Implementierung des Prozesselement-Funktionsanbieters!
                            """,
                    provider.getName(),
                    currentNode.resolveName(provider),
                    assigned.getAssignedUserId()
            );
        }

        if (assignedUser == null) {
            throw new ProcessNodeExecutionExceptionInvalidAssignment(
                    """
                            Der Prozesselement-Funktionsanbieter „%s“ des Prozesselementes „%s“ hat eine unbekannte Mitarbeiter:in-ID „%s“ zurückgegeben.
                            Bitte überprüfen Sie die Implementierung des Prozesselement-Funktionsanbieters!
                            """,
                    provider.getName(),
                    currentNode.resolveName(provider),
                    assigned.getAssignedUserId()
            );
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
            logger
                    .logf(
                            ProcessNodeExecutionLogLevel.Debug,
                            false,
                            true,
                            "Die Aufgabe wurde durch „%s“ der Mitarbeiter:in „%s“ zugewiesen",
                            triggeringUser.getFullName(),
                            assignedUser.getFullName()
                    );
        } else {
            /*
            processInstanceHistoryEventRepository
                    .save(new ProcessInstanceEventEntity(
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
             */
        }

        // TODO: send notification
    }

    private void handleTaskComplete(@Nonnull ProcessNodeExecutionLogger logger,
                                    @Nullable UserEntity triggeringUser,
                                    @Nonnull ProcessNodeDefinition provider,
                                    @Nonnull ProcessNodeEntity currentNode,
                                    @Nonnull ProcessInstanceEntity processInstance,
                                    @Nonnull ProcessInstanceTaskEntity processInstanceTask,
                                    @Nullable ProcessInstanceTaskEntity previousTask,
                                    @Nonnull ProcessNodeExecutionResultTaskCompleted taskCompleted) throws ProcessNodeExecutionException {
        var port = provider
                .getPorts()
                .stream()
                .filter(processNodePort -> processNodePort.key().equals(taskCompleted.getViaPort()))
                .findFirst();

        if (port.isEmpty()) {
            throw new ProcessNodeExecutionExceptionBrokenImplementation(
                    """
                            Für das Prozesselement „%s“ wird durch den Prozesselement-Funktionsanbieter „%s“ kein ausgehender Port mit dem Schlüssel „%s“ bereitgestellt.
                            Der Vorgang kann nicht fortgeführt werden. Bitte überprüfen Sie die Implementierung des Prozesselement-Funktionsanbieters.
                            """,
                    currentNode.resolveName(provider),
                    provider.getName(),
                    taskCompleted.getViaPort()
            );
        }

        var outEdge = processDefinitionEdgeRepository
                .findByFromNodeIdAndViaPort(
                        currentNode.getId(),
                        taskCompleted.getViaPort()
                );

        if (outEdge.isEmpty()) {
            throw new ProcessNodeExecutionExceptionBrokenImplementation(
                    """
                            Für das Prozesselement „%s“ wurde kein ausgehender Pfad für den Port „%s“ definiert.
                            Der Vorgang kann nicht fortgeführt werden. Bitte überprüfen Sie die Implementierung des Prozesselement-Funktionsanbieters.
                            Bitte prüfen Sie den Aufbau Ihres Prozessmodells.
                            """,
                    currentNode.resolveName(provider),
                    provider.getName(),
                    taskCompleted.getViaPort()
            );
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

        /*
        processInstanceHistoryEventRepository.save(new ProcessInstanceEventEntity(
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
         */
    }

    private void handleInstanceComplete(@Nonnull ProcessNodeExecutionLogger logger,
                                        @Nullable UserEntity triggeringUser,
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

        /*
        processInstanceHistoryEventRepository.save(new ProcessInstanceEventEntity(
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
         */
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
                    if (targetObject.get(pathPart) instanceof Map<?, ?> map) {
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
