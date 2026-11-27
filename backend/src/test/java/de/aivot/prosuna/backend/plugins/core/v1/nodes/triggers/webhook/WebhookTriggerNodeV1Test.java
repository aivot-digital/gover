package de.aivot.prosuna.backend.plugins.core.v1.nodes.triggers.webhook;

import de.aivot.prosuna.backend.elements.enums.OverrideFunctionType;
import de.aivot.prosuna.backend.elements.enums.ValidationFunctionType;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.ComputedElementStates;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.EffectiveElementValues;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.javascript.services.JavascriptEngine;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.nocode.models.NoCodeExpression;
import de.aivot.prosuna.backend.nocode.models.NoCodeReference;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import de.aivot.prosuna.backend.plugins.core.v1.operators.text.NoCodeRegexMatchOperator;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessTestClaimEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.enums.ProcessVersionStatus;
import de.aivot.prosuna.backend.process.enums.ProcessNodeConfigurationValidationPhase;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeConfigurationValidationContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionTestingLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.repositories.ProcessNodeRepository;
import de.aivot.prosuna.backend.process.services.PublicUrlService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.any;
import org.springframework.data.jpa.domain.Specification;

class WebhookTriggerNodeV1Test {
    private static final Integer PROCESS_ID = 42;
    private static final Integer PROCESS_VERSION = 3;
    private static final Integer NODE_ID = 123;

    @Test
    void validateConfiguration_ShouldCheckSlugSyntaxAndUniquenessDuringAuthoring() throws Exception {
        var repository = mock(ProcessNodeRepository.class);
        when(repository.exists(any(Specification.class))).thenReturn(true);
        var node = new WebhookTriggerNodeV1(new PublicUrlService(prosunaConfig()), repository);
        var configuration = new WebhookTriggerConfigV1();
        configuration.slug = "Invalid Slug";

        var errors = node.validateConfiguration(new ProcessNodeConfigurationValidationContext<>(
                processNode(), configuration, DerivedRuntimeElementData.empty(), ProcessNodeConfigurationValidationPhase.Authoring
        ));

        assertNotNull(errors);
        assertEquals(List.of(
                "Das URL-Segment darf nur Kleinbuchstaben von a bis z, Zahlen und Bindestriche enthalten.",
                "Das URL-Segment wird in dieser Prozessversion bereits von einem anderen Webhook verwendet. Wählen Sie ein anderes Segment."
        ), errors.get(WebhookTriggerConfigV1.SLUG_CONFIG_KEY));
        verify(repository).exists(any(Specification.class));
    }

    @Test
    void validateConfiguration_ShouldSkipDefinitionChecksAtRuntime() throws Exception {
        var repository = mock(ProcessNodeRepository.class);
        var node = new WebhookTriggerNodeV1(new PublicUrlService(prosunaConfig()), repository);
        var configuration = new WebhookTriggerConfigV1();
        configuration.slug = "Invalid Slug";

        var errors = node.validateConfiguration(new ProcessNodeConfigurationValidationContext<>(
                processNode(), configuration, DerivedRuntimeElementData.empty(), ProcessNodeConfigurationValidationPhase.Runtime
        ));

        assertNull(errors);
        verifyNoInteractions(repository);
    }

