package de.aivot.prosuna.backend.plugins.core.v1.nodes.triggers.fitconnect;

import de.aivot.prosuna.backend.elements.enums.AssetVisibility;
import de.aivot.prosuna.backend.elements.enums.ValidationFunctionType;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.AssetSelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.CheckboxInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SecretSelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.nocode.models.NoCodeExpression;
import de.aivot.prosuna.backend.nocode.models.NoCodeReference;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import de.aivot.prosuna.backend.plugins.core.v1.operators.text.NoCodeRegexMatchOperator;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessTestClaimEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidDataType;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionTestingLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.repositories.ProcessNodeRepository;
import de.aivot.prosuna.backend.process.services.PublicUrlService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FitConnectTriggerNodeV1Test {
    private static final String REMOVED_DESTINATION_TYPE_CONFIG_KEY = "destination_type";

    @Test
    void createsConfigurationLayoutForAllConfigFields() throws Exception {
        var publicUrlService = mock(PublicUrlService.class);
        when(publicUrlService.createPublicApiUrl(anyString(), anyString(), anyString(), any(Object[].class)))
                .thenReturn("https://prosuna.example/api/public/fit-connect/example/__copy_value__/");
        var node = new FitConnectTriggerNodeV1(
                publicUrlService,
                mock(ProcessNodeRepository.class),
                mock(FitConnectTriggerOrganisationFactoryV1.class)
        );
        var process = new ProcessEntity().setSlug("example");

        var layout = node.getConfigurationLayout(new ProcessNodeDefinitionConfigurationLayoutContext(
                null,
                process,
                mock(ProcessVersionEntity.class),
                mock(ProcessNodeEntity.class)
        ));

        var slugField = layout
                .findChild(FitConnectTriggerConfigV1.SLUG_CONFIG_KEY, TextInputElement.class)
                .orElseThrow();
        assertEquals(
                List.of("slug", "callback_secret", "environment", "destination_id", "subscriber_client_id",
                        "subscriber_client_secret", "private_signing_key", "private_decryption_keys", "copy_to_process_data"),
                layout.getChildren().stream().map(BaseElement::getId).toList()
        );
        assertEquals("Callback-URL (URL-Segment)", slugField.getLabel());
        assertTrue(slugField.getHint().contains("vollständige Adresse"));
        assertTrue(slugField.getHint().contains("FIT-Connect-Zustellpunkt"));
        assertTrue(slugField.getCopyable());
        assertTrue(slugField.getCopyValueTemplate().startsWith("https://prosuna.example/api/public/fit-connect/example/"));
        var callbackSecret = layout
                .findChild(FitConnectTriggerConfigV1.CALLBACK_SECRET_KEY, SecretSelectInputElement.class)
                .orElseThrow();
        assertEquals("Callback-Secret", callbackSecret.getLabel());
        assertTrue(callbackSecret.getHint().contains("Auf beiden Seiten muss derselbe Wert verwendet werden"));
        assertEquals("Client-ID", layout
                .findChild(FitConnectTriggerConfigV1.SUBSCRIBER_CLIENT_ID_CONFIG_KEY, TextInputElement.class)
                .orElseThrow().getLabel());
        var clientSecret = layout
                .findChild(FitConnectTriggerConfigV1.SUBSCRIBER_CLIENT_SECRET_CONFIG_KEY, SecretSelectInputElement.class)
                .orElseThrow();
        assertEquals("Client-Secret", clientSecret.getLabel());
        assertTrue(clientSecret.getHint().contains("vom Callback-Secret getrennt"));
        var environment = layout
                .findChild(FitConnectTriggerConfigV1.ENVIRONMENT_CONFIG_KEY, SelectInputElement.class)
                .orElseThrow();
        assertTrue(environment.getHint().contains("Der Prosuna-Testmodus ändert diese Auswahl nicht"));
        var copyToProcessData = layout
                .findChild(FitConnectTriggerConfigV1.COPY_TO_PROCESS_DATA_CONFIG_KEY, CheckboxInputElement.class)
                .orElseThrow();
        assertEquals("Fachdaten in Vorgangsdaten übernehmen", copyToProcessData.getLabel());
        assertTrue(copyToProcessData.getHint().contains("JSON-Objekt erforderlich"));
        assertTrue(copyToProcessData.getHint().contains("Vorgangsdaten leer"));
        var slugValidation = slugField.getValidation();
        assertNotNull(slugValidation);
        assertEquals(ValidationFunctionType.NoCode, slugValidation.getType());
        assertEquals(List.of(FitConnectTriggerConfigV1.SLUG_CONFIG_KEY), List.copyOf(slugValidation.getReferencedIds()));
        var slugExpression = assertInstanceOf(
                NoCodeExpression.class,
                slugValidation.getNoCodeList().getFirst().getNoCode()
        );
        assertEquals(NoCodeRegexMatchOperator.OPERATOR_ID, slugExpression.getOperatorIdentifier());
        assertEquals(
                List.of(
                        NoCodeReference.of(FitConnectTriggerConfigV1.SLUG_CONFIG_KEY),
                        NoCodeStaticValue.of("^[a-z0-9-]+$")
                ),
                List.copyOf(slugExpression.getOperands())
        );

        assertTrue(layout.findChild(FitConnectTriggerConfigV1.ENVIRONMENT_CONFIG_KEY).isPresent());
        assertTrue(layout.findChild(FitConnectTriggerConfigV1.DESTINATION_ID_CONFIG_KEY).isPresent());
        assertTrue(layout.findChild(FitConnectTriggerConfigV1.SUBSCRIBER_CLIENT_ID_CONFIG_KEY).isPresent());
        assertTrue(layout.findChild(FitConnectTriggerConfigV1.SUBSCRIBER_CLIENT_SECRET_CONFIG_KEY).isPresent());
        assertTrue(layout.findChild(FitConnectTriggerConfigV1.PRIVATE_SIGNING_KEY_CONFIG_KEY).isPresent());
        assertTrue(layout.findChild(FitConnectTriggerConfigV1.PRIVATE_DECRYPTION_KEYS_CONFIG_KEY).isPresent());
        assertTrue(layout.findChild(FitConnectTriggerConfigV1.PrivateDecryptionKeyConfig.KEY_FILE_CONFIG_KEY).isPresent());
        assertTrue(layout.findChild(FitConnectTriggerConfigV1.CALLBACK_SECRET_KEY).isPresent());
        assertTrue(layout.findChild(FitConnectTriggerConfigV1.COPY_TO_PROCESS_DATA_CONFIG_KEY).isPresent());
        assertTrue(layout.findChild(REMOVED_DESTINATION_TYPE_CONFIG_KEY).isEmpty());

        var privateSigningKey = layout
                .findChild(FitConnectTriggerConfigV1.PRIVATE_SIGNING_KEY_CONFIG_KEY, AssetSelectInputElement.class)
                .orElseThrow();
        var privateDecryptionKeys = layout
                .findChild(FitConnectTriggerConfigV1.PRIVATE_DECRYPTION_KEYS_CONFIG_KEY, ReplicatingContainerLayoutElement.class)
                .orElseThrow();
        assertTrue(privateSigningKey.getRequired());
        assertEquals(List.of("application/json"), privateSigningKey.getAllowedMimeTypes());
        assertEquals(AssetVisibility.Private, privateSigningKey.getAssetVisibility());
        assertTrue(privateDecryptionKeys.getRequired());
        assertNull(privateSigningKey.getVisibility());
        assertNull(privateDecryptionKeys.getVisibility());
    }

    @Test
    void declaresAllImportedValuesAsOutputs() {
        var node = createNode();

        assertEquals(
                List.of("payload", "submission", "metadata", "attachments", "files", "started"),
                node.getOutputs().stream().map(output -> output.key()).toList()
        );
        assertEquals(
                List.of("Fachdaten (JSON)", "Einreichungsdetails", "Metadaten", "Anlagen", "Dateiverweise", "Eingegangen am"),
                node.getOutputs().stream().map(output -> output.label()).toList()
        );
        var port = node.getPorts(new FitConnectTriggerConfigV1()).getFirst();
        assertEquals("output", port.key());
        assertEquals("Einreichung empfangen", port.label());
    }

    @Test
    void testingHelpKeepsTheFullCallbackUrlAndExplainsSecretAndEnvironment() {
        var prosunaConfig = new ProsunaConfig();
        prosunaConfig.setProsunaHostname("https://example.test/");
        var node = new FitConnectTriggerNodeV1(
                new PublicUrlService(prosunaConfig),
                mock(ProcessNodeRepository.class),
                mock(FitConnectTriggerOrganisationFactoryV1.class)
        );
        var config = new FitConnectTriggerConfigV1();
        config.slug = "eingang";
        config.environment = "PROD";

        var layout = node.getTestingLayout(new ProcessNodeDefinitionTestingLayoutContext<>(
                new UserEntity().setId("staff"),
                new ProcessEntity().setSlug("example"),
                new ProcessVersionEntity(),
                new ProcessNodeEntity(),
                new ProcessTestClaimEntity().setAccessKey("test-session"),
                config
        ));

        var content = layout.findChild("callback-url", RichTextContentElement.class).orElseThrow().getContent();
        assertTrue(content.contains("https://example.test/api/public/fit-connect/example/eingang/?test-claim=test-session"), content);
        assertTrue(content.contains("vollständige Callback-URL"), content);
        assertTrue(content.contains("denselben Callback-Secret-Wert wie in Prosuna"), content);
        assertTrue(content.contains("Benachrichtigungen weiterhin damit absichern"), content);
        assertTrue(content.contains("`test-claim` ordnet die Einreichung Ihrer Prosuna-Testsitzung zu"), content);
        assertTrue(content.contains("FIT-Connect-Umgebung wird durch den Prosuna-Testmodus nicht geändert"), content);
    }

    @Test
    void initExposesImportedValuesWithoutCopyingProcessDataByDefault() throws Exception {
        var startedAt = Instant.parse("2026-09-02T12:00:00Z");
        var payload = Map.<String, Object>of("applicant", "Ada");
        var submission = Map.<String, Object>of("submissionId", "submission");
        var metadata = Map.<String, Object>of("schemaVersion", "1.0");
        var attachments = List.of(Map.<String, Object>of("filename", "proof.pdf"));
        var files = List.<Object>of(Map.of("name", "proof.pdf"));
        var initialPayload = importedPayload(payload, submission, metadata, attachments, files, startedAt);
        var config = new FitConnectTriggerConfigV1();

        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskCompleted.class,
                createNode().init(context(initialPayload, config))
        );

        assertEquals("output", result.getViaPort());
        assertEquals(initialPayload, result.getNodeData());
        assertTrue(result.getProcessData().isEmpty());
    }

    @Test
    void initCopiesObjectPayloadIntoProcessDataWhenEnabled() throws Exception {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("applicant", "Ada");
        payload.put("amount", 42);
        var config = new FitConnectTriggerConfigV1();
        config.copyToProcessData = true;

        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskCompleted.class,
                createNode().init(context(
                        importedPayload(payload, Map.of(), Map.of(), List.of(), List.of(), Instant.EPOCH),
                        config
                ))
        );

        assertEquals(payload, result.getProcessData());
    }

    @Test
    void initRejectsNonObjectPayloadWhenCopyingIsEnabled() {
        var config = new FitConnectTriggerConfigV1();
        config.copyToProcessData = true;

        var exception = assertThrows(
                ProcessNodeExecutionExceptionInvalidDataType.class,
                () -> createNode().init(context(
                        importedPayload(List.of("not", "an", "object"), Map.of(), Map.of(), List.of(), List.of(), Instant.EPOCH),
                        config
                ))
        );
        assertEquals(
                "Die FIT-Connect-Fachdaten können nicht in die Vorgangsdaten übernommen werden. Erwartet wird ein JSON-Objekt mit Feldnamen und Werten.",
                exception.getMessage()
        );
    }

    @Test
    void initRejectsNonTextObjectKeysWhenCopyingIsEnabled() {
        var config = new FitConnectTriggerConfigV1();
        config.copyToProcessData = true;

        var exception = assertThrows(
                ProcessNodeExecutionExceptionInvalidDataType.class,
                () -> createNode().init(context(
                        importedPayload(Map.of(42, "Ada"), Map.of(), Map.of(), List.of(), List.of(), Instant.EPOCH),
                        config
                ))
        );

        assertEquals("Die Feldnamen der FIT-Connect-Fachdaten müssen Texte sein.", exception.getMessage());
    }

    private static FitConnectTriggerNodeV1 createNode() {
        return new FitConnectTriggerNodeV1(
                mock(PublicUrlService.class),
                mock(ProcessNodeRepository.class),
                mock(FitConnectTriggerOrganisationFactoryV1.class)
        );
    }

    private static Map<String, Object> importedPayload(
            Object payload,
            Object submission,
            Object metadata,
            Object attachments,
            Object files,
            Instant startedAt) {
        var values = new LinkedHashMap<String, Object>();
        values.put(FitConnectTriggerNodeV1.INITIAL_DATA_KEY_PAYLOAD, payload);
        values.put(FitConnectTriggerNodeV1.INITIAL_DATA_KEY_SUBMISSION, submission);
        values.put(FitConnectTriggerNodeV1.INITIAL_DATA_KEY_METADATA, metadata);
        values.put(FitConnectTriggerNodeV1.INITIAL_DATA_KEY_ATTACHMENTS, attachments);
        values.put(FitConnectTriggerNodeV1.INITIAL_DATA_KEY_FILES, files);
        values.put(FitConnectTriggerNodeV1.INITIAL_DATA_KEY_STARTED, startedAt);
        return values;
    }

    private static ProcessNodeExecutionInitContext<FitConnectTriggerConfigV1> context(
            Map<String, Object> initialPayload,
            FitConnectTriggerConfigV1 config) {
        var instance = new ProcessInstanceEntity().setInitialPayload(initialPayload);
        return new ProcessNodeExecutionInitContext<>(
                mock(ProcessNodeExecutionLogger.class),
                mock(ProcessNodeEntity.class),
                instance,
                mock(ProcessInstanceTaskEntity.class),
                null,
                new ProcessExecutionData(),
                config
        );
    }
}
