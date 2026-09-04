package de.aivot.prosuna.backend.plugins.core.v1.nodes.triggers.fitconnect;

import de.aivot.prosuna.backend.elements.models.elements.form.input.StoragePathSelectorInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidDataType;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.repositories.ProcessNodeRepository;
import de.aivot.prosuna.backend.process.services.PublicUrlService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
                mock(FitConnectTriggerSubscriberClientFactoryV1.class)
        );
        var process = new ProcessEntity().setSlug("example");

        var layout = node.getConfigurationLayout(new ProcessNodeDefinitionConfigurationLayoutContext(
                null,
                process,
                mock(ProcessVersionEntity.class),
                mock(ProcessNodeEntity.class)
        ));

        assertTrue(layout.findChild(FitConnectTriggerConfigV1.SLUG_CONFIG_KEY).isPresent());
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
                .findChild(FitConnectTriggerConfigV1.PRIVATE_SIGNING_KEY_CONFIG_KEY, StoragePathSelectorInputElement.class)
                .orElseThrow();
        var privateDecryptionKeys = layout
                .findChild(FitConnectTriggerConfigV1.PRIVATE_DECRYPTION_KEYS_CONFIG_KEY, ReplicatingContainerLayoutElement.class)
                .orElseThrow();
        assertTrue(privateSigningKey.getRequired());
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
        assertEquals("output", node.getPorts().getFirst().key());
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

        assertThrows(
                ProcessNodeExecutionExceptionInvalidDataType.class,
                () -> createNode().init(context(
                        importedPayload(List.of("not", "an", "object"), Map.of(), Map.of(), List.of(), List.of(), Instant.EPOCH),
                        config
                ))
        );
    }

    private static FitConnectTriggerNodeV1 createNode() {
        return new FitConnectTriggerNodeV1(
                mock(PublicUrlService.class),
                mock(ProcessNodeRepository.class),
                mock(FitConnectTriggerSubscriberClientFactoryV1.class)
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