    @Test
    void getConfigurationLayout_ShouldExposeCopyableSlugUrlTemplateAndDynamicOverride() throws Exception {
        var publicUrlService = new PublicUrlService(prosunaConfig());
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
        var slugValidation = slugField.getValidation();
        assertNotNull(slugValidation);
        assertEquals(ValidationFunctionType.NoCode, slugValidation.getType());
        assertEquals(List.of(WebhookTriggerConfigV1.SLUG_CONFIG_KEY), List.copyOf(slugValidation.getReferencedIds()));
        assertEquals("Das URL-Segment darf nur Kleinbuchstaben von a bis z, Zahlen und Bindestriche enthalten.",
                slugValidation.getNoCodeList().getFirst().getMessage());
        var slugExpression = assertInstanceOf(
                NoCodeExpression.class,
                slugValidation.getNoCodeList().getFirst().getNoCode()
        );
        assertEquals(NoCodeRegexMatchOperator.OPERATOR_ID, slugExpression.getOperatorIdentifier());
        assertEquals(
                List.of(
                        NoCodeReference.of(WebhookTriggerConfigV1.SLUG_CONFIG_KEY),
                        NoCodeStaticValue.of("^[a-z0-9-]+$")
                ),
                List.copyOf(slugExpression.getOperands())
        );
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

    @Test
    void outputsNameTheReceivedDataAndReusableFileReferences() {
        var node = new WebhookTriggerNodeV1(new PublicUrlService(prosunaConfig()), mock(ProcessNodeRepository.class));
        var outputs = node.getOutputs().stream().collect(java.util.stream.Collectors.toMap(output -> output.key(), output -> output.label()));

        assertEquals("Empfangene Daten", outputs.get(WebhookTriggerNodeV1.INITIAL_DATA_KEY_PAYLOAD));
        assertEquals("Anlagen", outputs.get(WebhookTriggerNodeV1.INITIAL_DATA_KEY_ATTACHMENTS));
        assertEquals("Dateiverweise", outputs.get(WebhookTriggerNodeV1.INITIAL_DATA_KEY_FILES));
        assertEquals("Eingegangen am", outputs.get(WebhookTriggerNodeV1.INITIAL_DATA_KEY_STARTED));
    }

    @ParameterizedTest
    @CsvSource({"GET,,", "POST,json,basic", "PUT,form,query_param", "PATCH,xml,"})
    void testingHelpKeepsTheConfiguredCallAndDistinguishesTestClaimFromAuthentication(
            String method, String bodyType, String authentication) throws Exception {
        var node = new WebhookTriggerNodeV1(new PublicUrlService(prosunaConfig()), mock(ProcessNodeRepository.class));
        var config = new WebhookTriggerConfigV1();
        config.requestMethod = method;
        config.slug = "eingang";
        if (bodyType != null) {
            config.requestBodyConfig = new WebhookTriggerConfigV1.WebhookRequestBodyConfig();
            config.requestBodyConfig.requestBodyType = bodyType;
        }
        if (authentication != null) {
            config.authRequired = true;
            config.authConfig = new WebhookTriggerConfigV1.WebhookConfigAuth();
            config.authConfig.authMethod = authentication;
            config.authConfig.authUsername = "user";
            config.authConfig.authPassword = "password";
            config.authConfig.authToken = "hook-token";
        }
        var claim = new ProcessTestClaimEntity().setAccessKey("test-session");
        var layout = node.getTestingLayout(new ProcessNodeDefinitionTestingLayoutContext<>(
                new UserEntity().setId("staff"), process(), processVersion(), processNode(), claim, config));
        var content = layout.findChild("text", RichTextContentElement.class).orElseThrow().getContent();
        var suffix = bodyType == null ? "" : bodyType.equals("form") ? "form-data/" : bodyType + "/";
        var expectedUrl = "https://example.test/api/public/webhook/antrag-prozess/eingang/" + suffix
                + "?test-claim=test-session" + ("query_param".equals(authentication) ? "&token=hook-token" : "");

        assertTrue(content.contains("Mit diesem Aufruf können Sie einen Testvorgang starten."), content);
        assertTrue(content.contains("Er ordnet den Aufruf Ihrer Testsitzung zu."), content);
        assertTrue(content.contains("Eine für den Webhook eingerichtete Authentifizierung ist zusätzlich erforderlich."), content);
        assertTrue(content.contains("Beispielaufruf mit curl:"), content);
        assertTrue(content.contains("--request " + method), content);
        assertTrue(content.contains("--url '" + expectedUrl + "'"), content);
        assertFalse(content.contains("damit der Request authentifiziert ist"), content);
        assertFalse(content.contains("mit einem leeren Payload gestartet"), content);

        if (bodyType == null) {
            assertFalse(content.contains("--header 'Content-Type:"), content);
            assertFalse(content.contains("--data "), content);
            assertFalse(content.contains("--form "), content);
        } else if (bodyType.equals("json")) {
            assertTrue(content.contains("application/json"), content);
            assertTrue(content.contains("Für JSON ist ein Objekt erforderlich"), content);
            assertTrue(content.contains("--data '{\"key\": \"value\"}'"), content);
        } else if (bodyType.equals("form")) {
            assertTrue(content.contains("multipart/form-data"), content);
            assertTrue(content.contains("--form 'key=value'"), content);
            assertTrue(content.contains("--form 'file=@/path/to/file'"), content);
        } else {
            assertTrue(content.contains("application/xml"), content);
            assertTrue(content.contains("--data '<root><key>value</key></root>'"), content);
        }
        if (!"json".equals(bodyType)) {
            assertFalse(content.contains("Für JSON ist ein Objekt erforderlich"), content);
        }
        if ("basic".equals(authentication)) {
            assertTrue(content.contains("Authorization: Basic dXNlcjpwYXNzd29yZA=="), content);
            assertTrue(content.contains("Das folgende Beispiel enthält den passenden Authorization-Header."), content);
        } else if ("query_param".equals(authentication)) {
            assertTrue(content.contains("Die Testadresse enthält bereits den für diesen Webhook eingerichteten Parameter `token`."), content);
        }
    }

    @Test
    void invalidPayloadHasAReadableErrorAndRetainsItsCastCause() {
        var node = new WebhookTriggerNodeV1(new PublicUrlService(prosunaConfig()), mock(ProcessNodeRepository.class));
        var config = new WebhookTriggerConfigV1();
        config.requestBodyConfig = new WebhookTriggerConfigV1.WebhookRequestBodyConfig();
        config.requestBodyConfig.copyToProcessData = true;
        var context = new ProcessNodeExecutionInitContext<>(null, processNode(),
                new ProcessInstanceEntity().setInitialPayload(Map.of(WebhookTriggerNodeV1.INITIAL_DATA_KEY_PAYLOAD, List.of("value"))),
                new ProcessInstanceTaskEntity(), null, new ProcessExecutionData(), config);

        var error = assertThrows(ProcessNodeExecutionExceptionUnknown.class, () -> node.init(context));

        assertEquals("Die empfangenen Webhook-Daten können nicht in die Vorgangsdaten übernommen werden. Erwartet wird ein Objekt mit Feldnamen und Werten.",
                error.getMessage());
        assertInstanceOf(ClassCastException.class, error.getCause());
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

    private static ProsunaConfig prosunaConfig() {
        var config = new ProsunaConfig();
        config.setProsunaHostname("https://example.test/");
        return config;
    }
}
