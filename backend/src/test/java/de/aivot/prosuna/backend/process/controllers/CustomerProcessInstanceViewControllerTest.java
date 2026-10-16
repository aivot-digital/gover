package de.aivot.prosuna.backend.process.controllers;

import de.aivot.prosuna.backend.asset.services.AssetService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
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
import de.aivot.prosuna.backend.process.services.ProcessSubmissionBindingService;
import de.aivot.prosuna.backend.process.services.ProcessVersionService;
import de.aivot.prosuna.backend.theme.entities.ThemeEntity;
import de.aivot.prosuna.backend.theme.services.ThemeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerProcessInstanceViewControllerTest {
    private static final String INSTANCE_ACCESS_KEY = "instance-access-key";
    private static final String SUBMISSION_SECRET = "submission-secret";

    private ProcessInstanceEntity instance;
    private ProcessVersionEntity processVersion;
    private ProcessEntity process;
    private UUID logoKey;
    private UUID faviconKey;
    private ProcessInstanceTaskService processInstanceTaskService;
    private ProcessSubmissionBindingService processSubmissionBindingService;
    private ProcessInstanceService processInstanceService;
    private ThemeService themeService;
    private CustomerProcessInstanceViewController controller;

    @BeforeEach
    void setUp() throws ResponseException {
        instance = new ProcessInstanceEntity()
                .setId(42L)
                .setCaseNumber("CASE-1")
                .setAccessKey(INSTANCE_ACCESS_KEY)
                .setProcessId(7)
                .setInitialProcessVersion(3)
                .setStatus(ProcessInstanceStatus.Running);

        processVersion = new ProcessVersionEntity()
                .setProcessId(instance.getProcessId())
                .setProcessVersion(instance.getInitialProcessVersion())
                .setStatus(ProcessVersionStatus.Published)
                .setPublicTitle("Public title")
                .setAccessibilityDepartmentId(11)
                .setPrivacyDepartmentId(12)
                .setImprintDepartmentId(13);
        process = new ProcessEntity()
                .setId(instance.getProcessId())
                .setDepartmentId(20);
        logoKey = UUID.randomUUID();
        faviconKey = UUID.randomUUID();
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

        processInstanceService = mock(ProcessInstanceService.class);
        when(processInstanceService.retrieveByAccessKey(INSTANCE_ACCESS_KEY))
                .thenReturn(Optional.of(instance));

        var processVersionService = mock(ProcessVersionService.class);
        when(processVersionService.retrieve(ProcessVersionEntityId.of(instance.getProcessId(), instance.getInitialProcessVersion())))
                .thenReturn(Optional.of(processVersion));

        processInstanceTaskService = mock(ProcessInstanceTaskService.class);
        processSubmissionBindingService = mock(ProcessSubmissionBindingService.class);
        var processService = mock(ProcessService.class);
        when(processService.retrieve(instance.getProcessId())).thenReturn(Optional.of(process));
        themeService = mock(ThemeService.class);
        when(themeService.resolveProcessTheme(processVersion, process.getDepartmentId())).thenReturn(resolvedTheme);
        var assetService = mock(AssetService.class);
        when(assetService.createUrl(logoKey)).thenReturn("https://assets.example/logo");
        when(assetService.createUrl(faviconKey)).thenReturn("https://assets.example/favicon");

        controller = new CustomerProcessInstanceViewController(
                processInstanceService,
                processInstanceTaskService,
                processVersionService,
                processService,
                themeService,
                assetService,
                processSubmissionBindingService
        );
    }

    @Test
    void retrieveShouldSupportNonUuidProcessAccessKeys() throws ResponseException {
        var task = createTask(9L, "task-access-key");
        when(processInstanceTaskService.retrieveByProcessInstanceIdAndAccessKey(instance.getId(), "task-access-key"))
                .thenReturn(Optional.of(task));

        var response = controller.retrieve(INSTANCE_ACCESS_KEY, "task-access-key", null);

        assertEquals(processVersion.getPublicTitle(), response.title());
        assertEquals(ProcessInstanceStatus.Running, response.status());
        assertEquals("task-access-key", response.tasks().getFirst().accessKey());
        assertEquals(processVersion.getAccessibilityDepartmentId(), response.accessibilityDepartmentId());
        assertEquals(processVersion.getPrivacyDepartmentId(), response.privacyDepartmentId());
        assertEquals(processVersion.getImprintDepartmentId(), response.imprintDepartmentId());
        assertEquals("#111111", response.theme().primaryColor());
        assertEquals("https://assets.example/logo", response.theme().logoUrl());
        assertEquals("https://assets.example/logo", response.theme().logoUrlDark());
        assertEquals("https://assets.example/favicon", response.theme().faviconUrl());
        verify(themeService).resolveProcessTheme(processVersion, process.getDepartmentId());
        verify(processInstanceService).retrieveByAccessKey(INSTANCE_ACCESS_KEY);
    }

    @Test
    void retrieveShouldNotListTasksForTheInstanceAccessKeyAlone() throws ResponseException {
        var response = controller.retrieve(INSTANCE_ACCESS_KEY, null, null);

        assertTrue(response.tasks().isEmpty());
        verify(processInstanceTaskService, never()).list(any(Pageable.class), any(ProcessInstanceTaskFilter.class));
        verify(processInstanceTaskService, never()).retrieveByProcessInstanceIdAndAccessKey(anyLong(), anyString());
    }

    @Test
    void retrieveShouldOnlyListTheTaskOfThePresentedTaskAccessKey() throws ResponseException {
        var task = createTask(10L, "own-task-access-key");
        when(processInstanceTaskService.retrieveByProcessInstanceIdAndAccessKey(instance.getId(), "own-task-access-key"))
                .thenReturn(Optional.of(task));

        var response = controller.retrieve(INSTANCE_ACCESS_KEY, "own-task-access-key", null);

        assertEquals(1, response.tasks().size());
        assertEquals("own-task-access-key", response.tasks().getFirst().accessKey());
        verify(processInstanceTaskService, never()).list(any(Pageable.class), any(ProcessInstanceTaskFilter.class));
    }

    @Test
    void retrieveShouldIgnoreTaskAccessKeysOfOtherInstances() throws ResponseException {
        when(processInstanceTaskService.retrieveByProcessInstanceIdAndAccessKey(instance.getId(), "foreign-task-access-key"))
                .thenReturn(Optional.empty());

        var response = controller.retrieve(INSTANCE_ACCESS_KEY, "foreign-task-access-key", null);

        assertTrue(response.tasks().isEmpty());
    }

    @Test
    void retrieveShouldListTheFirstTaskForTheSubmittingBrowser() throws ResponseException {
        var firstTask = createTask(1L, "first-task-access-key");
        when(processSubmissionBindingService.isBound(SUBMISSION_SECRET, instance.getId())).thenReturn(true);
        when(processInstanceTaskService.list(any(Pageable.class), any(ProcessInstanceTaskFilter.class)))
                .thenReturn(new PageImpl<>(List.of(firstTask)));

        var response = controller.retrieve(INSTANCE_ACCESS_KEY, null, SUBMISSION_SECRET);

        assertEquals(1, response.tasks().size());
        assertEquals("first-task-access-key", response.tasks().getFirst().accessKey());
        verify(processInstanceTaskService).list(
                argThat((Pageable pageable) -> pageable.getPageSize() == 1 &&
                        pageable.getPageNumber() == 0 &&
                        Sort.by(Sort.Direction.ASC, "started").equals(pageable.getSort())),
                argThat((ProcessInstanceTaskFilter filter) -> instance.getId().equals(filter.getProcessInstanceId()))
        );
    }

    @Test
    void retrieveShouldNotListTheFirstTaskForUnboundSubmissionSecrets() throws ResponseException {
        when(processSubmissionBindingService.isBound("other-secret", instance.getId())).thenReturn(false);

        var response = controller.retrieve(INSTANCE_ACCESS_KEY, null, "other-secret");

        assertTrue(response.tasks().isEmpty());
        verify(processInstanceTaskService, never()).list(any(Pageable.class), any(ProcessInstanceTaskFilter.class));
    }

    @Test
    void retrieveShouldListTheFirstTaskOnceIfItIsAlsoThePresentedTask() throws ResponseException {
        var firstTask = createTask(1L, "first-task-access-key");
        when(processSubmissionBindingService.isBound(SUBMISSION_SECRET, instance.getId())).thenReturn(true);
        when(processInstanceTaskService.list(any(Pageable.class), any(ProcessInstanceTaskFilter.class)))
                .thenReturn(new PageImpl<>(List.of(firstTask)));
        when(processInstanceTaskService.retrieveByProcessInstanceIdAndAccessKey(instance.getId(), "first-task-access-key"))
                .thenReturn(Optional.of(createTask(1L, "first-task-access-key")));

        var response = controller.retrieve(INSTANCE_ACCESS_KEY, "first-task-access-key", SUBMISSION_SECRET);

        assertEquals(1, response.tasks().size());
    }

    private ProcessInstanceTaskEntity createTask(Long id, String accessKey) {
        var now = Instant.now();
        return new ProcessInstanceTaskEntity(
                id,
                accessKey,
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
    }
}
