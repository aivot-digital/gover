package de.aivot.GoverBackend.plugins.core.v1.nodes.triggers.webhook;

import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import de.aivot.GoverBackend.elements.models.ComputedElementStates;
import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.elements.models.EffectiveElementValues;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.repositories.ProcessTestClaimRepository;
import de.aivot.GoverBackend.process.services.ProcessInstanceAttachmentService;
import de.aivot.GoverBackend.process.services.ProcessInstanceService;
import de.aivot.GoverBackend.process.services.ProcessNodeService;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebhookTriggerControllerV1Test {
    @Test
    void optionsEndpointShouldRemainPublic() throws NoSuchMethodException {
        var mapping = WebhookTriggerControllerV1.class
                .getMethod("handleOptions", String.class, String.class)
                .getAnnotation(RequestMapping.class);

        assertNotNull(mapping);
        assertArrayEquals(new String[]{"/api/public/webhooks/v1/{slug}/"}, mapping.value());
        assertArrayEquals(new RequestMethod[]{RequestMethod.OPTIONS}, mapping.method());
    }

    @Test
    void handleOptionsShouldReturnWebhookConnectionInformation() throws ResponseException {
        var controller = createController(
                WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_POST,
                WebhookTriggerConfigV1.REQUEST_BODY_TYPE_OPTION_JSON
        );

        var response = controller.handleOptions("example-slug", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("OPTIONS, POST", response.getHeaders().getFirst(HttpHeaders.ALLOW));

        var body = response.getBody();
        assertNotNull(body);
        assertEquals(RequestMethod.OPTIONS.name(), body.method());
        assertEquals(List.of(RequestMethod.OPTIONS.name(), WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_POST), body.allow());
        assertEquals(WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_POST, body.allowedMethod());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, body.acceptedContentType());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, body.returnedContentType());
    }

    @Test
    void handleOptionsShouldReturnNullAcceptedContentTypeForBodylessWebhook() throws ResponseException {
        var controller = createController(
                WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_GET,
                null
        );

        var response = controller.handleOptions("example-slug", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("OPTIONS, GET", response.getHeaders().getFirst(HttpHeaders.ALLOW));

        var body = response.getBody();
        assertNotNull(body);
        assertEquals(List.of(RequestMethod.OPTIONS.name(), WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_GET), body.allow());
        assertEquals(WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_GET, body.allowedMethod());
        assertNull(body.acceptedContentType());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, body.returnedContentType());
    }

    @Test
    void handleFormDataShouldRejectMultipartWhenJsonIsConfigured() throws ResponseException {
        var fixture = createControllerFixture(
                WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_POST,
                WebhookTriggerConfigV1.REQUEST_BODY_TYPE_OPTION_JSON
        );
        var request = mock(org.springframework.web.multipart.support.StandardMultipartHttpServletRequest.class);
        when(request.getMethod()).thenReturn(WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_POST);
        when(request.getContentType()).thenReturn(MediaType.MULTIPART_FORM_DATA_VALUE);
        when(request.getParameterMap()).thenReturn(java.util.Map.of());
        when(request.getFileMap()).thenReturn(java.util.Map.of());

        var exception = assertThrows(
                ResponseException.class,
                () -> fixture.controller().handleFormData(request, "example-slug", null, null, null, null)
        );

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, exception.getStatus());
        verify(fixture.processInstanceService(), never()).create(any());
    }

    @Test
    void handleXmlAndJsonShouldRejectJsonWhenXmlIsConfigured() throws ResponseException {
        var fixture = createControllerFixture(
                WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_POST,
                WebhookTriggerConfigV1.REQUEST_BODY_TYPE_OPTION_XML
        );
        var request = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(request.getMethod()).thenReturn(WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_POST);
        when(request.getContentType()).thenReturn(MediaType.APPLICATION_JSON_VALUE);

        var exception = assertThrows(
                ResponseException.class,
                () -> fixture.controller().handleXmlAndJson(request, "example-slug", java.util.Map.of("key", "value"), null, null, null, null)
        );

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, exception.getStatus());
        verify(fixture.processInstanceService(), never()).create(any());
    }

    @Test
    void handleWithoutBodyShouldRejectUnsupportedContentTypeWhenBodyIsPresent() throws ResponseException {
        var fixture = createControllerFixture(
                WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_POST,
                WebhookTriggerConfigV1.REQUEST_BODY_TYPE_OPTION_JSON
        );
        var request = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(request.getMethod()).thenReturn(WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_POST);
        when(request.getContentType()).thenReturn(MediaType.TEXT_PLAIN_VALUE);
        when(request.getContentLengthLong()).thenReturn(4L);

        var exception = assertThrows(
                ResponseException.class,
                () -> fixture.controller().handleWithoutBody(request, "example-slug", null, null, null, null)
        );

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, exception.getStatus());
        verify(fixture.processInstanceService(), never()).create(any());
    }

    @Test
    void handleXmlAndJsonShouldAcceptMatchingJsonContentType() throws ResponseException {
        var fixture = createControllerFixture(
                WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_POST,
                WebhookTriggerConfigV1.REQUEST_BODY_TYPE_OPTION_JSON
        );
        var request = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(request.getMethod()).thenReturn(WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_POST);
        when(request.getContentType()).thenReturn(MediaType.APPLICATION_JSON_VALUE);
        when(request.getHeaderNames()).thenReturn(java.util.Collections.emptyEnumeration());
        when(request.getParameterMap()).thenReturn(java.util.Map.of());

        var response = fixture.controller().handleXmlAndJson(
                request,
                "example-slug",
                java.util.Map.of("key", "value"),
                null,
                null,
                null,
                null
        );

        assertEquals("Webhook empfangen und verarbeitet.", response.message());
        verify(fixture.processInstanceService()).create(any(ProcessInstanceEntity.class));
    }

    @Test
    void handleWithoutBodyShouldAllowEmptyRequestsWithoutContentType() throws ResponseException {
        var fixture = createControllerFixture(
                WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_POST,
                WebhookTriggerConfigV1.REQUEST_BODY_TYPE_OPTION_JSON
        );
        var request = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(request.getMethod()).thenReturn(WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_POST);
        when(request.getContentType()).thenReturn(null);
        when(request.getContentLengthLong()).thenReturn(0L);
        when(request.getHeader(HttpHeaders.TRANSFER_ENCODING)).thenReturn(null);
        when(request.getHeaderNames()).thenReturn(java.util.Collections.emptyEnumeration());
        when(request.getParameterMap()).thenReturn(java.util.Map.of());

        var response = fixture.controller().handleWithoutBody(request, "example-slug", null, null, null, null);

        assertEquals("Webhook empfangen und verarbeitet.", response.message());
        verify(fixture.processInstanceService()).create(any(ProcessInstanceEntity.class));
    }

    private static WebhookTriggerControllerV1 createController(String requestMethod,
                                                               String requestBodyType) throws ResponseException {
        return createControllerFixture(requestMethod, requestBodyType).controller();
    }

    private static TestControllerFixture createControllerFixture(String requestMethod,
                                                                 String requestBodyType) throws ResponseException {
        var node = new ProcessNodeEntity()
                .setId(1)
                .setProcessId(1)
                .setProcessVersion(1)
                .setDataKey("webhook")
                .setProcessNodeDefinitionKey(WebhookTriggerNodeV1.NODE_KEY)
                .setProcessNodeDefinitionVersion(1)
                .setConfiguration(new AuthoredElementValues())
                .setOutputMappings(java.util.Map.of());

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

        var controller = new WebhookTriggerControllerV1(
                processInstanceService,
                mock(ProcessTestClaimRepository.class),
                mock(ProcessInstanceAttachmentService.class),
                new TestProcessNodeService(node, effectiveValues)
        );

        return new TestControllerFixture(controller, processInstanceService);
    }

    private record TestControllerFixture(WebhookTriggerControllerV1 controller,
                                         ProcessInstanceService processInstanceService) {
    }

    private static final class TestProcessNodeService extends ProcessNodeService {
        private final ProcessNodeEntity node;
        private final DerivedRuntimeElementData derivedConfiguration;

        private TestProcessNodeService(ProcessNodeEntity node,
                                       EffectiveElementValues effectiveValues) {
            super(null, null, null, null, null, null, null);
            this.node = node;
            this.derivedConfiguration = new DerivedRuntimeElementData(effectiveValues, new ComputedElementStates());
        }

        @Override
        public Optional<ProcessNodeEntity> retrieve(Specification<ProcessNodeEntity> specification) {
            return Optional.of(node);
        }

        @Override
        public DerivedRuntimeElementData deriveConfiguration(ProcessNodeEntity entity,
                                                             boolean skipErrors) {
            return derivedConfiguration;
        }
    }
}
