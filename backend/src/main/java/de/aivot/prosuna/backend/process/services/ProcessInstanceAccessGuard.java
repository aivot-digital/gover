package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.projections.ProcessTaskAssignmentProjection;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.repositories.*;
import de.aivot.prosuna.backend.user.repositories.UserRepository;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider.*;

@Service
@Transactional(propagation = Propagation.MANDATORY)
public class ProcessInstanceAccessGuard {
    private static final Set<ProcessTaskStatus> ACTIVE = Set.of(ProcessTaskStatus.Running, ProcessTaskStatus.Paused,
            ProcessTaskStatus.AwaitingCustomer, ProcessTaskStatus.AwaitingPayment);
    private static final List<String> REQUIRED = List.of(PROCESS_INSTANCE_READ, PROCESS_INSTANCE_EDIT_TASK);
    private final ProcessInstanceRepository instances;
    private final ProcessInstanceTaskRepository tasks;
    private final PermissionService permissions;
    private final ProcessNodeRepository nodes;
    private final ProcessNodeDefinitionService definitions;
    private final UserRepository users;

    public ProcessInstanceAccessGuard(ProcessInstanceRepository instances, ProcessInstanceTaskRepository tasks,
                                      PermissionService permissions, ProcessNodeRepository nodes,
                                      ProcessNodeDefinitionService definitions, UserRepository users) {
        this.instances = instances;
        this.tasks = tasks;
        this.permissions = permissions;
        this.nodes = nodes;
        this.definitions = definitions;
        this.users = users;
    }

    public void lock(@Nonnull Long instanceId) throws ResponseException {
        instances.lockAccessById(instanceId).orElseThrow(ResponseException::notFound);
    }

    @Nonnull
    public Map<String, Set<String>> lockAndSnapshot(@Nonnull Long instanceId) throws ResponseException {
        lock(instanceId);
        var before = new HashMap<String, Set<String>>();
        for (var task : activeAssignments(instanceId)) {
            before.computeIfAbsent(task.assignedUserId(), userId -> {
                var ownPermissions = new HashSet<String>();
                for (var key : REQUIRED) {
                    if (permissions.hasProcessInstancePermissionWithoutDeputies(userId, instanceId, key)) {
                        ownPermissions.add(key);
                    }
                }
                return Set.copyOf(ownPermissions);
            });
        }
        return before;
    }

    public void requireRetainedAccess(@Nonnull Long instanceId, @Nonnull Map<String, Set<String>> before) throws ResponseException {
        requireRetainedAccess(instanceId, before, false);
    }

    public void requireRetainedAccess(@Nonnull Long instanceId, @Nonnull Map<String, Set<String>> before,
                                      boolean includeTaskDetails) throws ResponseException {
        var affectedUsers = new HashSet<String>();
        before.forEach((userId, ownPermissions) -> {
            // Existing missing rights must not prevent unrelated changes or attempts to repair access.
            // Temporary deputy rights must not mask the loss of an assignee's own access.
            if (ownPermissions.stream().anyMatch(key -> !permissions.hasProcessInstancePermissionWithoutDeputies(userId, instanceId, key))) {
                affectedUsers.add(userId);
            }
        });
        if (affectedUsers.isEmpty()) return;
        var affectedTasks = activeAssignments(instanceId).stream()
                .filter(task -> affectedUsers.contains(task.assignedUserId())).toList();
        if (affectedTasks.isEmpty()) return;
        // Updating ACLs does not imply permission to read task or assignee details.
        var affected = includeTaskDetails ? affectedTasks.stream()
                .sorted(Comparator.comparing(ProcessTaskAssignmentProjection::id))
                .map(task -> new AffectedTask(task.id(), nodes.findById(task.processNodeId())
                        .map(this::nodeName).orElse("Aufgabe " + task.id()),
                        users.findById(task.assignedUserId()).map(user -> user.getFullName())
                                .orElse("Nicht mehr verfügbare Person")))
                .toList() : List.<AffectedTask>of();
        throw ResponseException.conflictWithDetails(
                "Die Berechtigungen konnten nicht gespeichert werden. Durch diese Änderung würden zugewiesene Personen den erforderlichen Zugriff auf aktive Aufgaben verlieren. Bitte weisen Sie die betroffenen Aufgaben zuerst anderen berechtigten Personen zu.",
                Map.of("reason", "assigned_tasks_lose_access", "tasks", affected));
    }

    private List<ProcessTaskAssignmentProjection> activeAssignments(@Nonnull Long instanceId) {
        // The instance lock serializes assignments, but does not refresh previously loaded JPA entities.
        // Read scalar values so a committed reassignment cannot be hidden by the first-level cache.
        return tasks.findAssignmentSnapshots(instanceId).stream()
                .filter(task -> task.assignedUserId() != null && ACTIVE.contains(task.status())).toList();
    }

    private String nodeName(ProcessNodeEntity node) {
        return definitions.getProcessNodeDefinition(node).map(node::resolveName)
                .orElseGet(() -> node.getName() == null || node.getName().isBlank() ? "Unbenannte Aufgabe" : node.getName());
    }

    public record AffectedTask(@Nonnull Long id, @Nonnull String name, @Nonnull String assignedUserName) {
    }
}
