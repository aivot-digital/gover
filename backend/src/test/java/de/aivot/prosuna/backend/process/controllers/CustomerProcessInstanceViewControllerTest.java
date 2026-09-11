package de.aivot.prosuna.backend.process.controllers;

import de.aivot.prosuna.backend.asset.services.AssetService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntityId;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.enums.ProcessVersionStatus;
import de.aivot.prosuna.backend.process.filters.ProcessInstanceTaskFilter;
import de.aivot.prosuna.backend.process.services.ProcessInstanceService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceTaskService;
import de.aivot.prosuna.backend.process.services.ProcessService;
import de.aivot.prosuna.backend.process.services.ProcessVersionService;
import de.aivot.prosuna.backend.theme.entities.ThemeEntity;
import de.aivot.prosuna.backend.theme.services.ThemeService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerProcessInstanceViewControllerTest {
    @Test
    void retrieveShouldSupportNonUuidProcessAccessKeys() throws ResponseException {
        var instanceAccessKey = "instance-access-key";
        var taskAccessKey = "task-access-key";
        var now = Instant.now();

        var instance = new ProcessInstanceEntity()
                .setId(42L)
                .setCaseNumber("CASE-1")
                .setAccessKey(instanceAccessKey)
                .setProcessId(7)
                .setInitialProcessVersion(3)
                .setStatus(ProcessInstanceStatus.Running);

        var processVersion = new ProcessVersionEntity()
                .setProcessId(instance.getProcessId())
                .setProcessVersion(instance.getInitialProcessVersion())
                .setStatus(ProcessVersionStatus.Published)
                .setPublicTitle("Public title")
                .setAccessibilityDepartmentId(11)
                .setPrivacyDepartmentId(12)
                .setImprintDepartmentId(13);
        var process = new ProcessEntity()
                .setId(instance.getProcessId())
                .setDepartmentId(20);
        var logoKey = UUID.randomUUID();
        var faviconKey = UUID.randomUUID();
        var resolvedTheme = new ThemeEntity(
                1,
                "Resolved theme",
                "#111111",
                "#222222",
                null,
                null,
                logoKey,
                null,
                faviconKey
        );

        var task = new ProcessInstanceTaskEntity(
                9L,
                taskAccessKey,
                instance.getId(),
                instance.getProcessId(),
                instance.getInitialProcessVersion(),
                11,
                null,
                null,
                null,
                ProcessTaskStatus.Completed,
                null,
                now,
                now,
                now,
                null,
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                null,
                null,
                null,
                null,
                null,
                null
        );

        var processInstanceService = mock(ProcessInstanceService.class);
        when(processInstanceService.retrieveByAccessKey(instanceAccessKey))
                .thenReturn(Optional.of(instance));

        var processVersionService = mock(ProcessVersionService.class);
        when(processVersionService.retrieve(ProcessVersionEntityId.of(instance.getProcessId(), instance.getInitialProcessVersion())))
                .thenReturn(Optional.of(processVersion));

        var processInstanceTaskService = mock(ProcessInstanceTaskService.class);
        when(processInstanceTaskService.list(any(Pageable.class), any(ProcessInstanceTaskFilter.class)))
                .thenReturn(new PageImpl<>(List.of(task)));
        var processService = mock(ProcessService.class);
        when(processService.retrieve(instance.getProcessId())).thenReturn(Optional.of(process));
        var themeService = mock(ThemeService.class);
        when(themeService.resolveProcessTheme(processVersion, process.getDepartmentId())).thenReturn(resolvedTheme);
        var assetService = mock(AssetService.class);
        when(assetService.createUrl(logoKey)).thenReturn("https://assets.example/logo");
        when(assetService.createUrl(faviconKey)).thenReturn("https://assets.example/favicon");
        var prosunaConfig = mock(ProsunaConfig.class);

        var controller = new CustomerProcessInstanceViewController(
                processInstanceService,
                processInstanceTaskService,
                processVersionService,
                processService,
                themeService,
                assetService,
                prosunaConfig
        );

        var response = controller.retrieve(instanceAccessKey);

        assertEquals(processVersion.getPublicTitle(), response.title());
        assertEquals(ProcessInstanceStatus.Running, response.status());
        assertEquals(taskAccessKey, response.tasks().getFirst().accessKey());
        assertEquals(processVersion.getAccessibilityDepartmentId(), response.accessibilityDepartmentId());
        assertEquals(processVersion.getPrivacyDepartmentId(), response.privacyDepartmentId());
        assertEquals(processVersion.getImprintDepartmentId(), response.imprintDepartmentId());
        assertEquals("#111111", response.theme().primaryColor());
        assertEquals("https://assets.example/logo", response.theme().logoUrl());
        assertEquals("https://assets.example/logo", response.theme().logoUrlDark());
        assertEquals("https://assets.example/favicon", response.theme().faviconUrl());
        verify(themeService).resolveProcessTheme(processVersion, process.getDepartmentId());
        verify(processInstanceService).retrieveByAccessKey(instanceAccessKey);
        verify(processInstanceTaskService).list(any(Pageable.class), argThat((ProcessInstanceTaskFilter filter) ->
                instance.getId().equals(filter.getProcessInstanceId())
        ));
    }
}
