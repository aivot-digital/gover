package de.aivot.prosuna.backend.system.services;

import de.aivot.prosuna.backend.config.services.SystemConfigService;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessNodeRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessRepository;
import de.aivot.prosuna.backend.process.services.ProcessNodeDefinitionService;
import de.aivot.prosuna.backend.system.dtos.DashboardActivityDTO;
import de.aivot.prosuna.backend.system.dtos.DashboardOverviewDTO;
import de.aivot.prosuna.backend.system.configs.DashboardActivityEnabledSystemConfigDefinition;
import de.aivot.prosuna.backend.system.configs.DashboardActivityPeriodSystemConfigDefinition;
import de.aivot.prosuna.backend.system.enums.DashboardActivityPeriod;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.utils.ApplicationTimeZone;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import jakarta.annotation.Nonnull;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    private static final int TASK_PREVIEW_SIZE = 4;
    private static final int RECENT_PROCESS_SIZE = 3;

    private final PermissionService permissionService;
    private final ProcessInstanceTaskRepository taskRepository;
    private final ProcessInstanceRepository instanceRepository;
    private final ProcessRepository processRepository;
    private final ProcessNodeRepository nodeRepository;
    private final ProcessNodeDefinitionService nodeDefinitionService;
    private final SystemConfigService systemConfigService;

    public DashboardService(PermissionService permissionService,
                            ProcessInstanceTaskRepository taskRepository,
                            ProcessInstanceRepository instanceRepository,
                            ProcessRepository processRepository,
                            ProcessNodeRepository nodeRepository,
                            ProcessNodeDefinitionService nodeDefinitionService,
                            SystemConfigService systemConfigService) {
        this.permissionService = permissionService;
        this.taskRepository = taskRepository;
        this.instanceRepository = instanceRepository;
        this.processRepository = processRepository;
        this.nodeRepository = nodeRepository;
        this.nodeDefinitionService = nodeDefinitionService;
        this.systemConfigService = systemConfigService;
    }

    @Nonnull
    public DashboardOverviewDTO getOverview(@Nonnull UserEntity user) {
        return new DashboardOverviewDTO(
                getTaskSummary(user),
                getRecentProcesses(user)
        );
    }

    @Nonnull
    public DashboardActivityDTO getActivity(@Nonnull UserEntity user) throws ResponseException {
        var period = getActivityPeriod();
        if (!getActivityEnabled()) {
            return unavailableActivity(period);
        }

        var permission = ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ;
        // Missing access hides the widget; an authorized user with zero activity gets a meaningful empty state instead.
        if (!permissionService.hasInAnyProcessInstancePermission(user.getId(), permission)) {
            return unavailableActivity(period);
        }
        var hasSystemAccess = permissionService.hasSystemPermission(user.getId(), permission);
        var today = LocalDate.now(ApplicationTimeZone.getZoneId());
        var buckets = instanceRepository
                .getDashboardActivity(
                        user.getId(),
                        hasSystemAccess,
                        permission,
                        ProcessInstanceStatus.Completed.ordinal(),
                        period.getFirstBucketStart(today),
                        period.getLastBucketStart(today),
                        period.getBucketDays()
                )
                .stream()
                .map(bucket -> new DashboardActivityDTO.Bucket(
                        bucket.getPeriodStart(),
                        bucket.getStartedCount(),
                        bucket.getCompletedCount()
                ))
                .toList();

        return new DashboardActivityDTO(
                true,
                period,
                buckets.stream().mapToLong(DashboardActivityDTO.Bucket::started).sum(),
                buckets.stream().mapToLong(DashboardActivityDTO.Bucket::completed).sum(),
                instanceRepository.countActiveDashboardInstances(
                        user.getId(),
                        hasSystemAccess,
                        permission,
                        ProcessInstanceStatus.Running.ordinal()
                ),
                buckets
        );
    }

    private boolean getActivityEnabled() throws ResponseException {
        return Boolean.TRUE.equals(systemConfigService.getValue(DashboardActivityEnabledSystemConfigDefinition.KEY));
    }

    @Nonnull
    private DashboardActivityPeriod getActivityPeriod() throws ResponseException {
        var configValue = (String) systemConfigService.getValue(DashboardActivityPeriodSystemConfigDefinition.KEY);
        return DashboardActivityPeriod.fromConfigValue(configValue);
    }

    @Nonnull
    private static DashboardActivityDTO unavailableActivity(@Nonnull DashboardActivityPeriod period) {
        return new DashboardActivityDTO(false, period, 0, 0, 0, Collections.emptyList());
    }

    @Nonnull
    private DashboardOverviewDTO.TaskSummary getTaskSummary(@Nonnull UserEntity user) {
        var permission = ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ;
        var hasSystemAccess = permissionService.hasSystemPermission(user.getId(), permission);
        var tasks = taskRepository.findDashboardTasks(
                user.getId(),
                ProcessTaskStatus.Running.getDatabaseValue(),
                hasSystemAccess,
                permission,
                PageRequest.of(0, TASK_PREVIEW_SIZE)
        );
        var counts = taskRepository.getDashboardTaskCounts(
                user.getId(),
                ProcessTaskStatus.Running.getDatabaseValue(),
                hasSystemAccess,
                permission,
                Instant.now()
        );

        // Resolve related rows in batches: task entities only carry IDs, while the dashboard needs human-readable labels.
        var instances = instanceRepository
                .findAllById(tasks.stream().map(task -> task.getProcessInstanceId()).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(ProcessInstanceEntity::getId, Function.identity()));
        var processes = processRepository
                .findAllById(tasks.stream().map(task -> task.getProcessId()).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(ProcessEntity::getId, Function.identity()));
        var nodes = nodeRepository
                .findAllById(tasks.stream().map(task -> task.getProcessNodeId()).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(ProcessNodeEntity::getId, Function.identity()));

        var items = tasks.stream().map(task -> {
            var instance = instances.get(task.getProcessInstanceId());
            var process = processes.get(task.getProcessId());
            var node = nodes.get(task.getProcessNodeId());

            return new DashboardOverviewDTO.Task(
                    task.getId(),
                    task.getProcessInstanceId(),
                    task.getProcessId(),
                    task.getProcessVersion(),
                    resolveTaskName(node),
                    process != null ? process.getInternalTitle() : "Prozess #" + task.getProcessId(),
                    instance != null ? instance.getCaseNumber() : "Vorgang #" + task.getProcessInstanceId(),
                    task.getStarted(),
                    task.getDeadline()
            );
        }).toList();

        return new DashboardOverviewDTO.TaskSummary(counts.getTotalCount(), counts.getOverdueCount(), items);
    }

    @Nonnull
    private String resolveTaskName(ProcessNodeEntity node) {
        if (node == null) {
            return "Aufgabe";
        }

        return nodeDefinitionService
                .getProcessNodeDefinition(node)
                .map(node::resolveName)
                .filter(name -> !name.isBlank())
                .orElseGet(() -> node.getName() != null && !node.getName().isBlank() ? node.getName() : "Aufgabe");
    }

    @Nonnull
    private List<DashboardOverviewDTO.RecentProcess> getRecentProcesses(@Nonnull UserEntity user) {
        var permission = ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE;
        var processes = processRepository.findDashboardProcesses(
                user.getId(),
                permissionService.hasSystemPermission(user.getId(), permission),
                permission,
                PageRequest.of(0, RECENT_PROCESS_SIZE)
        );

        return processes.stream()
                .map(process -> new DashboardOverviewDTO.RecentProcess(
                        process.getId(),
                        process.getInternalTitle(),
                        process.getDraftedVersion(),
                        process.getPublishedVersion(),
                        process.getUpdated()
                ))
                .toList();
    }
}
