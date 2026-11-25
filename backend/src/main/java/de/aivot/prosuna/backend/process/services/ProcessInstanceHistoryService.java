package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.dtos.ProcessInstanceHistoryTasksDTO;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class ProcessInstanceHistoryService {
    private static final Sort ORDER = Sort.by("started", "id");
    private static final List<ProcessTaskStatus> ACTIVE = List.of(ProcessTaskStatus.Running,
            ProcessTaskStatus.InProgress, ProcessTaskStatus.AwaitingStaff, ProcessTaskStatus.Paused,
            ProcessTaskStatus.AwaitingCustomer, ProcessTaskStatus.AwaitingPayment);
    private final ProcessInstanceTaskRepository tasks;
    private final PermissionService permissions;

    public ProcessInstanceHistoryService(@Nonnull ProcessInstanceTaskRepository tasks,
                                         @Nonnull PermissionService permissions) {
        this.tasks = tasks;
        this.permissions = permissions;
    }

    @Nonnull
    @Transactional(readOnly = true)
    public ProcessInstanceHistoryTasksDTO getTasks(@Nonnull String userId, long instanceId,
                                                   @Nonnull Pageable pageable, @Nullable String target) throws ResponseException {
        permissions.requireProcessInstancePermission(userId, instanceId, ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ);
        Specification<ProcessInstanceTaskEntity> visible = (root, query, builder) -> builder.and(
                builder.equal(root.get("processInstanceId"), instanceId),
                builder.notEqual(root.get("status"), ProcessTaskStatus.Restarted));
        var size = Math.min(pageable.getPageSize(), 100);
        var count = tasks.count(visible);
        var page = Math.min(pageable.getPageNumber(), Math.max(0, (count - 1) / size));
        var selected = resolveTarget(instanceId, visible, target);
        if (selected.isPresent()) {
            var task = selected.get();
            // Count visible predecessors in the same stable order used for the page itself.
            page = tasks.count(visible.and((root, query, builder) -> builder.or(
                    builder.lessThan(root.get("started"), task.getStarted()),
                    builder.and(builder.equal(root.get("started"), task.getStarted()),
                            builder.lessThan(root.get("id"), task.getId()))))) / size;
        }
        var taskPage = tasks.findAll(visible, PageRequest.of((int) page, size, ORDER));
        var predecessorIds = taskPage.stream().map(ProcessInstanceTaskEntity::getPreviousProcessInstanceTaskId)
                .filter(Objects::nonNull).distinct().toList();
        var predecessors = predecessorIds.isEmpty() ? List.<ProcessInstanceTaskEntity>of()
                : tasks.findAllById(predecessorIds).stream()
                .filter(task -> task.getProcessInstanceId().equals(instanceId)).toList();
        // Preview metadata must include active tasks outside the selected history page.
        var active = tasks.findAll(visible.and((root, query, builder) -> root.get("status").in(ACTIVE)), ORDER)
                .stream().map(task -> new ProcessInstanceHistoryTasksDTO.ActiveTask(
                        task.getProcessNodeId(), task.getProcessVersion(), task.getStatus())).toList();
        return new ProcessInstanceHistoryTasksDTO(taskPage, predecessors, active);
    }

    @Nonnull
    private Optional<ProcessInstanceTaskEntity> resolveTarget(long instanceId,
                                                              @Nonnull Specification<ProcessInstanceTaskEntity> visible,
                                                              @Nullable String target) {
        if (target == null) return Optional.empty();
        if (target.equals("active") || target.equals("current")) {
            var active = first(visible.and((root, query, builder) -> root.get("status").in(ACTIVE)), ORDER);
            if (active.isPresent() || target.equals("active")) return active;
            return first(visible, ORDER.descending());
        }
        if (target.equals("failed")) {
            return first(visible.and((root, query, builder) -> builder.equal(root.get("status"), ProcessTaskStatus.Failed)), ORDER);
        }
        if (target.equals("latest")) return first(visible, ORDER.descending());
        if (!target.matches("[1-9][0-9]{0,15}")) return Optional.empty();
        var id = Long.parseLong(target);
        if (id > 9_007_199_254_740_991L) return Optional.empty();
        return tasks.findById(id).filter(task -> task.getProcessInstanceId().equals(instanceId)
                && task.getStatus() != ProcessTaskStatus.Restarted);
    }

    @Nonnull
    private Optional<ProcessInstanceTaskEntity> first(@Nonnull Specification<ProcessInstanceTaskEntity> specification,
                                                      @Nonnull Sort sort) {
        return tasks.findAll(specification, PageRequest.of(0, 1, sort)).stream().findFirst();
    }
}
