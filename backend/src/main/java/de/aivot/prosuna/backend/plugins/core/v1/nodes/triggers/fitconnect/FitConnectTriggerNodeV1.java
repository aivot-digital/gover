package de.aivot.prosuna.backend.plugins.core.v1.nodes.triggers.fitconnect;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.elements.ElementValidationFunctions;
import de.aivot.prosuna.backend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.prosuna.backend.elements.models.elements.form.input.AssetSelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.elements.enums.AssetVisibility;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.nocode.models.NoCodeExpression;
import de.aivot.prosuna.backend.nocode.models.NoCodeReference;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.plugins.core.v1.operators.text.NoCodeRegexMatchOperator;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidDataType;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionSummaryContext;
import de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeConfigurationValidationContext;
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
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.detail;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.map;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.timestamp;

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
    private final FitConnectTriggerOrganisationFactoryV1 organisationFactory;

    public FitConnectTriggerNodeV1(PublicUrlService publicUrlService,
                                   ProcessNodeRepository processNodeRepository,
                                   FitConnectTriggerOrganisationFactoryV1 organisationFactory) {
        this.publicUrlService = publicUrlService;
        this.processNodeRepository = processNodeRepository;
        this.organisationFactory = organisationFactory;
    }

    @Nonnull
    @Override
    public String generateExecutionSummary(@Nonnull ProcessNodeExecutionSummaryContext<FitConnectTriggerConfigV1> context) {
        var summary = new ExecutionSummaryMarkdown(context);
        var started = summary.data(INITIAL_DATA_KEY_STARTED);
        var receivedAt = timestamp(started).isEmpty() ? context.thisProcessInstance().getStarted() : started;
        var submission = map(summary.data(INITIAL_DATA_KEY_SUBMISSION));
        return "Der Vorgang wurde" + summary.at(receivedAt) + " durch eine über FIT-Connect empfangene Einreichung ausgelöst."
                + detail("Zustellpunkt-ID", submission.get("destinationId"))
                + detail("Einreichungs-ID", submission.get("submissionId"))
                + detail("Fall-ID bei FIT-Connect", submission.get("caseId"))
                + detail("Bei FIT-Connect eingereicht am", timestamp(submission.get("submittedAt")))
                + detail("Kurzbeschreibung", context.thisNode().getDescription());
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
        return "FIT-Connect-Zustellpunkt";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Startet einen neuen Vorgang aus einer über FIT-Connect empfangenen Einreichung.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                ### Wofür verwenden Sie dieses Prozesselement?

                Empfangen Sie Fachdaten und Anhänge über einen bereits eingerichteten FIT-Connect-Zustellpunkt und starten Sie daraus einen Vorgang in Prosuna. FIT-Connect bezeichnet die übermittelten Daten und Anhänge als Einreichung. Eine neu übernommene Einreichung startet einen eigenen Vorgang.

                ### Was richten Sie ein?

                Legen Sie unter „Callback-URL (URL-Segment)“ den letzten Teil der Prosuna-Empfangsadresse fest. Kopieren Sie anschließend über das Kopiersymbol die vollständige Callback-URL und hinterlegen Sie diese im Zustellpunkt bei FIT-Connect. Die Callback-URL muss für FIT-Connect über das Internet per HTTPS erreichbar sein.

                Wählen Sie direkt darunter das „Callback-Secret“. Hinterlegen Sie den Wert dieses Geheimnisses auch im FIT-Connect-Zustellpunkt als Callback-Secret. Auf beiden Seiten muss derselbe Wert verwendet werden; damit prüft Prosuna die eingehenden Benachrichtigungen.

                Wählen Sie die zum Zustellpunkt passende FIT-Connect-Umgebung und geben Sie dessen Zustellpunkt-ID an. Für den Abruf benötigen Sie die Client-ID und das zugehörige Client-Secret des empfangenden Systems sowie die passenden privaten Signatur- und Entschlüsselungsschlüssel im JWK-Format (JSON Web Key).

                Mit „Fachdaten in Vorgangsdaten übernehmen“ legen Sie fest, ob die JSON-Fachdaten zusätzlich in die Vorgangsdaten übernommen werden. Dafür müssen die Fachdaten ein JSON-Objekt mit Feldnamen und Werten sein.

                ### Was passiert bei der Ausführung?

                FIT-Connect benachrichtigt Prosuna über neue Einreichungen. Prosuna prüft die Benachrichtigung, reserviert einen Vorgang und ruft die Einreichung ab. Die Fachdaten, Metadaten und Anhänge werden entschlüsselt und durch die vorhandene FIT-Connect-Empfangsprüfung geprüft.

                Prosuna speichert die Daten und Anlagen lokal und bestätigt anschließend die Annahme bei FIT-Connect. Danach wird der Vorgang über „Einreichung empfangen“ fortgesetzt. Bereits angenommene oder zurückgewiesene Einreichungen werden bei weiteren Benachrichtigungen nicht erneut importiert.

                ### Welches Ergebnis steht danach bereit?

                In den Elementdaten stehen die JSON-Fachdaten, Einreichungsdetails, FIT-Connect-Metadaten, Anlagen, Dateiverweise und der Zeitpunkt der Benachrichtigung bereit. Bei aktivierter Übernahme stehen die Eigenschaften des Fachdatenobjekts zusätzlich in den Vorgangsdaten. Andernfalls bleiben die Vorgangsdaten leer.

                Der Vorgangsverlauf nennt den Eingang über FIT-Connect und die zugehörigen Kennungen. Der ursprüngliche Einreichungszeitpunkt bei FIT-Connect wird zusätzlich angezeigt, wenn er aus dem FIT-Connect-Fallprotokoll ermittelt werden konnte. Die Zustellpunkt-ID bezeichnet den empfangenden Endpunkt.

                ### Wie sieht ein Beispiel aus?

                FIT-Connect meldet eine neue Einreichung mit den Fachdaten `{"kontakt":{"name":"Ada"}}` und einem PDF-Anhang. Prosuna ruft beides ab und speichert das PDF als Anlage. Ist „Fachdaten in Vorgangsdaten übernehmen“ aktiviert, steht der Name im nächsten Schritt unter `$.kontakt.name` bereit.

                ### Was ist zu beachten?

                Die Callback-URL und das Callback-Secret werden zusätzlich im FIT-Connect-Zustellpunkt eingerichtet. Dieses Prozesselement legt den externen Zustellpunkt nicht automatisch an.

                Der Prosuna-Testmodus verwendet weiterhin die hier konfigurierte FIT-Connect-Umgebung. Die Testadresse ordnet Einreichungen einer Prosuna-Testsitzung zu; sie schaltet die FIT-Connect-Verbindung nicht automatisch auf TEST um. Auch im Test müssen Benachrichtigungen mit dem passenden Callback-Secret abgesichert sein.

                Eine eigene JSON-Schema-Prüfung können Sie in diesem Prozesselement derzeit nicht konfigurieren. Die vorhandenen SDK-Empfangsprüfungen bleiben wirksam. Eine Annahme bei FIT-Connect bestätigt den technischen Empfang, nicht den fachlichen Abschluss des Vorgangs.
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
    public List<ProcessNodePort> getPorts(@Nonnull FitConnectTriggerConfigV1 configuration) {
        return List.of(
                new ProcessNodePort(
                        PORT_NAME,
                        "Einreichung empfangen",
                        "Die Einreichung wurde übernommen und bei FIT-Connect angenommen. Der Vorgang wird hier fortgesetzt."
                )
        );
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(INITIAL_DATA_KEY_PAYLOAD, "Fachdaten (JSON)", "Die entschlüsselten JSON-Fachdaten der Einreichung. Bei aktivierter Übernahme werden die Eigenschaften eines JSON-Objekts zusätzlich in den Vorgangsdaten bereitgestellt.", "unknown"),
                new ProcessNodeOutput(INITIAL_DATA_KEY_SUBMISSION, "Einreichungsdetails", "Die gespeicherten Angaben zur FIT-Connect-Einreichung, darunter Zustellpunkt-ID, Einreichungs-ID, Fall-ID und gegebenenfalls der Einreichungszeitpunkt.", SUBMISSION_OUTPUT_TYPE),
                new ProcessNodeOutput(INITIAL_DATA_KEY_METADATA, "Metadaten", "Die entschlüsselten Begleitangaben der FIT-Connect-Einreichung.", "Record<string, unknown>"),
                new ProcessNodeOutput(INITIAL_DATA_KEY_ATTACHMENTS, "Anlagen", "Informationen zu den FIT-Connect-Anhängen, die als Anlagen beim Vorgang gespeichert wurden.", ATTACHMENTS_OUTPUT_TYPE),
                new ProcessNodeOutput(INITIAL_DATA_KEY_FILES, "Dateiverweise", "Die importierten Anhänge als Dateiverweise zur Weiterverwendung in Anlagenfeldern.", FILES_OUTPUT_TYPE),
                new ProcessNodeOutput(INITIAL_DATA_KEY_STARTED, "Eingegangen am", "Der bei der Verarbeitung der FIT-Connect-Benachrichtigung festgehaltene Zeitpunkt im ISO-Format. Der ursprüngliche FIT-Connect-Einreichungszeitpunkt steht gesondert in den Einreichungsdetails.", "string")
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
            throw ResponseException.internalServerErrorWithDetails(
                    e,
                    "Die Einstellungen für den FIT-Connect-Zustellpunkt konnten nicht geladen werden.",
                    null
            );
        }

        layout
                .findChild(FitConnectTriggerConfigV1.SLUG_CONFIG_KEY, TextInputElement.class)
                .ifPresent(field -> {
                    field.setValidation(ElementValidationFunctions.of(
                            NoCodeExpression.of(
                                    NoCodeRegexMatchOperator.OPERATOR_ID,
                                    NoCodeReference.of(FitConnectTriggerConfigV1.SLUG_CONFIG_KEY),
                                    NoCodeStaticValue.of("^[a-z0-9-]+$")
                            ),
                            "Das URL-Segment der Callback-URL darf nur Kleinbuchstaben von a bis z, Zahlen und Bindestriche enthalten."
                    ));
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
    public Map<String, List<String>> validateConfiguration(@Nonnull ProcessNodeConfigurationValidationContext<FitConnectTriggerConfigV1> context) {
        if (!context.isAuthoring()) {
            return null;
        }
        var processNodeEntity = context.thisNode();
        var configuration = context.configuration();
        var errors = new LinkedHashMap<String, List<String>>();
        var slug = StringUtils.toNullableTrimmedString(configuration.slug);
        if (slug == null) {
            addValidationError(errors, FitConnectTriggerConfigV1.SLUG_CONFIG_KEY, "Legen Sie ein URL-Segment für die Callback-URL fest.");
        } else {
            if (!slug.matches("^[a-z0-9-]+$")) {
                addValidationError(
                        errors,
                        FitConnectTriggerConfigV1.SLUG_CONFIG_KEY,
                        "Das URL-Segment der Callback-URL darf nur Kleinbuchstaben von a bis z, Zahlen und Bindestriche enthalten."
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
                        "Das URL-Segment der Callback-URL wird in dieser Prozessversion bereits von einem anderen FIT-Connect-Zustellpunkt verwendet. Wählen Sie ein anderes Segment."
                );
            }
        }

        var destinationId = StringUtils.toNullableTrimmedString(configuration.destinationId);
        if (destinationId == null) {
            addValidationError(errors, FitConnectTriggerConfigV1.DESTINATION_ID_CONFIG_KEY, "Hinterlegen Sie die Zustellpunkt-ID aus FIT-Connect.");
        } else {
            try {
                UUID.fromString(destinationId);
            } catch (IllegalArgumentException e) {
                addValidationError(errors, FitConnectTriggerConfigV1.DESTINATION_ID_CONFIG_KEY, "Die Zustellpunkt-ID muss eine gültige UUID sein.");
            }
        }

        for (var issue : organisationFactory.validateConfiguration(configuration)) {
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
                        "FIT-Connect-Anlagen",
                        "Alle mit der Übermittlung empfangenen Anlagen.",
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
                Hinterlegen Sie für Ihren Test-Zustellpunkt in FIT-Connect die folgende vollständige Callback-URL:

                <%s>

                Hinterlegen Sie dort außerdem denselben Callback-Secret-Wert wie in Prosuna. FIT-Connect muss Benachrichtigungen weiterhin damit absichern. Der Parameter `test-claim` ordnet die Einreichung Ihrer Prosuna-Testsitzung zu. Die ausgewählte FIT-Connect-Umgebung wird durch den Prosuna-Testmodus nicht geändert.
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
                        "Die FIT-Connect-Fachdaten können nicht in die Vorgangsdaten übernommen werden. Erwartet wird ein JSON-Objekt mit Feldnamen und Werten."
                );
            }

            var convertedPayload = new LinkedHashMap<String, Object>();
            for (var entry : payloadMap.entrySet()) {
                if (!(entry.getKey() instanceof String key)) {
                    throw new ProcessNodeExecutionExceptionInvalidDataType(
                            "Die Feldnamen der FIT-Connect-Fachdaten müssen Texte sein."
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
                .findChild(fieldId, AssetSelectInputElement.class)
                .ifPresent(field -> {
                    field.setAllowedMimeTypes(List.of("application/json"));
                    field.setAssetVisibility(AssetVisibility.Private);
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
