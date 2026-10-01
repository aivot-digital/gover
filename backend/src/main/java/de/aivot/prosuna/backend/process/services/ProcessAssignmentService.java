package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.audit.enums.AuditAction;
import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.audit.services.ScopedAuditService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.dtos.ProcessAssignmentOptionDTO;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.repositories.UserRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider.*;

@Service
public class ProcessAssignmentService {
    private static final Set<ProcessTaskStatus> ACTIVE_TASK_STATUSES = Set.of(ProcessTaskStatus.Running,
            ProcessTaskStatus.Paused, ProcessTaskStatus.AwaitingCustomer, ProcessTaskStatus.AwaitingPayment);
    private static final List<String> TASK_ASSIGNMENT_PERMISSIONS = List.of(PROCESS_INSTANCE_READ, PROCESS_INSTANCE_EDIT_TASK);
    private final PermissionService permissions;
    private final UserRepository users;
    private final ProcessInstanceRepository instances;
    private final ProcessInstanceTaskRepository tasks;
    private final ScopedAuditService audit;

    public ProcessAssignmentService(PermissionService permissions, UserRepository users,
                                    ProcessInstanceRepository instances, ProcessInstanceTaskRepository tasks,
                                    AuditService audit) {
        this.permissions = permissions;
        this.users = users;
        this.instances = instances;
        this.tasks = tasks;
        this.audit = audit.createScopedAuditService(ProcessAssignmentService.class, "Vorgänge");
    }

    @Nonnull
    @Transactional(readOnly = true)
    public List<ProcessAssignmentOptionDTO> instanceOptions(@Nonnull String actorId, @Nonnull Long instanceId) throws ResponseException {
        requireAssignableInstance(actorId, instanceId);
        return options(instanceId, false);
    }

    /** Returns eligible recipients for a process node without requiring a human actor. */
    @Nonnull
    @Transactional(readOnly = true)
    public List<ProcessAssignmentOptionDTO> runtimeInstanceOptions(@Nonnull Long instanceId,
                                                                    @Nonnull List<String> additionalPermissions) throws ResponseException {
        if (!instances.existsById(instanceId)) throw ResponseException.notFound();
        return options(instanceId, false).stream()
                .filter(option -> additionalPermissions.stream().allMatch(permission ->
                        permissions.hasProcessInstancePermission(option.id(), instanceId, permission)))
                .toList();
    }

    /** Rechecks the recipient immediately before a process node changes the assignment. */
    @Transactional(readOnly = true)
    public void requireRuntimeInstanceAssignee(@Nonnull Long instanceId, @Nonnull String userId) throws ResponseException {
        validateAssignee(userId, instanceId, false);
    }

    @Nonnull
    @Transactional(readOnly = true)
    public List<ProcessAssignmentOptionDTO> taskOptions(@Nonnull String actorId, @Nonnull Long taskId) throws ResponseException {
        var task = requireAssignableTask(actorId, taskId);
        return options(task.getProcessInstanceId(), true);
    }

    @Nonnull
    @Transactional(rollbackFor = ResponseException.class)
    public ProcessInstanceEntity reassignInstance(@Nonnull UserEntity actor, @Nonnull Long instanceId,
                                                  @Nullable String assignedUserId) throws ResponseException {
        var instance = requireAssignableInstance(actor.getId(), instanceId);
        validateAssignee(assignedUserId, instanceId, false);
        var previousUserId = instance.getAssignedUserId();
        instance.setAssignedUserId(assignedUserId).setUpdated(Instant.now());
        var result = instances.saveAndFlush(instance);
        audit.create().withUser(actor).withAuditAction(AuditAction.Update, ProcessInstanceEntity.class, instanceId,
                        "id", Map.of("id", instanceId, "processDefinitionId", instance.getProcessId()))
                .withDiff(Collections.singletonMap("assignedUserId", previousUserId), Collections.singletonMap("assignedUserId", assignedUserId))
                .withMessage("Die Zuweisung des Vorgangs mit der ID %s wurde geändert.", instanceId).log();
        return result;
    }

    @Nonnull
    @Transactional(rollbackFor = ResponseException.class)
    public ProcessInstanceTaskEntity reassignTask(@Nonnull UserEntity actor, @Nonnull Long taskId,
                                                  @Nullable String assignedUserId) throws ResponseException {
        var instanceId = tasks.findInstanceIdById(taskId).orElseThrow(ResponseException::notFound);
        instances.lockAccessById(instanceId).orElseThrow(ResponseException::notFound);
        // Load the task and evaluate both parties' rights only after acquiring the instance lock.
        var task = requireAssignableTask(actor.getId(), taskId);
        // Staff may transfer tasks; clearing an assignment is reserved for internal lifecycle operations.
        if (assignedUserId == null) {
            throw ResponseException.badRequest("Die Zuweisung einer Aufgabe kann nicht aufgehoben werden. Bitte wählen Sie eine andere Person aus.");
        }
        validateAssignee(assignedUserId, task.getProcessInstanceId(), true);
        var previousUserId = task.getAssignedUserId();
        task.setAssignedUserId(assignedUserId).setUpdated(Instant.now());
        var result = tasks.saveAndFlush(task);
        audit.create().withUser(actor).withAuditAction(AuditAction.Update, ProcessInstanceTaskEntity.class, taskId,
                        "id", Map.of("id", taskId, "processInstanceId", task.getProcessInstanceId()))
                .withDiff(Collections.singletonMap("assignedUserId", previousUserId), Collections.singletonMap("assignedUserId", assignedUserId))
                .withMessage("Die Zuweisung der Aufgabe mit der ID %s wurde geändert.", taskId).log();
        return result;
    }

