package de.aivot.prosuna.backend.plugins.form.v1.nodes;

import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntityId;
import de.aivot.prosuna.backend.process.enums.ProcessVersionStatus;
import de.aivot.prosuna.backend.process.filters.ProcessNodeFilter;
import de.aivot.prosuna.backend.process.services.ProcessNodeService;
import de.aivot.prosuna.backend.process.services.ProcessService;
import de.aivot.prosuna.backend.process.services.ProcessVersionService;
import de.aivot.prosuna.backend.process.services.PublicUrlService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FormTriggerListControllerV1Test {
    private UserService userService;
    private ProcessService processService;
    private ProcessNodeService processNodeService;
    private ProcessVersionService processVersionService;
    private FormTriggerNodeV1 formTriggerNode;
    private PublicUrlService publicUrlService;
    private FormTriggerListControllerV1 controller;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        processService = mock(ProcessService.class);
        processNodeService = mock(ProcessNodeService.class);
        processVersionService = mock(ProcessVersionService.class);
        formTriggerNode = mock(FormTriggerNodeV1.class);
        publicUrlService = mock(PublicUrlService.class);
        controller = new FormTriggerListControllerV1(
                userService,
                processService,
                processNodeService,
                processVersionService,
                formTriggerNode,
                publicUrlService
        );

        when(formTriggerNode.getKey()).thenReturn("form:form:1");
    }

    @Test
    void listShouldReturnCompactFormOverviewData() throws Exception {
        var now = Instant.parse("2026-08-13T10:00:00Z");
        var pageable = PageRequest.of(0, 12);
        var user = new UserEntity().setId("user-1");
        var process = new ProcessEntity(
                42,
                "Hundesteuer",
                10,
                UUID.randomUUID(),
                "hundesteuer",
                2,
                2,
                1,
                now,
                now
        );
        var version = new ProcessVersionEntity(
                42,
                1,
                ProcessVersionStatus.Published,
                "Hundesteuer beantragen",
                null,
                now,
                now,
                now,
                null
        );
        var formLayout = new FormLayoutElement()
                .setPublicTitle("Hundesteuer online beantragen")
                .setShowOnFormIndexPage(false);
        var configuration = new AuthoredElementValues();
        configuration.put(FormTriggerConfigV1.FORM_SLUG, "antrag");
        configuration.put(FormTriggerConfigV1.FORM_LAYOUT, formLayout);
        var node = new ProcessNodeEntity(
                7,
                42,
                1,
                "Online-Antrag",
                null,
                "formInput",
                "form:form:1",
                1,
                configuration,
                Map.of(),
                null,
                null,
                null,
                false
        ).setUpdated(now);

        when(userService.fromJWT(null)).thenReturn(Optional.of(user));
        when(processService.listAllByAccessibleForUser(eq(org.springframework.data.domain.Pageable.unpaged()), eq("user-1"), eq(null)))
                .thenReturn(new PageImpl<>(List.of(process)));
        when(processNodeService.list(eq(pageable), any(ProcessNodeFilter.class)))
                .thenReturn(new PageImpl<>(List.of(node), pageable, 1));
        when(processService.retrieve(42)).thenReturn(Optional.of(process));
        when(processVersionService.retrieve(ProcessVersionEntityId.of(42, 1))).thenReturn(Optional.of(version));
        when(publicUrlService.createPublicFormUrl(process, "antrag"))
                .thenReturn("https://example.test/form/hundesteuer/antrag/");

        var result = controller.list(
                null,
                pageable,
                FormTriggerListControllerV1.FormOverviewMode.Published,
                "hundesteuer"
        );

        assertEquals(1, result.getTotalElements());
        var item = result.getContent().getFirst();
        assertEquals(7, item.id());
        assertEquals("Online-Antrag", item.nodeName());
        assertEquals("Hundesteuer online beantragen", item.formTitle());
        assertEquals("Hundesteuer", item.processTitle());
        assertEquals(ProcessVersionStatus.Published, item.status());
        assertEquals("https://example.test/form/hundesteuer/antrag/", item.publicUrl());
        assertEquals(now, item.published());
        assertEquals(false, item.showOnFormIndexPage());

        var filterCaptor = ArgumentCaptor.forClass(ProcessNodeFilter.class);
        verify(processNodeService).list(eq(pageable), filterCaptor.capture());
        assertEquals(List.of(42), filterCaptor.getValue().getProcessIds());
        assertEquals("form:form:1", filterCaptor.getValue().getProcessNodeDefinitionKey());
        assertEquals(1, filterCaptor.getValue().getProcessNodeDefinitionVersion());
    }

    @Test
    void listShouldReturnEmptyPageWithoutAccessibleProcesses() throws Exception {
        var pageable = PageRequest.of(0, 12);
        var user = new UserEntity().setId("user-1");
        when(userService.fromJWT(null)).thenReturn(Optional.of(user));
        when(processService.listAllByAccessibleForUser(eq(org.springframework.data.domain.Pageable.unpaged()), eq("user-1"), eq(null)))
                .thenReturn(new PageImpl<>(List.of()));

        var result = controller.list(
                null,
                pageable,
                FormTriggerListControllerV1.FormOverviewMode.Drafted,
                null
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void listShouldNotExposeAPublicUrlForDraftForms() throws Exception {
        var now = Instant.parse("2026-08-13T10:00:00Z");
        var pageable = PageRequest.of(0, 12);
        var user = new UserEntity().setId("user-1");
        var process = new ProcessEntity(
                42,
                "Hundesteuer",
                10,
                UUID.randomUUID(),
                "hundesteuer",
                2,
                2,
                1,
                now,
                now
        );
        var version = new ProcessVersionEntity(
                42,
                2,
                ProcessVersionStatus.Drafted,
                "Hundesteuer beantragen",
                null,
                now,
                now,
                null,
                null
        );
        var configuration = new AuthoredElementValues();
        configuration.put(FormTriggerConfigV1.FORM_SLUG, "antrag");
        var node = new ProcessNodeEntity(
                8,
                42,
                2,
                "Online-Antrag",
                null,
                "formInput",
                "form:form:1",
                1,
                configuration,
                Map.of(),
                null,
                null,
                null,
                false
        ).setUpdated(now);

        when(userService.fromJWT(null)).thenReturn(Optional.of(user));
        when(processService.listAllByAccessibleForUser(eq(org.springframework.data.domain.Pageable.unpaged()), eq("user-1"), eq(null)))
                .thenReturn(new PageImpl<>(List.of(process)));
        when(processNodeService.list(eq(pageable), any(ProcessNodeFilter.class)))
                .thenReturn(new PageImpl<>(List.of(node), pageable, 1));
        when(processService.retrieve(42)).thenReturn(Optional.of(process));
        when(processVersionService.retrieve(ProcessVersionEntityId.of(42, 2))).thenReturn(Optional.of(version));

        var result = controller.list(
                null,
                pageable,
                FormTriggerListControllerV1.FormOverviewMode.Drafted,
                null
        );

        var item = result.getContent().getFirst();
        assertEquals("Hundesteuer beantragen", item.formTitle());
        assertEquals(ProcessVersionStatus.Drafted, item.status());
        assertNull(item.publicUrl());
        assertNull(item.published());
        verifyNoInteractions(publicUrlService);
    }
}
