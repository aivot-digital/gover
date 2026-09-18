package de.aivot.prosuna.backend.system.services;

import de.aivot.prosuna.backend.config.services.SystemConfigService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessNodeRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessRepository;
import de.aivot.prosuna.backend.process.services.ProcessNodeDefinitionService;
import de.aivot.prosuna.backend.process.projections.DashboardActivityBucketProjection;
import de.aivot.prosuna.backend.process.projections.DashboardTaskCountsProjection;
import de.aivot.prosuna.backend.system.configs.DashboardActivityEnabledSystemConfigDefinition;
import de.aivot.prosuna.backend.system.configs.DashboardActivityPeriodSystemConfigDefinition;
import de.aivot.prosuna.backend.system.enums.DashboardActivityPeriod;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DashboardServiceTest {
    @Test
    void activityShouldBeUnavailableWithoutInstanceAccess() throws ResponseException {
        var fixture = fixture();
        when(fixture.permissionService.hasInAnyProcessInstancePermission(
                "user-1",
                ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ
        )).thenReturn(false);

        var activity = fixture.service.getActivity(user());

        assertFalse(activity.available());
        assertTrue(activity.buckets().isEmpty());
        verify(fixture.instanceRepository, never()).getDashboardActivity(
                any(), anyBoolean(), any(), anyInt(), any(), any(), anyInt()
        );
    }

    @Test
    void activityShouldAggregateAccessibleInstances() throws ResponseException {
        var fixture = fixture();
        var bucket = mock(DashboardActivityBucketProjection.class);
        when(bucket.getPeriodStart()).thenReturn(LocalDate.of(2026, 8, 3));
        when(bucket.getStartedCount()).thenReturn(5L);
        when(bucket.getCompletedCount()).thenReturn(3L);
        when(fixture.permissionService.hasInAnyProcessInstancePermission(
                "user-1",
                ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ
        )).thenReturn(true);
        when(fixture.permissionService.hasSystemPermission(
                "user-1",
                ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ
        )).thenReturn(false);
        when(fixture.instanceRepository.getDashboardActivity(
                eq("user-1"),
                eq(false),
                eq(ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ),
                eq(ProcessInstanceStatus.Completed.ordinal()),
                any(LocalDate.class),
                any(LocalDate.class),
                eq(7)
        )).thenReturn(List.of(bucket));
        when(fixture.instanceRepository.countActiveDashboardInstances(
                "user-1",
                false,
                ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ,
                ProcessInstanceStatus.Running.ordinal()
        )).thenReturn(2L);

        var activity = fixture.service.getActivity(user());

        assertTrue(activity.available());
        assertEquals(5, activity.started());
        assertEquals(3, activity.completed());
        assertEquals(2, activity.active());
        assertEquals(LocalDate.of(2026, 8, 3), activity.buckets().getFirst().periodStart());
        assertEquals(DashboardActivityPeriod.ThreeMonths, activity.period());
    }

    @Test
    void activityShouldBeUnavailableWhenDisabledBySystemConfig() throws ResponseException {
        var fixture = fixture();
        when(fixture.systemConfigService.getValue(DashboardActivityEnabledSystemConfigDefinition.KEY))
                .thenReturn(false);

        var activity = fixture.service.getActivity(user());

        assertFalse(activity.available());
        verify(fixture.permissionService, never()).hasInAnyProcessInstancePermission(any(), any());
        verify(fixture.instanceRepository, never()).getDashboardActivity(
                any(), anyBoolean(), any(), anyInt(), any(), any(), anyInt()
        );
    }

    @Test
    void overviewShouldUseDomainScopedProcessPermissions() {
        var fixture = fixture();
        mockEmptyTasks(fixture);
        var process = mock(ProcessEntity.class);
        when(process.getId()).thenReturn(7);
        when(process.getInternalTitle()).thenReturn("Baugenehmigung");
        when(process.getDraftedVersion()).thenReturn(4);
        when(process.getUpdated()).thenReturn(Instant.parse("2026-08-03T10:15:30Z"));
        when(fixture.permissionService.hasSystemPermission(
                "user-1",
                ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE
        )).thenReturn(false);
        when(fixture.processRepository.findDashboardProcesses(
                eq("user-1"),
                eq(false),
                eq(ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE),
                any(Pageable.class)
        )).thenReturn(List.of(process));

        var overview = fixture.service.getOverview(user());

        assertEquals(1, overview.recentProcesses().size());
        assertEquals("Baugenehmigung", overview.recentProcesses().getFirst().title());
        verify(fixture.processRepository).findDashboardProcesses(
                eq("user-1"),
                eq(false),
                eq(ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE),
                any(Pageable.class)
        );
    }

    @Test
    void overviewShouldPassSystemWideProcessAccessToRepository() {
        var fixture = fixture();
        mockEmptyTasks(fixture);
        when(fixture.permissionService.hasSystemPermission(
                "user-1",
                ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE
        )).thenReturn(true);
        when(fixture.processRepository.findDashboardProcesses(
                eq("user-1"),
                eq(true),
                eq(ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE),
                any(Pageable.class)
        )).thenReturn(List.of());

        fixture.service.getOverview(user());

        verify(fixture.processRepository).findDashboardProcesses(
                eq("user-1"),
                eq(true),
                eq(ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE),
                any(Pageable.class)
        );
    }

    private static void mockEmptyTasks(Fixture fixture) {
        var counts = mock(DashboardTaskCountsProjection.class);
        when(counts.getTotalCount()).thenReturn(0L);
        when(counts.getOverdueCount()).thenReturn(0L);
        when(fixture.taskRepository.findDashboardTasks(any(), anyShort(), anyBoolean(), any(), any()))
                .thenReturn(List.of());
        when(fixture.taskRepository.getDashboardTaskCounts(any(), anyShort(), anyBoolean(), any(), any()))
                .thenReturn(counts);
    }

    private static UserEntity user() {
        return new UserEntity().setId("user-1");
    }

    private static Fixture fixture() {
        var permissionService = mock(PermissionService.class);
        var taskRepository = mock(ProcessInstanceTaskRepository.class);
        var instanceRepository = mock(ProcessInstanceRepository.class);
        var processRepository = mock(ProcessRepository.class);
        var nodeRepository = mock(ProcessNodeRepository.class);
        var nodeDefinitionService = mock(ProcessNodeDefinitionService.class);
        var systemConfigService = mock(SystemConfigService.class);
        try {
            when(systemConfigService.getValue(DashboardActivityEnabledSystemConfigDefinition.KEY)).thenReturn(true);
            when(systemConfigService.getValue(DashboardActivityPeriodSystemConfigDefinition.KEY))
                    .thenReturn(DashboardActivityPeriod.ThreeMonths.getConfigValue());
        } catch (ResponseException exception) {
            throw new IllegalStateException(exception);
        }
        var service = new DashboardService(
                permissionService,
                taskRepository,
                instanceRepository,
                processRepository,
                nodeRepository,
                nodeDefinitionService,
                systemConfigService
        );
        return new Fixture(
                service,
                permissionService,
                taskRepository,
                instanceRepository,
                processRepository,
                systemConfigService
        );
    }

    private record Fixture(
            DashboardService service,
            PermissionService permissionService,
            ProcessInstanceTaskRepository taskRepository,
            ProcessInstanceRepository instanceRepository,
            ProcessRepository processRepository,
            SystemConfigService systemConfigService
    ) {
    }
}