    /**
     * Runtime assignments use the same lock and eligibility rules as manual assignments.
     */
    @Transactional(rollbackFor = ResponseException.class)
    public void saveRuntimeAssignment(@Nonnull ProcessInstanceTaskEntity task, @Nonnull String assignedUserId) throws ResponseException {
        instances.lockAccessById(task.getProcessInstanceId()).orElseThrow(ResponseException::notFound);
        validateAssignee(assignedUserId, task.getProcessInstanceId(), true);
        task.setAssignedUserId(assignedUserId);
        tasks.saveAndFlush(task);
    }

    /**
     * Check the same account and instance rights as persistence before applying node preferences or load balancing.
     * Node-specific permissions may extend, but never replace, the read and edit permissions required for every task.
     * This does not define the configured candidate pool; the resolver must establish that pool separately.
     */
    public boolean canReceiveTaskAssignment(@Nonnull String userId, @Nonnull Long instanceId,
                                            @Nonnull List<String> requiredPermissions) {
        var permissionsToCheck = Stream.concat(TASK_ASSIGNMENT_PERMISSIONS.stream(), requiredPermissions.stream())
                .distinct().toList();
        return users.findById(userId)
                .map(user -> canReceiveAssignment(user, instanceId, permissionsToCheck))
                .orElse(false);
    }

    @Nonnull
    private ProcessInstanceEntity requireAssignableInstance(@Nonnull String actorId, @Nonnull Long instanceId) throws ResponseException {
        permissions.requireProcessInstancePermission(actorId, instanceId, PROCESS_INSTANCE_REASSIGN);
        var instance = instances.findById(instanceId).orElseThrow(ResponseException::notFound);
        if (instance.getStatus() == ProcessInstanceStatus.Completed || instance.getStatus() == ProcessInstanceStatus.Aborted) {
            throw ResponseException.badRequest("Die Zuweisung abgeschlossener oder abgebrochener Vorgänge kann nicht mehr geändert werden.");
        }
        return instance;
    }

    @Nonnull
    private ProcessInstanceTaskEntity requireAssignableTask(@Nonnull String actorId, @Nonnull Long taskId) throws ResponseException {
        var task = tasks.findById(taskId).orElseThrow(ResponseException::notFound);
        permissions.requireProcessInstancePermission(actorId, task.getProcessInstanceId(), PROCESS_INSTANCE_EDIT_TASK);
        if (!ACTIVE_TASK_STATUSES.contains(task.getStatus())) {
            throw ResponseException.badRequest("Nur aktive Aufgaben können neu zugewiesen werden.");
        }
        return task;
    }

    @Nonnull
    private List<ProcessAssignmentOptionDTO> options(@Nonnull Long instanceId, boolean forTask) {
        return users.findAllByEnabledTrueAndDeletedInIdpFalseOrderByFullNameAsc().stream()
                .filter(user -> canReceiveAssignment(user, instanceId, forTask))
                .map(user -> new ProcessAssignmentOptionDTO(user.getId(), user.getFullName(), user.getEmail()))
                .toList();
    }

    private void validateAssignee(@Nullable String userId, @Nonnull Long instanceId, boolean forTask) throws ResponseException {
        // Instance assignments can be cleared even when the previous assignee has lost access.
        if (userId == null) return;
        var user = users.findById(userId).orElseThrow(() -> ResponseException.badRequest("Die ausgewählte Person wurde nicht gefunden."));
        if (!canReceiveAssignment(user, instanceId, forTask)) {
            throw ResponseException.badRequest("Die ausgewählte Person ist nicht aktiv oder hat nicht die erforderlichen eigenen Berechtigungen für diesen Vorgang. Berechtigungen aus einer Stellvertretung reichen für eine Zuweisung nicht aus.");
        }
    }

    private boolean canReceiveAssignment(@Nonnull UserEntity user, @Nonnull Long instanceId, boolean forTask) {
        return canReceiveAssignment(user, instanceId, forTask ? TASK_ASSIGNMENT_PERMISSIONS : List.of(PROCESS_INSTANCE_READ));
    }

    private boolean canReceiveAssignment(@Nonnull UserEntity user, @Nonnull Long instanceId, @Nonnull List<String> requiredPermissions) {
        return Boolean.TRUE.equals(user.getEnabled()) && Boolean.FALSE.equals(user.getDeletedInIdp())
                && requiredPermissions.stream().allMatch(permission ->
                        permissions.hasProcessInstancePermissionWithoutDeputies(user.getId(), instanceId, permission));
    }
}
