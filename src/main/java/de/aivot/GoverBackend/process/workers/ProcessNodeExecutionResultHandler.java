package de.aivot.GoverBackend.process.workers;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.mail.services.ProcessTaskMailService;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.enums.ProcessInstanceStatus;
import de.aivot.GoverBackend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.GoverBackend.process.enums.ProcessTaskStatus;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionBrokenImplementation;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionInvalidAssignment;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.GoverBackend.process.models.*;
import de.aivot.GoverBackend.process.models.executionResult.*;
import de.aivot.GoverBackend.process.repositories.ProcessEdgeRepository;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceRepository;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.GoverBackend.process.repositories.ProcessNodeRepository;
import de.aivot.GoverBackend.process.services.ProcessNodeDefinitionService;
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
import java.util.Objects;

@Service
public class ProcessNodeExecutionResultHandler {
    private final RabbitTemplate rabbitTemplate;
    private final ProcessInstanceRepository processInstanceRepository;
    private final ProcessInstanceTaskRepository processInstanceTaskRepository;
    private final ProcessEdgeRepository processDefinitionEdgeRepository;
    private final UserService userService;
    private final ProcessTaskMailService processTaskMailService;
    private final ProcessNodeRepository processNodeRepository;
    private final ProcessNodeDefinitionService processNodeDefinitionService;

    @Autowired
    public ProcessNodeExecutionResultHandler(RabbitTemplate rabbitTemplate,
                                             ProcessInstanceRepository processInstanceRepository,
                                             ProcessInstanceTaskRepository processInstanceTaskRepository,
                                             ProcessEdgeRepository processDefinitionEdgeRepository,
                                             UserService userService,
                                             ProcessTaskMailService processTaskMailService,
                                             ProcessNodeRepository processNodeRepository,
                                             ProcessNodeDefinitionService processNodeDefinitionService) {
        this.rabbitTemplate = rabbitTemplate;
        this.processInstanceRepository = processInstanceRepository;
        this.processInstanceTaskRepository = processInstanceTaskRepository;
        this.processDefinitionEdgeRepository = processDefinitionEdgeRepository;
        this.userService = userService;
        this.processTaskMailService = processTaskMailService;
        this.processNodeRepository = processNodeRepository;
        this.processNodeDefinitionService = processNodeDefinitionService;
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
            var err = String.format(
                    """
                            Der Prozesselement-Funktionsanbieter %s des Prozesselementes %s hat ein leeres Ergebnis zurückgegeben.
                            Bitte überprüfen Sie die Implementierung des Prozesselement-Funktionsanbieters!
                            """,
                    StringUtils.quote(provider.getName()),
                    StringUtils.quote(currentNode.resolveName(provider))
            );
            logger.logf(
                    ProcessNodeExecutionLogLevel.Error,
                    true,
                    true,
                    "Leeres Ergebnis",
                    err,
                    StringUtils.quote(currentNode.resolveName(provider))
            );
            throw new ProcessNodeExecutionExceptionBrokenImplementation(err)
                    .setAlreadyLogged(true);
        }

        switch (executionResult) {
            case ProcessNodeExecutionResultTaskUpdated taskUpdated -> handleTaskUpdated(
                    logger,
                    triggeringUser,
                    provider,
                    currentNode,
                    processInstance,
                    processInstanceTask,
                    previousTask,
                    taskUpdated
            );
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
        String previousAssignedUserId = processInstanceTask.getAssignedUserId();

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
            logger.logf(
                    ProcessNodeExecutionLogLevel.Info,
                    false,
                    true,
                    "Aufgabe neu zugewiesen",
                    "Die Aufgabe wurde durch %s der Mitarbeiter:in %s zugewiesen.",
                    StringUtils.quote(triggeringUser.getFullName()),
                    StringUtils.quote(assignedUser.getFullName())
            );
        } else {
            logger.logf(
                    ProcessNodeExecutionLogLevel.Info,
                    true,
                    true,
                    "Aufgabe " + StringUtils.quote(currentNode.resolveName(provider)) + " automatisch zugewiesen",
                    "Die Aufgabe wurde automatisch der Mitarbeiter:in %s zugewiesen.",
                    StringUtils.quote(assignedUser.getFullName())
            );
        }

        if (Objects.equals(previousAssignedUserId, assignedUser.getId())) {
            return;
        }

        if (triggeringUser != null && assignedUser.getId().equals(triggeringUser.getId())) {
            return;
        }

        if (processInstance.getStatus() != ProcessInstanceStatus.Running) {
            processInstance.setStatus(ProcessInstanceStatus.Running);
            processInstanceRepository.save(processInstance);
        }

