package de.aivot.GoverBackend.plugins.core.v1.nodes.triggers.webhook;

import de.aivot.GoverBackend.elements.models.ComputedElementStates;
import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.elements.models.EffectiveElementValues;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.permissions.services.PermissionService;
import de.aivot.GoverBackend.process.entities.ProcessEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.repositories.ProcessNodeRepository;
import de.aivot.GoverBackend.process.repositories.ProcessRepository;
import de.aivot.GoverBackend.process.repositories.ProcessTestClaimRepository;
import de.aivot.GoverBackend.process.services.ProcessInstanceAttachmentService;
import de.aivot.GoverBackend.process.services.ProcessInstanceService;
import de.aivot.GoverBackend.process.services.ProcessNodeService;
import de.aivot.GoverBackend.process.services.ProcessService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebhookTriggerControllerV1Test {
    private static final UUID ACCESS_KEY = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void jsonEndpointShouldRemainPublic() throws NoSuchMethodException {
        var mapping = WebhookTriggerControllerV1.class
                .getMethod(
                        "handleJson",
                        HttpServletRequest.class,
                        UUID.class,
                        String.class,
                        Map.class,
                        String.class,
                        String.class,
                        UUID.class,
                        String.class
                )
                .getAnnotation(RequestMapping.class);

        assertNotNull(mapping);
        assertArrayEquals(new String[]{"/api/public/webhooks/v1/{accessKey}/{slug}/json/"}, mapping.value());
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
                        UUID.class,
                        String.class,
                        String.class,
                        String.class,
                        UUID.class,
                        String.class
                )
                .getAnnotation(RequestMapping.class);

        assertNotNull(mapping);
        assertArrayEquals(new String[]{"/api/public/webhooks/v1/{accessKey}/{slug}/"}, mapping.value());
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
                        ACCESS_KEY,
                        "example-slug",
                        Map.<String, Object>of("key", "value"),
                        null,
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
                        ACCESS_KEY,
                        "example-slug",
                        Map.<String, Object>of("key", "value"),
                        null,
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
                ACCESS_KEY,
                "example-slug",
                Map.<String, Object>of("key", "value"),
                null,
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
        when(request.getFileMap()).thenReturn(Map.<String, MultipartFile>of());
        when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());

        var response = fixture.controller().handleFormData(
                request,
                ACCESS_KEY,
                "example-slug",
                null,
                null,
                null,
                null
        );

        assertEquals("Webhook empfangen und verarbeitet.", response.message());
        verify(fixture.processInstanceService()).create(any(ProcessInstanceEntity.class));
        verify(fixture.processInstanceService()).update(any(Long.class), any(ProcessInstanceEntity.class));
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
                ACCESS_KEY,
                "example-slug",
                null,
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
                .setAccessKey(ACCESS_KEY);

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

        var processInstanceService = mock(ProcessInstanceService.class);
        when(processInstanceService.create(any(ProcessInstanceEntity.class))).thenAnswer(invocation -> {
            var entity = invocation.getArgument(0, ProcessInstanceEntity.class);
            entity.setId(1L);
            return entity;
        });

        var processNodeService = mock(ProcessNodeService.class);
        when(processNodeService.deriveConfiguration(any(ProcessNodeEntity.class), anyBoolean()))
                .thenReturn(new DerivedRuntimeElementData(effectiveValues, new ComputedElementStates()));

        var processRepository = mock(ProcessRepository.class);
        when(processRepository.findOne(any(Specification.class))).thenReturn(Optional.of(process));
        var processService = new ProcessService(processRepository, mock(PermissionService.class));

        var processNodeRepository = mock(ProcessNodeRepository.class);
        when(processNodeRepository.findAll(any(Specification.class))).thenReturn(List.of(node));

        var controller = new WebhookTriggerControllerV1(
                processInstanceService,
                mock(ProcessTestClaimRepository.class),
                mock(ProcessInstanceAttachmentService.class),
                processNodeService,
                processService,
                processNodeRepository
        );

        return new TestControllerFixture(controller, processInstanceService);
    }

    private record TestControllerFixture(WebhookTriggerControllerV1 controller,
                                         ProcessInstanceService processInstanceService) {
    }
}
