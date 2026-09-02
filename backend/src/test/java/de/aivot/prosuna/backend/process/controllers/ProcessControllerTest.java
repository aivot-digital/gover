package de.aivot.prosuna.backend.process.controllers;

import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.audit.services.ScopedAuditService;
import de.aivot.prosuna.backend.department.services.DepartmentService;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAccessControlPresetEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntityId;
import de.aivot.prosuna.backend.process.enums.ProcessVersionStatus;
import de.aivot.prosuna.backend.process.filters.ProcessInstanceAccessControlPresetFilter;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.prosuna.backend.process.repositories.ProcessVersionRepository;
import de.aivot.prosuna.backend.process.services.ProcessEdgeService;
import de.aivot.prosuna.backend.process.services.ProcessExportService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAccessControlPresetService;
import de.aivot.prosuna.backend.process.services.ProcessNodeDefinitionService;
import de.aivot.prosuna.backend.process.services.ProcessNodeService;
import de.aivot.prosuna.backend.process.services.ProcessService;
import de.aivot.prosuna.backend.process.services.ProcessVersionService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessControllerTest {
    @Test
    void newVersionFromExistingShouldCopyProcessInstanceAccessControlPresetsToDraftVersion() throws Exception {
        var auditService = mock(AuditService.class);
        when(auditService.createScopedAuditService(ProcessController.class, "Prozesse"))
                .thenReturn(mock(ScopedAuditService.class));

        var userService = mock(UserService.class);
        var processDefinitionService = mock(ProcessService.class);
        var departmentService = mock(DepartmentService.class);
        var permissionService = mock(PermissionService.class);
        var processExportService = mock(ProcessExportService.class);
        var processDefinitionVersionRepository = mock(ProcessVersionRepository.class);
        var processDefinitionVersionService = mock(ProcessVersionService.class);
        var processDefinitionNodeService = mock(ProcessNodeService.class);
        var processDefinitionEdgeService = mock(ProcessEdgeService.class);
        var processInstanceAccessControlPresetService = mock(ProcessInstanceAccessControlPresetService.class);
        var processNodeProviderService = mock(ProcessNodeDefinitionService.class);

        var controller = new ProcessController(
                auditService,
                userService,
                processDefinitionService,
                departmentService,
                permissionService,
                processExportService,
                processDefinitionVersionRepository,
                processDefinitionVersionService,
                processDefinitionNodeService,
                processDefinitionEdgeService,
                processInstanceAccessControlPresetService,
                processNodeProviderService,
                new JsonMapper()
        );

        var user = new UserEntity()
                .setId("user-1")
                .setEmail("user@example.com")
                .setFirstName("Test")
                .setLastName("User")
                .setEnabled(true)
                .setVerified(true)
                .setDeletedInIdp(false);
        var process = new ProcessEntity(
                42,
                "Hundesteuer Antrag",
                10,
                UUID.randomUUID(),
                "hundesteuer-antrag",
                2,
                null,
                2,
                Instant.now(),
                Instant.now()
        );
        var originalProcessVersion = new ProcessVersionEntity(
                42,
                2,
                ProcessVersionStatus.Published,
                "Hundesteuer Antrag",
                null,
                null,
                Instant.now(),
                Instant.now(),
                Instant.now(),
                null
        ).setThemeId(77);

        var departmentPreset = new ProcessInstanceAccessControlPresetEntity()
                .setId(1)
                .setSourceDepartmentId(100)
                .setTargetProcessId(42)
                .setTargetProcessVersion(2)
                .setPermissions(List.of("task:edit", "task:read"));
        var teamPreset = new ProcessInstanceAccessControlPresetEntity()
                .setId(2)
                .setSourceTeamId(200)
                .setTargetProcessId(42)
                .setTargetProcessVersion(2)
                .setPermissions(List.of("task:edit"));

        when(userService.fromJWT(null))
                .thenReturn(Optional.of(user));
        when(processDefinitionService.retrieve(42))
                .thenReturn(Optional.of(process));
        when(processDefinitionVersionRepository.existsByProcessIdAndStatus(42, ProcessVersionStatus.Drafted))
                .thenReturn(false);
        when(processDefinitionVersionService.retrieve(ProcessVersionEntityId.of(42, 2)))
                .thenReturn(Optional.of(originalProcessVersion));
        when(processDefinitionVersionRepository.maxVersionForProcessDefinition(42))
                .thenReturn(Optional.of(2));
        when(processDefinitionNodeService.findAllByProcessIdAndProcessVersion(42, 2))
                .thenReturn(List.of());
        when(processDefinitionVersionService.create(any(ProcessVersionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(processInstanceAccessControlPresetService.performList(any(Pageable.class), any(), any()))
                .thenReturn(new PageImpl<>(List.of(departmentPreset, teamPreset)));
        when(processInstanceAccessControlPresetService.create(any(ProcessInstanceAccessControlPresetEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(processDefinitionEdgeService.findAllByProcessIdAndProcessVersion(42, 2))
                .thenReturn(List.of());

        var result = controller.newVersionFromExisting(null, 42, 2);

        assertEquals(3, result.getProcessVersion());
        assertEquals(77, result.getThemeId());
        verify(permissionService)
                .requireProcessPermission("user-1", 42, ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE);

        var filterCaptor = ArgumentCaptor.forClass(ProcessInstanceAccessControlPresetFilter.class);
        verify(processInstanceAccessControlPresetService)
                .performList(any(Pageable.class), any(), filterCaptor.capture());
        assertEquals(42, filterCaptor.getValue().getTargetProcessId());
        assertEquals(2, filterCaptor.getValue().getTargetProcessVersion());

        var presetCaptor = ArgumentCaptor.forClass(ProcessInstanceAccessControlPresetEntity.class);
        verify(processInstanceAccessControlPresetService, times(2))
                .create(presetCaptor.capture());

        var createdPresets = presetCaptor.getAllValues();
        assertEquals(100, createdPresets.get(0).getSourceDepartmentId());
        assertEquals(42, createdPresets.get(0).getTargetProcessId());
        assertEquals(3, createdPresets.get(0).getTargetProcessVersion());
        assertEquals(List.of("task:edit", "task:read"), createdPresets.get(0).getPermissions());

        assertEquals(200, createdPresets.get(1).getSourceTeamId());
        assertEquals(42, createdPresets.get(1).getTargetProcessId());
        assertEquals(3, createdPresets.get(1).getTargetProcessVersion());
        assertEquals(List.of("task:edit"), createdPresets.get(1).getPermissions());
    }
}
