package de.aivot.GoverBackend.plugins.core.v1.nodes.triggers.webhook;

import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import de.aivot.GoverBackend.elements.models.ComputedElementStates;
import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.elements.models.EffectiveElementValues;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
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
import static org.mockito.Mockito.mock;

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

    private static WebhookTriggerControllerV1 createController(String requestMethod,
                                                               String requestBodyType) {
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

        return new WebhookTriggerControllerV1(
                mock(ProcessInstanceService.class),
                mock(ProcessTestClaimRepository.class),
                mock(ProcessInstanceAttachmentService.class),
                new TestProcessNodeService(node, effectiveValues)
        );
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
