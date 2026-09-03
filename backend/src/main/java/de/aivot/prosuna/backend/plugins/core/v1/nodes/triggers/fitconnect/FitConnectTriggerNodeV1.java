package de.aivot.prosuna.backend.plugins.core.v1.nodes.triggers.fitconnect;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.prosuna.backend.elements.models.elements.form.input.StoragePathSelectorInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElementPattern;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.elements.enums.StoragePathSelectorMode;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidDataType;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinitionMetadata;
import de.aivot.prosuna.backend.process.models.ProcessNodeOutput;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionTestingLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.filters.ProcessNodeFilter;
import de.aivot.prosuna.backend.process.repositories.ProcessNodeRepository;
import de.aivot.prosuna.backend.process.services.PublicUrlService;
import de.aivot.prosuna.backend.storage.enums.StorageProviderType;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class FitConnectTriggerNodeV1 implements ProcessNodeDefinition<FitConnectTriggerConfigV1> {
    public static final String NODE_KEY = "fit_connect_trigger";
    private static final String PORT_NAME = "output";
    private static final String COPY_VALUE_TEMPLATE_PATH_SEGMENT = "__copy_value__";

    public static final String INITIAL_DATA_KEY_PAYLOAD = "payload";
    public static final String INITIAL_DATA_KEY_SUBMISSION = "submission";
    public static final String INITIAL_DATA_KEY_METADATA = "metadata";
    public static final String INITIAL_DATA_KEY_ATTACHMENTS = "attachments";
    public static final String INITIAL_DATA_KEY_FILES = "files";
    public static final String INITIAL_DATA_KEY_STARTED = "started";

    private static final String SUBMISSION_OUTPUT_TYPE =
            "{ destinationId: string; submissionId: string; caseId: string; submittedAt: string | null; " +
                    "service: { identifier: string | null; name: string | null; }; region: string | null; " +
                    "dataMimeType: string | null; dataSchemaUri: string | null; }";
    private static final String ATTACHMENTS_OUTPUT_TYPE =
            "Array<{ fitConnectAttachmentId: string | null; key: string; filename: string; originalFilename: string; " +
                    "description: string | null; mimeType: string | null; purpose: string | null; size: number; " +
                    "storageProviderId: number; storagePathFromRoot: string; }>";
    private static final String FILES_OUTPUT_TYPE =
            "Array<{ name: string; originalFileName: string; uri: string; size: number; }>";

    private final PublicUrlService publicUrlService;
    private final ProcessNodeRepository processNodeRepository;
    private final FitConnectTriggerSubscriberClientFactoryV1 subscriberClientFactory;

    public FitConnectTriggerNodeV1(PublicUrlService publicUrlService,
                                   ProcessNodeRepository processNodeRepository,
                                   FitConnectTriggerSubscriberClientFactoryV1 subscriberClientFactory) {
        this.publicUrlService = publicUrlService;
        this.processNodeRepository = processNodeRepository;
        this.subscriberClientFactory = subscriberClientFactory;
    }

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return CorePlugin.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    public String getComponentKey() {
        return NODE_KEY;
    }

    @Nonnull
    @Override
    public String getComponentVersion() {
        return "1.0.0";
    }

    @Nonnull
    @Override
    public String getName() {
        return "FIT-Connect Zustellpunkt";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Empfange Daten von FIT-Connect und starte den Prozess.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                Empfängt neue Einreichungen eines FIT-Connect-Zustellpunkts und startet für jede Einreichung einen Vorgang.

                Die Fach- und Metadaten werden über die FIT-Connect-API abgerufen und entschlüsselt. Übermittelte Anhänge werden als Vorgangsanhänge gespeichert; JSON-Daten, Metadaten, Anhänge und Einreichungsinformationen stehen als Ausgänge zur Verfügung.
                """;
    }

    @Nonnull
    @Override
    public ProcessNodeType getType() {
        return ProcessNodeType.Trigger;
    }

    @Nonnull
    @Override
    public ProcessNodeExecutionType[] getExecutionTypes() {
        return new ProcessNodeExecutionType[] {
                ProcessNodeExecutionType.Automatic,
        };
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of(
                new ProcessNodePort(
                        PORT_NAME,
                        "Einreichung empfangen",
                        "Der Prozess wird mit den importierten FIT-Connect-Daten fortgesetzt."
                )
        );
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(INITIAL_DATA_KEY_PAYLOAD, "JSON-Daten", "Die entschlüsselten Fach- oder Antragsdaten.", "unknown"),
                new ProcessNodeOutput(INITIAL_DATA_KEY_SUBMISSION, "Einreichung", "Technische Informationen zur FIT-Connect-Einreichung.", SUBMISSION_OUTPUT_TYPE),
                new ProcessNodeOutput(INITIAL_DATA_KEY_METADATA, "Metadaten", "Die entschlüsselten FIT-Connect-Metadaten.", "Record<string, unknown>"),
                new ProcessNodeOutput(INITIAL_DATA_KEY_ATTACHMENTS, "Anhänge", "Die gespeicherten FIT-Connect-Anhänge.", ATTACHMENTS_OUTPUT_TYPE),
                new ProcessNodeOutput(INITIAL_DATA_KEY_FILES, "Dateien", "Die Anhänge im Format eines Datei-Anlagen-Feldes.", FILES_OUTPUT_TYPE),
                new ProcessNodeOutput(INITIAL_DATA_KEY_STARTED, "Eingangszeitstempel", "Der Zeitpunkt, an dem der Callback empfangen wurde.", "string")
        );
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        final ConfigLayoutElement layout;
        try {
            layout = ElementPOJOMapper.createFromPOJO(FitConnectTriggerConfigV1.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(
                    "Das Konfigurationslayout des FIT-Connect-Triggers konnte nicht erstellt werden.",
                    e
            );
        }

        layout
                .findChild(FitConnectTriggerConfigV1.SLUG_CONFIG_KEY, TextInputElement.class)
                .ifPresent(field -> {
                    field.setPattern(new TextInputElementPattern()
                            .setRegex("^[a-z0-9-]+$")
                            .setMessage("Das URL-Segment darf nur aus Kleinbuchstaben, Zahlen und Bindestrichen bestehen."));
                    field.setPrefix(publicUrlService.createProcessNamespaceDisplayPrefix());
                    field.setCopyable(true);
                    field.setCopyValueTemplate(createCallbackCopyValueTemplate(context.processDefinition()));
                });

        layout
                .findChild(FitConnectTriggerConfigV1.ENVIRONMENT_CONFIG_KEY, SelectInputElement.class)
                .ifPresent(field -> field.setOptions(List.of(
                        SelectInputElementOption.of("TEST", "TEST"),
                        SelectInputElementOption.of("STAGE", "STAGE"),
                        SelectInputElementOption.of("PROD", "PROD")
                )));

        configureKeyFileSelector(layout, FitConnectTriggerConfigV1.PRIVATE_SIGNING_KEY_CONFIG_KEY);
        configureKeyFileSelector(layout, FitConnectTriggerConfigV1.PrivateDecryptionKeyConfig.KEY_FILE_CONFIG_KEY);
        return layout;
    }

    @Nullable
    @Override
    public Map<String, List<String>> validateConfiguration(@Nonnull ProcessNodeEntity processNodeEntity,
                                                           @Nonnull FitConnectTriggerConfigV1 configuration) {
        var errors = new LinkedHashMap<String, List<String>>();
        var slug = StringUtils.toNullableTrimmedString(configuration.slug);
        if (slug == null) {
            addValidationError(errors, FitConnectTriggerConfigV1.SLUG_CONFIG_KEY, "Das URL-Segment muss hinterlegt werden.");
        } else {
            if (!slug.matches("^[a-z0-9-]+$")) {
                addValidationError(
                        errors,
                        FitConnectTriggerConfigV1.SLUG_CONFIG_KEY,
                        "Das URL-Segment darf nur aus Kleinbuchstaben, Zahlen und Bindestrichen bestehen."
                );
            }

            var duplicateNodeFilter = ProcessNodeFilter
                    .create()
                    .setNotId(processNodeEntity.getId())
                    .setProcessId(processNodeEntity.getProcessId())
                    .setProcessVersion(processNodeEntity.getProcessVersion())
                    .setProcessNodeDefinitionKey(processNodeEntity.getProcessNodeDefinitionKey())
                    .addConfigEquals(FitConnectTriggerConfigV1.SLUG_CONFIG_KEY, slug);
            if (processNodeRepository.exists(duplicateNodeFilter.build())) {
                addValidationError(
                        errors,
                        FitConnectTriggerConfigV1.SLUG_CONFIG_KEY,
                        "Das URL-Segment wird in dieser Prozessversion bereits von einem anderen FIT-Connect-Trigger verwendet."
                );
            }
        }

        var destinationId = StringUtils.toNullableTrimmedString(configuration.destinationId);
        if (destinationId == null) {
            addValidationError(errors, FitConnectTriggerConfigV1.DESTINATION_ID_CONFIG_KEY, "Die Zustellpunkt-ID muss hinterlegt werden.");
        } else {
            try {
                UUID.fromString(destinationId);
            } catch (IllegalArgumentException e) {
                addValidationError(errors, FitConnectTriggerConfigV1.DESTINATION_ID_CONFIG_KEY, "Die Zustellpunkt-ID muss eine gültige UUID sein.");
            }
        }

        for (var issue : subscriberClientFactory.validateConfiguration(configuration)) {
            addValidationError(errors, issue.fieldId(), issue.message());
        }
        return errors.isEmpty() ? null : errors;
    }

    @Nonnull
    @Override
    public ProcessNodeDefinitionMetadata getMetadata(@Nonnull ProcessNodeEntity processNodeEntity,
                                                     @Nonnull FitConnectTriggerConfigV1 configuration,
                                                     @Nonnull ProcessNodeDefinitionMetadata previousMetadata) {
        return ProcessNodeDefinitionMetadata
                .reuse(previousMetadata)
                .addForwardedAttachmentSet(
                        processNodeEntity.getDataKey(),
                        "FIT-Connect-Anhänge",
                        "Alle mit der Einreichung empfangenen Anhänge.",
                        true,
                        processNodeEntity
                );
    }

    @Nullable
    @Override
    public GroupLayoutElement getTestingLayout(@Nonnull ProcessNodeDefinitionTestingLayoutContext<FitConnectTriggerConfigV1> context) {
        var callbackUrl = createCallbackUrl(context.processDefinition(), context.configuration().slug) +
                "?" + FitConnectTriggerControllerV1.TEST_CLAIM_QUERY_PARAM + "=" + context.testClaim().getAccessKey();

        var text = new RichTextContentElement();
        text.setId("callback-url");
        text.setContent("""
                Verwenden Sie für einen Test-Zustellpunkt die folgende Callback-URL:

                <%s>

                FIT-Connect muss den Request weiterhin mit dem konfigurierten Callback-Secret signieren.
                """.formatted(callbackUrl));

        var layout = new GroupLayoutElement();
        layout.setId("fit-connect-testing-layout");
        layout.setChildren(new LinkedList<>(List.of(text)));
        return layout;
    }

    @Nonnull
    @Override
    public AuthoredElementValues cleanConfigurationForExport(@Nonnull AuthoredElementValues configuration) {
        configuration.remove(FitConnectTriggerConfigV1.DESTINATION_ID_CONFIG_KEY);
        configuration.remove(FitConnectTriggerConfigV1.SUBSCRIBER_CLIENT_ID_CONFIG_KEY);
        configuration.remove(FitConnectTriggerConfigV1.SUBSCRIBER_CLIENT_SECRET_CONFIG_KEY);
        configuration.remove(FitConnectTriggerConfigV1.PRIVATE_SIGNING_KEY_CONFIG_KEY);
        configuration.remove(FitConnectTriggerConfigV1.PRIVATE_DECRYPTION_KEYS_CONFIG_KEY);
        configuration.remove(FitConnectTriggerConfigV1.CALLBACK_SECRET_KEY);
        return configuration;
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<FitConnectTriggerConfigV1> context) throws ProcessNodeExecutionException {
        var config = context.getConfigurationOfExecutingNode();
        var initialPayload = context.getThisProcessInstance().getInitialPayload();

        var nodeData = new LinkedHashMap<String, Object>();
        nodeData.put(INITIAL_DATA_KEY_PAYLOAD, initialPayload.get(INITIAL_DATA_KEY_PAYLOAD));
        nodeData.put(INITIAL_DATA_KEY_SUBMISSION, initialPayload.get(INITIAL_DATA_KEY_SUBMISSION));
        nodeData.put(INITIAL_DATA_KEY_METADATA, initialPayload.get(INITIAL_DATA_KEY_METADATA));
        nodeData.put(INITIAL_DATA_KEY_ATTACHMENTS, initialPayload.get(INITIAL_DATA_KEY_ATTACHMENTS));
        nodeData.put(INITIAL_DATA_KEY_FILES, initialPayload.get(INITIAL_DATA_KEY_FILES));
        nodeData.put(INITIAL_DATA_KEY_STARTED, initialPayload.get(INITIAL_DATA_KEY_STARTED));

        Map<String, Object> processData = Map.of();
        if (Boolean.TRUE.equals(config.copyToProcessData)) {
            var payload = nodeData.get(INITIAL_DATA_KEY_PAYLOAD);
            if (!(payload instanceof Map<?, ?> payloadMap)) {
                throw new ProcessNodeExecutionExceptionInvalidDataType(
                        "Die empfangenen FIT-Connect-Daten können nicht in die Vorgangsdaten kopiert werden, weil das JSON kein Objekt ist."
                );
            }

            var convertedPayload = new LinkedHashMap<String, Object>();
            for (var entry : payloadMap.entrySet()) {
                if (!(entry.getKey() instanceof String key)) {
                    throw new ProcessNodeExecutionExceptionInvalidDataType(
                            "Die empfangenen FIT-Connect-Daten enthalten einen ungültigen Objektschlüssel."
                    );
                }
                convertedPayload.put(key, entry.getValue());
            }
            processData = convertedPayload;
        }

        return new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(PORT_NAME)
                .setNodeData(nodeData)
                .setProcessData(processData);
    }

    @Nonnull
    @Override
    public Class<FitConnectTriggerConfigV1> getNodeConfigurationClass() {
        return FitConnectTriggerConfigV1.class;
    }

    private void configureKeyFileSelector(@Nonnull ConfigLayoutElement layout,
                                          @Nonnull String fieldId) {
        layout
                .findChild(fieldId, StoragePathSelectorInputElement.class)
                .ifPresent(field -> {
                    field.setMode(StoragePathSelectorMode.File);
                    field.setAllowedStorageProviderTypes(List.of(StorageProviderType.Assets));
                    field.setAllowReadOnlyStorageProviders(true);
                });
    }

    @Nonnull
    private String createCallbackCopyValueTemplate(@Nonnull ProcessEntity process) {
        return createCallbackUrl(process, COPY_VALUE_TEMPLATE_PATH_SEGMENT)
                .replace(COPY_VALUE_TEMPLATE_PATH_SEGMENT, TextInputElement.COPY_VALUE_TEMPLATE_PLACEHOLDER);
    }

    @Nonnull
    private String createCallbackUrl(@Nonnull ProcessEntity process,
                                     @Nullable String slug) {
        return publicUrlService.createPublicApiUrl("fit-connect", process.getSlug(), slug);
    }

    private static void addValidationError(@Nonnull Map<String, List<String>> errors,
                                           @Nonnull String fieldId,
                                           @Nonnull String message) {
        errors.computeIfAbsent(fieldId, ignored -> new LinkedList<>()).add(message);
    }
}
