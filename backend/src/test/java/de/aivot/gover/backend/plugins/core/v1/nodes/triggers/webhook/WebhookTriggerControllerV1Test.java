package de.aivot.gover.backend.plugins.core.v1.nodes.triggers.webhook;

import de.aivot.gover.backend.elements.models.ComputedElementStates;
import de.aivot.gover.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.gover.backend.elements.models.EffectiveElementValues;
import de.aivot.gover.backend.elements.models.elements.form.input.FileUploadInputElementItem;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.plugins.core.v1.nodes.triggers.webhook.WebhookTriggerConfigV1;
import de.aivot.gover.backend.plugins.core.v1.nodes.triggers.webhook.WebhookTriggerControllerV1;
import de.aivot.gover.backend.plugins.core.v1.nodes.triggers.webhook.WebhookTriggerNodeV1;
import de.aivot.gover.backend.process.entities.ProcessEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceAttachmentSetEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceEntity;
import de.aivot.gover.backend.process.entities.ProcessNodeEntity;
import de.aivot.gover.backend.process.models.ProcessNodeDefinition;
import de.aivot.gover.backend.process.repositories.ProcessNodeRepository;
import de.aivot.gover.backend.process.repositories.ProcessTestClaimRepository;
import de.aivot.gover.backend.process.services.ProcessInstanceAttachmentService;
import de.aivot.gover.backend.process.services.ProcessInstanceAttachmentSetService;
import de.aivot.gover.backend.process.services.ProcessInstanceService;
import de.aivot.gover.backend.process.services.ProcessNodeDefinitionService;
import de.aivot.gover.backend.process.services.ProcessNodeService;
import de.aivot.gover.backend.process.services.ProcessService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebhookTriggerControllerV1Test {
    private static final String PROCESS_SLUG = "example-process";

    @Test
    void jsonEndpointShouldRemainPublic() throws NoSuchMethodException {
        var mapping = WebhookTriggerControllerV1.class
                .getMethod(
                        "handleJson",
                        HttpServletRequest.class,
                        String.class,
                        String.class,
                        Map.class,
                        String.class,
                        String.class,
                        String.class
                )
                .getAnnotation(RequestMapping.class);

        assertNotNull(mapping);
        assertArrayEquals(new String[]{"/api/public/webhook/{processSlug}/{slug}/json/"}, mapping.value());
        assertArrayEquals(new RequestMethod[]{RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH}, mapping.method());
        assertArrayEquals(new String[]{MediaType.APPLICATION_JSON_VALUE}, mapping.consumes());
        assertArrayEquals(new String[]{MediaType.APPLICATION_JSON_VALUE}, mapping.produces());
    }

    @Test
    void bodylessEndpointShouldRemainPublic() throws NoSuchMethodException {
        var mapping = WebhookTriggerControllerV1.class
                .getMethod(
                        "handleWithoutBody",
                        HttpServletRequest.class,
                        String.class,
                        String.class,
                        String.class,
                        String.class,
                        String.class
                )
                .getAnnotation(RequestMapping.class);

        assertNotNull(mapping);
        assertArrayEquals(new String[]{"/api/public/webhook/{processSlug}/{slug}/"}, mapping.value());
        assertArrayEquals(new RequestMethod[]{RequestMethod.GET, RequestMethod.DELETE}, mapping.method());
        assertArrayEquals(new String[]{MediaType.APPLICATION_JSON_VALUE}, mapping.produces());
    }

    @Test
    void handleJsonShouldRejectUnsupportedContentType() throws ResponseException {
        var fixture = createControllerFixture(
                WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_POST,
                WebhookTriggerConfigV1.REQUEST_BODY_TYPE_OPTION_JSON
        );
        var request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_POST);
        when(request.getContentType()).thenReturn(MediaType.TEXT_PLAIN_VALUE);

        var exception = assertThrows(
                ResponseException.class,
                () -> fixture.controller().handleJson(
                        request,
                        PROCESS_SLUG,
                        "example-slug",
                        Map.<String, Object>of("key", "value"),
                        null,
                        null,
                        null
                )
        );

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, exception.getStatus());
        verify(fixture.processInstanceService(), never()).create(any(ProcessInstanceEntity.class));
    }

    @Test
    void handleXmlShouldRejectUnsupportedContentType() throws ResponseException {
        var fixture = createControllerFixture(
                WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_POST,
                WebhookTriggerConfigV1.REQUEST_BODY_TYPE_OPTION_XML
        );
        var request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_POST);
        when(request.getContentType()).thenReturn(MediaType.APPLICATION_JSON_VALUE);

        var exception = assertThrows(
                ResponseException.class,
                () -> fixture.controller().handleXml(
                        request,
                        PROCESS_SLUG,
                        "example-slug",
                        Map.<String, Object>of("key", "value"),
                        null,
                        null,
                        null
                )
        );

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, exception.getStatus());
        verify(fixture.processInstanceService(), never()).create(any(ProcessInstanceEntity.class));
    }

    @Test
    void handleJsonShouldAcceptMatchingJsonContentType() throws ResponseException {
        var fixture = createControllerFixture(
                WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_POST,
                WebhookTriggerConfigV1.REQUEST_BODY_TYPE_OPTION_JSON
        );
        var request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_POST);
        when(request.getContentType()).thenReturn(MediaType.APPLICATION_JSON_VALUE);
        when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(request.getParameterMap()).thenReturn(Map.of());

        var response = fixture.controller().handleJson(
                request,
                PROCESS_SLUG,
                "example-slug",
                Map.<String, Object>of("key", "value"),
                null,
                null,
                null
        );

        assertEquals("Webhook empfangen und verarbeitet.", response.message());
        verify(fixture.processInstanceService()).create(any(ProcessInstanceEntity.class));
        verify(fixture.processInstanceService()).update(any(Long.class), any(ProcessInstanceEntity.class));
    }

    @Test
    void handleFormDataShouldAcceptMultipartPayload() throws ResponseException {
        var fixture = createControllerFixture(
                WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_POST,
                WebhookTriggerConfigV1.REQUEST_BODY_TYPE_OPTION_FORM
        );
        var request = mock(StandardMultipartHttpServletRequest.class);
        when(request.getMethod()).thenReturn(WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_POST);
        when(request.getContentType()).thenReturn(MediaType.MULTIPART_FORM_DATA_VALUE);
        when(request.getParameterMap()).thenReturn(Map.of("field", new String[]{"value"}));
        when(request.getMultiFileMap()).thenReturn(new LinkedMultiValueMap<>());
        when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());

        var response = fixture.controller().handleFormData(
                request,
                PROCESS_SLUG,
                "example-slug",
                null,
                null,
                null
        );

        assertEquals("Webhook empfangen und verarbeitet.", response.message());
        verify(fixture.processInstanceService()).create(any(ProcessInstanceEntity.class));
        verify(fixture.processInstanceService()).update(any(Long.class), any(ProcessInstanceEntity.class));
    }

    @Test
    void handleFormDataShouldGroupUploadedFilesByMultipartFieldName() throws ResponseException {
        var fixture = createControllerFixture(
                WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_POST,
                WebhookTriggerConfigV1.REQUEST_BODY_TYPE_OPTION_FORM
        );
        var request = mock(StandardMultipartHttpServletRequest.class);
        when(request.getMethod()).thenReturn(WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_POST);
        when(request.getContentType()).thenReturn(MediaType.MULTIPART_FORM_DATA_VALUE);
        when(request.getParameterMap()).thenReturn(Map.of());
        when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());

        var files = new LinkedMultiValueMap<String, MultipartFile>();
        files.add("case.documents", new MockMultipartFile("case.documents", "first.pdf", "application/pdf", "1".getBytes(StandardCharsets.UTF_8)));
        files.add("case.documents", new MockMultipartFile("case.documents", "second.pdf", "application/pdf", "2".getBytes(StandardCharsets.UTF_8)));
        files.add("other", new MockMultipartFile("other", "other.pdf", "application/pdf", "3".getBytes(StandardCharsets.UTF_8)));
        when(request.getMultiFileMap()).thenReturn(files);

        var response = fixture.controller().handleFormData(
                request,
                PROCESS_SLUG,
                "example-slug",
                null,
                null,
                null
        );

        assertEquals("Webhook empfangen und verarbeitet.", response.message());
        assertEquals(2, fixture.createdAttachmentSets().size());
        assertEquals("case_documents", fixture.createdAttachmentSets().get(0).getDataKey());
        assertEquals("other", fixture.createdAttachmentSets().get(1).getDataKey());
        assertEquals(fixture.createdAttachmentSets().get(0).getId(), fixture.createdAttachments().get(0).getAttachmentSetId());
        assertEquals(fixture.createdAttachmentSets().get(0).getId(), fixture.createdAttachments().get(1).getAttachmentSetId());
        assertEquals(fixture.createdAttachmentSets().get(1).getId(), fixture.createdAttachments().get(2).getAttachmentSetId());

        var updatedInstanceCaptor = ArgumentCaptor.forClass(ProcessInstanceEntity.class);
        verify(fixture.processInstanceService()).update(eq(1L), updatedInstanceCaptor.capture());
        @SuppressWarnings("unchecked")
        var fileItems = (List<FileUploadInputElementItem>) updatedInstanceCaptor
                .getValue()
                .getInitialPayload()
                .get(WebhookTriggerNodeV1.INITIAL_DATA_KEY_FILES);
        assertEquals(3, fileItems.size());
        assertEquals("first.pdf", fileItems.getFirst().getName());
        assertEquals(1, fileItems.getFirst().getSize());
        assertTrue(fileItems.getFirst().getUri().startsWith("process-instance-attachment:"));
    }

    @Test
    void handleWithoutBodyShouldAllowEmptyGetRequestsWithoutContentType() throws ResponseException {
        var fixture = createControllerFixture(
                WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_GET,
                null
        );
        var request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_GET);
        when(request.getContentType()).thenReturn(null);
        when(request.getContentLengthLong()).thenReturn(0L);
        when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(request.getHeader("Transfer-Encoding")).thenReturn(null);
        when(request.getParameterMap()).thenReturn(Map.of());

        var response = fixture.controller().handleWithoutBody(
                request,
                PROCESS_SLUG,
                "example-slug",
                null,
                null,
                null
        );

        assertEquals("Webhook empfangen und verarbeitet.", response.message());
        verify(fixture.processInstanceService()).create(any(ProcessInstanceEntity.class));
        verify(fixture.processInstanceService()).update(any(Long.class), any(ProcessInstanceEntity.class));
    }

    private static TestControllerFixture createControllerFixture(String requestMethod,
                                                                 String requestBodyType) throws ResponseException {
        var process = new ProcessEntity()
                .setId(1)
                .setSlug(PROCESS_SLUG);

        var node = new ProcessNodeEntity()
                .setId(1)
                .setProcessId(1)
                .setProcessVersion(1)
                .setDataKey("webhook")
                .setProcessNodeDefinitionKey(WebhookTriggerNodeV1.NODE_KEY)
                .setProcessNodeDefinitionVersion(1);

        var effectiveValues = new EffectiveElementValues();
        effectiveValues.put(WebhookTriggerConfigV1.SLUG_CONFIG_KEY, "example-slug");
        effectiveValues.put(WebhookTriggerConfigV1.REQUEST_METHOD_CONFIG_KEY, requestMethod);

        if (requestBodyType != null) {
            effectiveValues.put(WebhookTriggerConfigV1.REQUEST_BODY_TYPE_CONFIG_KEY, requestBodyType);
        }

        var webhookConfig = new WebhookTriggerConfigV1();
        webhookConfig.slug = "example-slug";
        webhookConfig.requestMethod = requestMethod;
        if (requestBodyType != null) {
            webhookConfig.requestBodyConfig = new WebhookTriggerConfigV1.WebhookRequestBodyConfig();
            webhookConfig.requestBodyConfig.requestBodyType = requestBodyType;
        }

        var processInstanceService = mock(ProcessInstanceService.class);
        when(processInstanceService.create(any(ProcessInstanceEntity.class))).thenAnswer(invocation -> {
            var entity = invocation.getArgument(0, ProcessInstanceEntity.class);
            entity.setId(1L);
            return entity;
        });

        ProcessNodeDefinition<WebhookTriggerConfigV1> processNodeDefinition = mock(ProcessNodeDefinition.class);
        var processNodeService = mock(ProcessNodeService.class);
        when(processNodeService.deriveConfiguration(any(ProcessNodeEntity.class), eq(processNodeDefinition), isNull(), eq(true)))
                .thenReturn(new ProcessNodeService.ProcessConfigurationDetails<>(
                        webhookConfig,
                        new DerivedRuntimeElementData(effectiveValues, new ComputedElementStates())
                ));

        var processNodeDefinitionService = mock(ProcessNodeDefinitionService.class);
        when(processNodeDefinitionService.getProcessNodeDefinition(any(ProcessNodeEntity.class)))
                .thenReturn(Optional.of(processNodeDefinition));

        var processService = mock(ProcessService.class);
        when(processService.retrieveBySlugOrHistory(PROCESS_SLUG)).thenReturn(Optional.of(process));

        var processNodeRepository = mock(ProcessNodeRepository.class);
        when(processNodeRepository.findAll(any(Specification.class))).thenReturn(List.of(node));

        var createdAttachments = new ArrayList<ProcessInstanceAttachmentEntity>();
        var processInstanceAttachmentService = mock(ProcessInstanceAttachmentService.class);
        when(processInstanceAttachmentService.create(any(ProcessInstanceAttachmentEntity.class))).thenAnswer(invocation -> {
            var attachment = invocation.getArgument(0, ProcessInstanceAttachmentEntity.class);
            attachment
                    .setKey(UUID.nameUUIDFromBytes(("attachment-" + createdAttachments.size()).getBytes(StandardCharsets.UTF_8)))
                    .setStorageProviderId(1)
                    .setStoragePathFromRoot("/attachments/" + attachment.getFileName());
            createdAttachments.add(attachment);
            return attachment;
        });

        var createdAttachmentSets = new ArrayList<ProcessInstanceAttachmentSetEntity>();
        var processInstanceAttachmentSetService = mock(ProcessInstanceAttachmentSetService.class);
        when(processInstanceAttachmentSetService.create(any(ProcessInstanceAttachmentSetEntity.class))).thenAnswer(invocation -> {
            var attachmentSet = invocation.getArgument(0, ProcessInstanceAttachmentSetEntity.class);
            attachmentSet.setId(createdAttachmentSets.size() + 1);
            createdAttachmentSets.add(attachmentSet);
            return attachmentSet;
        });

        var controller = new WebhookTriggerControllerV1(
                processInstanceService,
                mock(ProcessTestClaimRepository.class),
                processInstanceAttachmentService,
                processInstanceAttachmentSetService,
                processNodeService,
                processService,
                processNodeRepository,
                processNodeDefinitionService
        );

        return new TestControllerFixture(controller, processInstanceService, createdAttachments, createdAttachmentSets);
    }

    private record TestControllerFixture(WebhookTriggerControllerV1 controller,
                                         ProcessInstanceService processInstanceService,
                                         List<ProcessInstanceAttachmentEntity> createdAttachments,
                                         List<ProcessInstanceAttachmentSetEntity> createdAttachmentSets) {
    }
}
