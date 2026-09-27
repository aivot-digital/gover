package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.department.entities.DepartmentEntity;
import de.aivot.prosuna.backend.department.repositories.DepartmentRepository;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.dtos.ProcessInstanceDetailsDTO;
import de.aivot.prosuna.backend.process.dtos.ProcessTaskDetailsDTO;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider;
import de.aivot.prosuna.backend.process.repositories.*;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Set;

@Service
public class ProcessInstanceDetailsService {
    private final PermissionService permissions;
    private final ProcessInstanceRepository instances;
    private final ProcessRepository processes;
    private final DepartmentRepository departments;
    private final ProcessNodeRepository nodes;
    private final ProcessInstanceTaskRepository tasks;
    private final ProcessNodeDefinitionService definitions;

    public ProcessInstanceDetailsService(PermissionService permissions, ProcessInstanceRepository instances,
                                         ProcessRepository processes, DepartmentRepository departments,
                                         ProcessNodeRepository nodes, ProcessInstanceTaskRepository tasks,
                                         ProcessNodeDefinitionService definitions) {
        this.permissions = permissions;
        this.instances = instances;
        this.processes = processes;
        this.departments = departments;
        this.nodes = nodes;
        this.tasks = tasks;
        this.definitions = definitions;
    }

    @Nonnull
    @Transactional(readOnly = true)
    public ProcessInstanceDetailsDTO retrieve(@Nonnull String userId, @Nonnull Long id) throws ResponseException {
        permissions.requireProcessInstancePermission(userId, id, ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ);
        var instance = instances.findById(id).orElseThrow(ResponseException::notFound);
        var process = processes.findById(instance.getProcessId()).orElseThrow(ResponseException::notFound);
        var trigger = nodes.findById(instance.getInitialNodeId());
        var activeStatuses = Set.of(ProcessTaskStatus.Running, ProcessTaskStatus.Paused,
                ProcessTaskStatus.AwaitingCustomer, ProcessTaskStatus.AwaitingPayment);
        // Only labels needed by the instance view are exposed, never the process configuration.
        var activeTasks = tasks.findAllByProcessInstanceId(id).stream()
                .filter(task -> activeStatuses.contains(task.getStatus()))
                .sorted(Comparator.comparing(task -> task.getId()))
                .map(task -> new ProcessInstanceDetailsDTO.ActiveTask(task.getId(),
                        nodes.findById(task.getProcessNodeId()).map(this::nodeName).orElse("Unbenannte Aufgabe"),
                        task.getStatus(), task.getStatusOverride(), task.getAssignedUserId(), task.getDeadline()))
                .toList();
        return new ProcessInstanceDetailsDTO(instance, process.getInternalTitle(), process.getDepartmentId(),
                departments.findById(process.getDepartmentId()).map(DepartmentEntity::getName).orElse(null),
                trigger.map(this::nodeName).orElse("Nicht verfügbar"),
                trigger.flatMap(definitions::getProcessNodeDefinition).map(definition -> definition.getName()).orElse(null),
                activeTasks);
    }

    @Nonnull
    @Transactional(readOnly = true)
    public ProcessTaskDetailsDTO retrieveTask(@Nonnull String userId, @Nonnull Long taskId) throws ResponseException {
        var task = tasks.findById(taskId).orElseThrow(ResponseException::notFound);
        permissions.requireProcessInstancePermission(userId, task.getProcessInstanceId(), ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ);
        var instance = instances.findById(task.getProcessInstanceId()).orElseThrow(ResponseException::notFound);
        var process = processes.findById(task.getProcessId()).orElseThrow(ResponseException::notFound);
        var node = nodes.findById(task.getProcessNodeId());
        return new ProcessTaskDetailsDTO(task, instance,
                new ProcessTaskDetailsDTO.ProcessSummary(process.getId(), process.getInternalTitle()),
                node.map(value -> new ProcessTaskDetailsDTO.NodeSummary(value.getName(), value.getDescription())).orElse(null),
                node.flatMap(definitions::getProcessNodeDefinition).map(value -> new ProcessTaskDetailsDTO.ProviderSummary(
                        value.getKey(), value.getComponentKey(), value.getName(), value.getAbstract())).orElse(null));
    }

    @Nonnull
    private String nodeName(@Nonnull ProcessNodeEntity node) {
        return definitions.getProcessNodeDefinition(node).map(node::resolveName)
                .orElseGet(() -> node.getName() == null || node.getName().isBlank() ? "Unbenanntes Prozesselement" : node.getName());
    }
}
