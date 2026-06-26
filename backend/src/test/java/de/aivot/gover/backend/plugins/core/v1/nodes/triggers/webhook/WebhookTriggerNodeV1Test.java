package de.aivot.gover.backend.plugins.core.v1.nodes.triggers.webhook;

import de.aivot.gover.backend.elements.enums.OverrideFunctionType;
import de.aivot.gover.backend.elements.models.AuthoredElementValues;
import de.aivot.gover.backend.elements.models.ComputedElementStates;
import de.aivot.gover.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.gover.backend.elements.models.EffectiveElementValues;
import de.aivot.gover.backend.elements.models.elements.BaseElement;
import de.aivot.gover.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.gover.backend.javascript.services.JavascriptEngine;
import de.aivot.gover.backend.models.config.GoverConfig;
import de.aivot.gover.backend.plugins.core.v1.nodes.triggers.webhook.WebhookTriggerConfigV1;
import de.aivot.gover.backend.plugins.core.v1.nodes.triggers.webhook.WebhookTriggerNodeV1;
import de.aivot.gover.backend.process.entities.ProcessEntity;
import de.aivot.gover.backend.process.entities.ProcessNodeEntity;
import de.aivot.gover.backend.process.entities.ProcessVersionEntity;
import de.aivot.gover.backend.process.enums.ProcessVersionStatus;
import de.aivot.gover.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.gover.backend.process.repositories.ProcessNodeRepository;
import de.aivot.gover.backend.process.services.PublicUrlService;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class WebhookTriggerNodeV1Test {
    private static final Integer PROCESS_ID = 42;
    private static final Integer PROCESS_VERSION = 3;
    private static final Integer NODE_ID = 123;

    @Test
    void getConfigurationLayout_ShouldExposeCopyableSlugUrlTemplateAndDynamicOverride() throws Exception {
        var publicUrlService = new PublicUrlService(goverConfig());
        var node = new WebhookTriggerNodeV1(publicUrlService, mock(ProcessNodeRepository.class));

        var layout = node.getConfigurationLayout(configurationLayoutContext());
        var slugField = layout
                .findChild(WebhookTriggerConfigV1.SLUG_CONFIG_KEY, TextInputElement.class)
                .orElseThrow();

        var rootChildIds = layout
                .getChildren()
                .stream()
                .map(BaseElement::getId)
                .toList();
        assertTrue(rootChildIds.indexOf(WebhookTriggerConfigV1.REQUEST_BODY_CONFIG_GROUP_ID) < rootChildIds.indexOf(WebhookTriggerConfigV1.SLUG_CONFIG_KEY));

        assertEquals(true, slugField.getCopyable());
        assertEquals("https://example.test/api/public/webhook/antrag-prozess/{value}/", slugField.getCopyValueTemplate());
        assertNotNull(slugField.getOverride());
        assertEquals(OverrideFunctionType.Javascript, slugField.getOverride().getType());
        assertNotNull(slugField.getOverride().getJavascriptCode());
        assertTrue(slugField.getOverride().getJavascriptCode().getCode().contains("ctx.effectiveValues"));

        assertEquals(
                "https://example.test/api/public/webhook/antrag-prozess/{value}/json/",
                resolveCopyValueTemplate(
                        slugField,
                        WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_POST,
                        WebhookTriggerConfigV1.REQUEST_BODY_TYPE_OPTION_JSON
                )
        );
        assertEquals(
                "https://example.test/api/public/webhook/antrag-prozess/{value}/form-data/",
                resolveCopyValueTemplate(
                        slugField,
                        WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_POST,
                        WebhookTriggerConfigV1.REQUEST_BODY_TYPE_OPTION_FORM
                )
        );
        assertEquals(
                "https://example.test/api/public/webhook/antrag-prozess/{value}/",
                resolveCopyValueTemplate(
                        slugField,
                        WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_GET,
                        WebhookTriggerConfigV1.REQUEST_BODY_TYPE_OPTION_JSON
                )
        );
    }

    private static String resolveCopyValueTemplate(TextInputElement slugField,
                                                   String requestMethod,
                                                   String requestBodyType) throws Exception {
        var effectiveValues = new EffectiveElementValues();
        effectiveValues.put(WebhookTriggerConfigV1.REQUEST_METHOD_CONFIG_KEY, requestMethod);
        effectiveValues.put(WebhookTriggerConfigV1.REQUEST_BODY_TYPE_CONFIG_KEY, requestBodyType);

        try (var javascriptEngine = new JavascriptEngine()) {
            var result = javascriptEngine
                    .registerGlobalContextObject(new DerivedRuntimeElementData(effectiveValues, new ComputedElementStates()))
                    .registerElementObject(slugField)
                    .evaluateCode(slugField.getOverride().getJavascriptCode());

            return (String) result.asMap().get("copyValueTemplate");
        }
    }

    private static ProcessNodeDefinitionConfigurationLayoutContext configurationLayoutContext() {
        return new ProcessNodeDefinitionConfigurationLayoutContext(
                null,
                process(),
                processVersion(),
                processNode()
        );
    }

    private static ProcessEntity process() {
        return new ProcessEntity()
                .setId(PROCESS_ID)
                .setInternalTitle("Antrag")
                .setDepartmentId(1)
                .setAccessKey(UUID.randomUUID())
                .setSlug("antrag-prozess")
                .setVersionCount(PROCESS_VERSION)
                .setDraftedVersion(PROCESS_VERSION);
    }

    private static ProcessVersionEntity processVersion() {
        return new ProcessVersionEntity()
                .setProcessId(PROCESS_ID)
                .setProcessVersion(PROCESS_VERSION)
                .setStatus(ProcessVersionStatus.Drafted)
                .setPublicTitle("Antrag");
    }

    private static ProcessNodeEntity processNode() {
        return new ProcessNodeEntity()
                .setId(NODE_ID)
                .setProcessId(PROCESS_ID)
                .setProcessVersion(PROCESS_VERSION)
                .setName("Webhook")
                .setDataKey("webhookNode")
                .setProcessNodeDefinitionKey(WebhookTriggerNodeV1.NODE_KEY)
                .setProcessNodeDefinitionVersion(1)
                .setConfiguration(new AuthoredElementValues())
                .setOutputMappings(Map.of());
    }

    private static GoverConfig goverConfig() {
        var config = new GoverConfig();
        config.setGoverHostname("https://example.test/");
        return config;
    }
}