        try {
            processTaskMailService.sendAssigned(
                    triggeringUser,
                    assignedUser,
                    processInstance,
                    processInstanceTask,
                    currentNode,
                    provider,
                    previousAssignedUserId != null
            );
        } catch (Exception e) {
            logger.logException(new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Die E-Mail-Benachrichtigung für die zugewiesene Aufgabe an '%s' konnte nicht versendet werden.",
                    assignedUser.getFullName()
            ));
        }
    }

    private void handleTaskUpdated(@Nonnull ProcessNodeExecutionLogger logger,
                                   @Nullable UserEntity triggeringUser,
                                   @Nonnull ProcessNodeDefinition provider,
                                   @Nonnull ProcessNodeEntity currentNode,
                                   @Nonnull ProcessInstanceEntity processInstance,
                                   @Nonnull ProcessInstanceTaskEntity processInstanceTask,
                                   @Nullable ProcessInstanceTaskEntity previousTask,
                                   @Nonnull ProcessNodeExecutionResultTaskUpdated updatedTask) throws ProcessNodeExecutionException {
        var newRuntimeData = updatedTask.getRuntimeData();
        if (newRuntimeData == null) {
            newRuntimeData = new HashMap<>();
        }
        processInstanceTask.setRuntimeData(newRuntimeData);

        var newNodeData = updatedTask.getNodeData();
        if (newNodeData == null) {
            newNodeData = new HashMap<>();
        }
        processInstanceTask.setNodeData(newNodeData);

        var newProcessData = updatedTask.getProcessData();
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

        processInstanceTask.setStatus(ProcessTaskStatus.Running);

        if (updatedTask.getTaskStatusOverride() != null) {
            processInstanceTask.setStatusOverride(updatedTask.getTaskStatusOverride());
        }
        if (Boolean.TRUE.equals(updatedTask.getClearTaskStatusOverride())) {
            processInstanceTask.setStatusOverride(null);
        }

        processInstanceTaskRepository.save(processInstanceTask);

        if (processInstance.getStatus() != ProcessInstanceStatus.Running) {
            processInstance.setStatus(ProcessInstanceStatus.Running);
            processInstanceRepository.save(processInstance);
        }

        if (triggeringUser != null) {
            logger.logf(
                    ProcessNodeExecutionLogLevel.Debug,
                    true,
                    false,
                    "Eingaben für " + StringUtils.quote(currentNode.resolveName(provider)) + " gespeichert",
                    "Für die Aufgabe %s wurden durch die Mitarbeiter:in %s Eingaben abgespeichert.",
                    StringUtils.quote(currentNode.resolveName(provider)),
                    StringUtils.quote(triggeringUser.getFullName())
            );
        } else {
            logger.logf(
                    ProcessNodeExecutionLogLevel.Debug,
                    true,
                    false,
                    "Eingaben für " + StringUtils.quote(currentNode.resolveName(provider)) + " gespeichert",
                    "Für die Aufgabe %s wurden durch die zugewiesen Mitarbeiter:in Eingaben abgespeichert.",
                    StringUtils.quote(currentNode.resolveName(provider))
            );
        }
    }

    private <NodeConfig> void handleTaskComplete(@Nonnull ProcessNodeExecutionLogger logger,
                                                 @Nullable UserEntity triggeringUser,
                                                 @Nonnull ProcessNodeDefinition<NodeConfig> provider,
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
                            Für das Prozesselement %s wird durch den Prozesselement-Funktionsanbieter %s kein ausgehender Port mit dem Schlüssel %s bereitgestellt.
                            Der Vorgang kann nicht fortgeführt werden. Bitte überprüfen Sie die Implementierung des Prozesselement-Funktionsanbieters.
                            """,
                    StringUtils.quote(currentNode.resolveName(provider)),
                    StringUtils.quote(provider.getName()),
                    StringUtils.quote(taskCompleted.getViaPort())
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
                            Für das Prozesselement %s wurde kein ausgehender Pfad für den Port %s definiert.
                            Der Vorgang kann nicht fortgeführt werden. Bitte überprüfen Sie die Implementierung des Prozesselement-Funktionsanbieters.
                            Bitte prüfen Sie den Aufbau Ihres Prozessmodells.
                            """,
                    StringUtils.quote(currentNode.resolveName(provider)),
                    StringUtils.quote(port.get().label())
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

        if (processInstance.getStatus() != ProcessInstanceStatus.Running) {
            processInstance.setStatus(ProcessInstanceStatus.Running);
            processInstanceRepository.save(processInstance);
        }

        var nextPayload = new ProcessWorker.WorkerPayload(
                processInstance.getId(),
                processInstanceTask.getId(),
                currentNode.getId(),
                taskCompleted.getViaPort(),
                outEdge.get().getToNodeId()
        );

        var nextNode = processNodeRepository
                .findById(outEdge.get().getToNodeId())
                .map(node -> processNodeDefinitionService
                        .getProcessNodeDefinition(node)
                        .map(node::resolveName)
                        .orElse("UNKNOWN"))
                .orElse("UNKNOWN");

        if (triggeringUser != null) {
            logger.logf(
                    ProcessNodeExecutionLogLevel.Info,
                    true,
                    true,
                    "Aufgabe " + StringUtils.quote(currentNode.resolveName(provider)) + " abgeschlossen",
                    """
                            Die Aufgabe für das Prozesselement %s wurde durch die Mitarbeiter:in %s abgeschlossen.
                            Als ausgehende Verbindung wird der Ausgang %s verwendet.
                            Das nächste Prozesselement ist %s.
                            """,
                    StringUtils.quote(currentNode.resolveName(provider)),
                    StringUtils.quote(triggeringUser.getFullName()),
                    StringUtils.quote(port.get().label()),
                    StringUtils.quote(nextNode)
            );
        } else {
            logger.logf(
                    ProcessNodeExecutionLogLevel.Info,
                    true,
                    true,
                    "Aufgabe " + StringUtils.quote(currentNode.resolveName(provider)) + " abgeschlossen",
                    """
                            Die Aufgabe für das Prozesselement %s wurde abgeschlossen.
                            Als ausgehende Verbindung wird der Ausgang %s verwendet.
                            Das nächste Prozesselement ist %s.
                            """,
                    StringUtils.quote(currentNode.resolveName(provider)),
                    StringUtils.quote(port.get().label()),
                    StringUtils.quote(nextNode)
            );
        }

        rabbitTemplate.convertAndSend(ProcessWorker.DO_WORK_ON_INSTANCE_QUEUE, nextPayload);
    }

    private void handleInstanceComplete(@Nonnull ProcessNodeExecutionLogger logger,
                                        @Nullable UserEntity triggeringUser,
                                        @Nonnull ProcessNodeDefinition provider,
                                        @Nonnull ProcessNodeEntity currentNode,
                                        @Nonnull ProcessInstanceEntity processInstance,
                                        @Nonnull ProcessInstanceTaskEntity processInstanceTask,
                                        @Nullable ProcessInstanceTaskEntity previousTask,
                                        @Nonnull ProcessNodeExecutionResultInstanceCompleted instanceCompleted) {
        var completionTime = LocalDateTime.now();

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
        processInstanceTask.setFinished(completionTime);

        if (instanceCompleted.getTaskStatusOverride() != null) {
            processInstanceTask.setStatusOverride(instanceCompleted.getTaskStatusOverride());
        }
        if (Boolean.TRUE.equals(instanceCompleted.getClearTaskStatusOverride())) {
            processInstanceTask.setStatusOverride(null);
        }

        processInstanceTaskRepository.save(processInstanceTask);

        processInstance.setStatus(ProcessInstanceStatus.Completed);
        processInstance.setFinished(completionTime);
        processInstance.setKeepUntil(instanceCompleted.getRetentionDate());
        processInstanceRepository.save(processInstance);


        logger.logf(
                ProcessNodeExecutionLogLevel.Info,
                true,
                true,
                "Vorgang abgeschlossen",
                "Der Vorgang wurde erfolgreich abgeschlossen. Das abschließende Prozesselement war %s",
                StringUtils.quote(currentNode.resolveName(provider))
        );
    }

    private static <NodeConfig> Map<String, Object> applyOutputMappings(@Nonnull ProcessNodeDefinition<NodeConfig> provider,
                                                                        @Nonnull Map<String, String> outputMappings,
                                                                        @Nonnull Map<String, Object> nodeData,
                                                                        @Nonnull Map<String, Object> processData) {
        var executionData = new ProcessExecutionData()
                .addProcessData(new HashMap<>(processData));

        for (var nodeProviderOutput : provider.getOutputs()) {
            var targetFieldPath = StringUtils.toNullableTrimmedString(outputMappings.get(nodeProviderOutput.key()));
            if (targetFieldPath == null) {
                continue;
            }

            var outputValue = nodeData.get(nodeProviderOutput.key());
            try {
                if (ProcessDataValueUtils.hasWildcardSegment(targetFieldPath)) {
                    var wildcardBindings = ProcessDataValueUtils
                            .resolveMatchingProcessDataValues(executionData, targetFieldPath)
                            .stream()
                            .map(ProcessDataValueUtils.ResolvedProcessDataValue::wildcardIndices)
                            .toList();

                    for (var wildcardBinding : wildcardBindings) {
                        ProcessDataValueUtils.writeProcessDataValue(
                                executionData,
                                targetFieldPath,
                                outputValue,
                                wildcardBinding
                        );
                    }
                } else {
                    ProcessDataValueUtils.writeProcessDataValue(
                            executionData,
                            targetFieldPath,
                            outputValue
                    );
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                throw new IllegalStateException(
                        "Das Ausgabe-Mapping '%s' fuer den Knotenausgang '%s' ist ungueltig: %s"
                                .formatted(targetFieldPath, nodeProviderOutput.key(), e.getMessage()),
                        e
                );
            }
        }

        return executionData.getProcessData();
    }
}
