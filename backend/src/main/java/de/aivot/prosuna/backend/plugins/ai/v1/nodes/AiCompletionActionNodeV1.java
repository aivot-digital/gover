package de.aivot.prosuna.backend.plugins.ai.v1.nodes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.aivot.prosuna.backend.core.exceptions.HttpConnectionException;
import de.aivot.prosuna.backend.core.models.HttpServiceHeaders;
import de.aivot.prosuna.backend.core.services.HttpService;
import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.enums.InputMode;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.plugins.ai.AiPlugin;
import de.aivot.prosuna.backend.plugins.ai.properties.AiPluginProperties;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.enums.TemplateOutputFormat;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionMissingValue;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionSummaryContext;
import de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodeOutput;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeConfigurationValidationContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.secrets.entities.SecretEntity;
import de.aivot.prosuna.backend.secrets.services.SecretService;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.detail;

/**
 * Executes a prompt against the AI Completions API and exposes the response as node outputs.
 */
@Component
public class AiCompletionActionNodeV1 implements ProcessNodeDefinition<AiCompletionActionNodeV1.AiCompletionActionNodeConfig> {
    public static final String NODE_KEY = "ai_completion";

    private static final String SUCCESS_PORT = "success";

    private static final String OUTPUT_PROMPT = "prompt";
    private static final String OUTPUT_COMPLETION = "completion";
    private static final String OUTPUT_FINISH_REASON = "finishReason";
    private static final String OUTPUT_RESPONSE_MODEL = "responseModel";
    private static final String OUTPUT_USAGE = "usage";
    private static final String OUTPUT_USAGE_TYPE_DEFINITION =
            "{ prompt_tokens: number | null; completion_tokens: number | null; total_tokens: number | null; } | null";

    private static final double DEFAULT_TEMPERATURE = 0.01d;
    private static final double DEFAULT_TOP_P = 0.9d;
    private static final int DEFAULT_N = 1;
    private static final boolean DEFAULT_STREAM = false;

    private static final String apiChatCompletionsPathSuffix = "/chat/completions";

    private final HttpService httpService;
    private final SecretService secretService;
    private final AiPluginProperties aiPluginProperties;

    public AiCompletionActionNodeV1(HttpService httpService,
                                    SecretService secretService,
                                    AiPluginProperties aiPluginProperties) {
        this.httpService = httpService;
        this.secretService = secretService;
        this.aiPluginProperties = aiPluginProperties;
    }

    @Nonnull
    @Override
    public String generateExecutionSummary(@Nonnull ProcessNodeExecutionSummaryContext<AiCompletionActionNodeV1.AiCompletionActionNodeConfig> context) {
        var summary = new ExecutionSummaryMarkdown(context);
        return "Die KI-Anfrage wurde" + summary.completedAt() + " abgeschlossen."
                + detail("Verwendetes Modell", summary.data(OUTPUT_RESPONSE_MODEL))
                + detail("Kurzbeschreibung", context.thisNode().getDescription());
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
    public String getParentPluginKey() {
        return AiPlugin.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    public ProcessNodeType getType() {
        return ProcessNodeType.Action;
    }

    @Nonnull
    @Override
    public ProcessNodeExecutionType[] getExecutionTypes() {
        return new ProcessNodeExecutionType[]{ProcessNodeExecutionType.Automatic};
    }

    @Nonnull
    @Override
    public String getName() {
        return "KI-Antwort erzeugen";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Sendet eine Anfrage an ein KI-Modell und stellt die Textantwort als Elementdaten bereit.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                ### Wofür verwenden Sie dieses Prozesselement?

                Lassen Sie ein KI-Modell eine Anfrage beantworten oder einen Text bearbeiten, zum Beispiel eine Zusammenfassung erstellen oder einen Text umformulieren. Die Textantwort steht für nachfolgende Schritte bereit.

                ### Was richten Sie ein?

                Geben Sie die API-Basis-URL und die genaue Modellkennung Ihres KI-Dienstes an. Wählen Sie ein in Prosuna gespeichertes Geheimnis mit dem zugehörigen API-Schlüssel aus. Der Dienst muss eine OpenAI-kompatible Chat-Completions-API mit den von Prosuna verwendeten Anfrageparametern unterstützen.

                Formulieren Sie unter „Anfrage an die KI“ die gewünschte Aufgabe. Dieser Anfragetext wird auch Prompt genannt. Ergänzen Sie benötigte Angaben über Variablen im Text. Sie können die gesamte Anfrage und die Modellkennung auch aus einer Variablen, mit No-Code oder mit Low-Code bestimmen.

                ### Was passiert bei der Ausführung?

                Prosuna wertet die Eingaben aus und sendet den vorbereiteten Anfragetext an den KI-Dienst. Weitere Vorgangsdaten werden diesem Aufruf nicht automatisch beigefügt.

                Sobald eine nicht leere Textantwort vorliegt, wird der Vorgang über „KI-Antwort erhalten“ fortgesetzt. Meldet die Schnittstelle einen HTTP-Fehler oder kann ihre Antwort nicht verarbeitet werden, schlägt die Aufgabe fehl.

                ### Welches Ergebnis steht danach bereit?

                Der gesendete Anfragetext und die KI-Antwort stehen als Elementdaten bereit. Liefert der Dienst zusätzliche Angaben, werden auch die gemeldete Modellkennung, der Abschlussgrund und der Token-Verbrauch gespeichert. Tokens sind Texteinheiten, mit denen der KI-Dienst die verarbeitete Textmenge zählt.

                In nachfolgenden Schritten können Sie die KI-Antwort über eine Variable verwenden. Über die Datenzuordnung unter „Ausgangsdaten“ können Sie einzelne Ergebnisse zusätzlich in die Vorgangsdaten übernehmen. Ohne eine solche Zuordnung verändert dieses Element die Vorgangsdaten nicht.

                ### Wie sieht ein Beispiel aus?

                Sie möchten den Text aus `$.beschreibung` zusammenfassen. Tragen Sie als Anfrage ein: „Fassen Sie den folgenden Text in drei Sätzen zusammen: {{ $.beschreibung }}“.

                Hat das Prozesselement den Datenschlüssel `zusammenfassung`, steht die empfangene Antwort anschließend unter `_.zusammenfassung.completion` bereit. Ein nachfolgender Schritt kann diese Antwort beispielsweise zur Prüfung anzeigen.

                ### Was ist zu beachten?

                Das Element prüft die Antwort weder auf fachliche Richtigkeit noch auf Vollständigkeit. Planen Sie bei Bedarf einen anschließenden Prüfungsschritt ein. Die Antwortlänge ist durch die Systemeinstellung und die Grenzen des gewählten Modells begrenzt.
                """;
    }

    @Nonnull
    @Override
    public Class<AiCompletionActionNodeConfig> getNodeConfigurationClass() {
        return AiCompletionActionNodeConfig.class;
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        try {
            return ElementPOJOMapper.createFromPOJO(AiCompletionActionNodeConfig.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerErrorWithDetails(
                    e,
                    "Die Konfiguration des Prozesselements konnte nicht geladen werden.",
                    null
            );
        }
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts(@Nonnull AiCompletionActionNodeConfig configuration) {
        return List.of(
                new ProcessNodePort(
                        SUCCESS_PORT,
                        "KI-Antwort erhalten",
                        "Der Vorgang wird hier fortgesetzt, nachdem eine KI-Antwort als Text empfangen und gespeichert wurde."
                )
        );
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(OUTPUT_PROMPT, "Gesendeter Anfragetext", "Der nach Auswertung der Eingaben an die KI gesendete Text.", "string"),
                new ProcessNodeOutput(OUTPUT_COMPLETION, "KI-Antwort", "Die erste nicht leere Textantwort des KI-Dienstes.", "string"),
                new ProcessNodeOutput(OUTPUT_FINISH_REASON, "Abschlussgrund", "Der vom KI-Dienst gemeldete Grund für das Ende der ersten Antwortoption. Diese Angabe ist keine fachliche Bewertung der Antwort.", "string | null"),
                new ProcessNodeOutput(OUTPUT_RESPONSE_MODEL, "Verwendetes Modell", "Die vom KI-Dienst für diese Antwort gemeldete Modellkennung.", "string | null"),
                new ProcessNodeOutput(OUTPUT_USAGE, "Token-Verbrauch", "Die vom KI-Dienst gemeldeten Token-Anzahlen für Anfrage, Antwort und Gesamtverbrauch.", OUTPUT_USAGE_TYPE_DEFINITION)
        );
    }

    @Nullable
    @Override
    public Map<String, List<String>> validateConfiguration(
            @Nonnull ProcessNodeConfigurationValidationContext<AiCompletionActionNodeConfig> context
    ) throws ResponseException {
        var configuration = context.configuration();
        var errors = new LinkedHashMap<String, List<String>>();

        if (!context.isDeferred(AiCompletionActionNodeConfig.ENDPOINT_URL_FIELD_ID)) {
            if (StringUtils.isNullOrEmpty(configuration.endpointUrl)) {
                errors.put(AiCompletionActionNodeConfig.ENDPOINT_URL_FIELD_ID, List.of("Geben Sie die API-Basis-URL an."));
            } else {
                try {
                    parseEndpointUri(configuration.endpointUrl);
                } catch (ProcessNodeExecutionExceptionInvalidConfiguration e) {
                    errors.put(AiCompletionActionNodeConfig.ENDPOINT_URL_FIELD_ID, List.of(e.getMessage()));
                }
            }
        }

        if (!context.isDeferred(AiCompletionActionNodeConfig.API_KEY_SECRET_FIELD_ID)) {
            if (StringUtils.isNullOrEmpty(configuration.apiKeySecret)) {
                errors.put(AiCompletionActionNodeConfig.API_KEY_SECRET_FIELD_ID, List.of("Wählen Sie ein gespeichertes Geheimnis mit dem API-Schlüssel aus."));
            } else {
                try {
                    var secretId = UUID.fromString(configuration.apiKeySecret.trim());
                    if (secretService.retrieve(secretId).isEmpty()) {
                        errors.put(AiCompletionActionNodeConfig.API_KEY_SECRET_FIELD_ID, List.of("Das ausgewählte Geheimnis für den API-Schlüssel wurde nicht gefunden."));
                    }
                } catch (IllegalArgumentException e) {
                    errors.put(AiCompletionActionNodeConfig.API_KEY_SECRET_FIELD_ID, List.of("Das ausgewählte Geheimnis für den API-Schlüssel ist ungültig."));
                }
            }
        }

        if (!context.isDeferred(AiCompletionActionNodeConfig.MODEL_FIELD_ID) && StringUtils.isNullOrEmpty(configuration.model)) {
            errors.put(AiCompletionActionNodeConfig.MODEL_FIELD_ID, List.of("Geben Sie eine Modellkennung an."));
        }

        if (!context.isDeferred(AiCompletionActionNodeConfig.PROMPT_FIELD_ID)) {
            if (StringUtils.isNullOrEmpty(configuration.prompt)) {
                errors.put(AiCompletionActionNodeConfig.PROMPT_FIELD_ID, List.of("Geben Sie eine Anfrage an die KI ein."));
            }
        }

        return errors.isEmpty() ? null : errors;
    }

    @Nonnull
    @Override
    public AuthoredElementValues cleanConfigurationForExport(@Nonnull AuthoredElementValues configuration) {
        configuration.remove(AiCompletionActionNodeConfig.API_KEY_SECRET_FIELD_ID);
        return configuration;
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<AiCompletionActionNodeConfig> context) throws ProcessNodeExecutionException {
        var configuration = context.getConfigurationOfExecutingNode();

        if (StringUtils.isNullOrEmpty(configuration.endpointUrl)) {
            throw new ProcessNodeExecutionExceptionMissingValue("Für die KI-Anfrage fehlt die API-Basis-URL.");
        }
        if (StringUtils.isNullOrEmpty(configuration.apiKeySecret)) {
            throw new ProcessNodeExecutionExceptionMissingValue("Für die KI-Anfrage wurde kein Geheimnis mit dem API-Schlüssel ausgewählt.");
        }
        if (StringUtils.isNullOrEmpty(configuration.model)) {
            throw new ProcessNodeExecutionExceptionMissingValue("Für die KI-Anfrage fehlt die Modellkennung.");
        }
        if (StringUtils.isNullOrEmpty(configuration.prompt)) {
            throw new ProcessNodeExecutionExceptionMissingValue("Die Anfrage an die KI ist leer. Prüfen Sie die Eingabe oder die verwendete Variable beziehungsweise Regel.");
        }

        var endpointUri = parseEndpointUri(configuration.endpointUrl);
        var apiKey = resolveApiKey(configuration.apiKeySecret);
        var renderedPrompt = configuration.prompt;

        if (StringUtils.isNullOrEmpty(renderedPrompt)) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Die Anfrage an die KI ist leer. Prüfen Sie die Eingabe oder die verwendete Variable beziehungsweise Regel."
            );
        }

        var requestBodyJson = serializeRequestBody(createRequestBody(configuration.model, renderedPrompt));
        var headers = HttpServiceHeaders.create()
                .withContentType(HttpServiceHeaders.APPLICATION_JSON)
                .withAccept(HttpServiceHeaders.APPLICATION_JSON)
                .withAuthorizationBearer(apiKey);

        ResponseEntity<byte[]> response;
        try {
            response = httpService.request(HttpMethod.POST, endpointUri, requestBodyJson, headers);
        } catch (HttpConnectionException e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Bei der Kommunikation mit dem KI-Dienst ist ein Fehler aufgetreten."
            );
        }

        var statusCode = response.getStatusCode().value();
        var rawBody = decodeResponseBody(response);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    "Der KI-Dienst hat mit dem HTTP-Statuscode %d geantwortet. Die Anfrage konnte nicht abgeschlossen werden.",
                    statusCode
            );
        }

        AiCompletionResponse completionResponse;
        try {
            completionResponse = parseResponse(rawBody);
        } catch (Exception e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Die Antwort des KI-Dienstes konnte nicht verarbeitet werden."
            );
        }

        var completionTexts = extractCompletionTexts(completionResponse.choices);
        if (completionTexts.isEmpty()) {
            throw new ProcessNodeExecutionExceptionUnknown("Der KI-Dienst hat keinen Antworttext geliefert.");
        }

        var nodeData = createNodeData(renderedPrompt, completionTexts, completionResponse);

        return ProcessNodeExecutionResultTaskCompleted.of(SUCCESS_PORT)
                .setNodeData(nodeData);
    }

    @Nonnull
    private URI parseEndpointUri(@Nullable String endpointUrl) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var normalizedUrl = StringUtils.toNullableTrimmedString(endpointUrl);
        if (normalizedUrl == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Für die KI-Anfrage fehlt die API-Basis-URL.");
        }

        try {
            var uri = UriComponentsBuilder
                    .fromUriString(normalizedUrl)
                    .path(apiChatCompletionsPathSuffix)
                    .build(true)
                    .toUri();

            if (!uri.isAbsolute() || StringUtils.isNullOrEmpty(uri.getScheme()) || StringUtils.isNullOrEmpty(uri.getHost())) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration("Die API-Basis-URL muss eine vollständige URL mit Hostnamen sein.");
            }

            return uri;
        } catch (ProcessNodeExecutionExceptionInvalidConfiguration e) {
            throw e;
        } catch (Exception e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Die API-Basis-URL des KI-Dienstes ist ungültig."
            );
        }
    }

    @Nonnull
    private String resolveApiKey(@Nullable String apiKeySecretKey) throws ProcessNodeExecutionException {
        var normalizedSecretKey = StringUtils.toNullableTrimmedString(apiKeySecretKey);
        if (normalizedSecretKey == null) {
            throw new ProcessNodeExecutionExceptionMissingValue("Für die KI-Anfrage wurde kein Geheimnis mit dem API-Schlüssel ausgewählt.");
        }

        UUID secretId;
        try {
            secretId = UUID.fromString(normalizedSecretKey);
        } catch (IllegalArgumentException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(e, "Das ausgewählte Geheimnis für den API-Schlüssel ist ungültig.");
        }

        SecretEntity secret = secretService
                .retrieve(secretId)
                .orElseThrow(() -> new ProcessNodeExecutionExceptionInvalidConfiguration("Das ausgewählte Geheimnis für den API-Schlüssel wurde nicht gefunden."));

        try {
            var decryptedSecret = secretService.decrypt(secret);
            if (StringUtils.isNullOrEmpty(decryptedSecret)) {
                throw new ProcessNodeExecutionExceptionMissingValue("Das ausgewählte Geheimnis für den API-Schlüssel ist leer.");
            }
            return decryptedSecret;
        } catch (ProcessNodeExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Der API-Schlüssel konnte nicht aus dem ausgewählten Geheimnis gelesen werden."
            );
        }
    }

    @Nonnull
    private Map<String, Object> createRequestBody(@Nonnull String model,
                                                  @Nonnull String renderedPrompt) {
        var body = new LinkedHashMap<String, Object>();
        body.put("model", model.trim());
        body.put("messages", List.of(Map.of(
                "role", "user",
                "content", renderedPrompt
        )));
        body.put("temperature", DEFAULT_TEMPERATURE);
        body.put("top_p", DEFAULT_TOP_P);
        body.put("n", DEFAULT_N);
        body.put("stream", DEFAULT_STREAM);
        body.put("max_tokens", aiPluginProperties.getCompletionMaxTokens());
        return body;
    }

    @Nonnull
    private String serializeRequestBody(@Nonnull Map<String, Object> requestBody) throws ProcessNodeExecutionExceptionUnknown {
        try {
            return JsonMapperFactory.getInstance().writeValueAsString(requestBody);
        } catch (Exception e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Die Anfrage an den KI-Dienst konnte nicht vorbereitet werden."
            );
        }
    }

    @Nonnull
    private AiCompletionResponse parseResponse(@Nullable String rawBody) throws Exception {
        if (StringUtils.isNullOrEmpty(rawBody)) {
            throw new IllegalArgumentException("Die API-Antwort ist leer.");
        }
        return JsonMapperFactory
                .getInstance()
                .readValue(rawBody, AiCompletionResponse.class);
    }

    @Nullable
    private String decodeResponseBody(@Nonnull ResponseEntity<byte[]> response) {
        var body = response.getBody();
        if (body == null) {
            return null;
        }

        Charset charset = StandardCharsets.UTF_8;
        var mediaType = response.getHeaders().getContentType();
        if (mediaType != null && mediaType.getCharset() != null) {
            charset = mediaType.getCharset();
        }

        return new String(body, charset);
    }

    @Nonnull
    private Map<String, Object> createNodeData(@Nonnull String renderedPrompt,
                                               @Nonnull List<String> completionTexts,
                                               @Nonnull AiCompletionResponse response) {
        var nodeData = new LinkedHashMap<String, Object>();
        var usage = response.usage != null
                ? JsonMapperFactory.getInstance().convertValue(response.usage, Map.class)
                : null;

        nodeData.put(OUTPUT_PROMPT, renderedPrompt);
        nodeData.put(OUTPUT_COMPLETION, completionTexts.getFirst());
        nodeData.put(OUTPUT_FINISH_REASON, response.choices != null && !response.choices.isEmpty() ? response.choices.get(0).finishReason : null);
        nodeData.put(OUTPUT_RESPONSE_MODEL, response.model);
        nodeData.put(OUTPUT_USAGE, usage);
        return nodeData;
    }

    @Nonnull
    private List<String> extractCompletionTexts(@Nullable List<AiCompletionChoice> choices) {
        if (choices == null) {
            return List.of();
        }

        return choices.stream()
                .map(choice -> choice.message != null ? choice.message.content : null)
                .filter(StringUtils::isNotNullOrEmpty)
                .toList();
    }

    /**
     * Configuration of the AI completion node.
     */
    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class AiCompletionActionNodeConfig {
        public static final String ENDPOINT_URL_FIELD_ID = "endpointUrl";
        public static final String API_KEY_SECRET_FIELD_ID = "apiKeySecret";
        public static final String MODEL_FIELD_ID = "model";
        public static final String PROMPT_FIELD_ID = "prompt";

        /**
         * Absolute API base URL. The chat completions path is appended without rendering the URL as a template.
         */
        @InputElementPOJOBinding(id = ENDPOINT_URL_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "API-Basis-URL"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Geben Sie die Basis-URL einer OpenAI-kompatiblen Chat-Completions-API an, zum Beispiel https://ki.example/v1. Der Pfad /chat/completions wird automatisch ergänzt."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0)
        })
        public String endpointUrl;

        /**
         * Reference to a stored secret that contains the bearer token for the AI request. The selected secret is decrypted only during execution.
         */
        @InputElementPOJOBinding(id = API_KEY_SECRET_FIELD_ID, type = ElementType.SecretSelectInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "API-Schlüssel"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Wählen Sie ein in Prosuna gespeichertes Geheimnis aus, das den API-Schlüssel dieses KI-Dienstes enthält."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0)
        })
        public String apiKeySecret;

        /**
         * Identifier of the AI model that should generate the completion.
         */
        @InputElementPOJOBinding(id = MODEL_FIELD_ID, type = ElementType.Text,
                allowedInputModes = {InputMode.Literal, InputMode.Variable, InputMode.NoCode, InputMode.LowCode}, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Modellkennung"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Geben Sie die genaue Kennung eines Modells an, das über die gewählte API verfügbar ist."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0)
        })
        public String model;

        /**
         * Template-based prompt text that is rendered against the current process data before the API request is sent.
         */
        @InputElementPOJOBinding(id = PROMPT_FIELD_ID, type = ElementType.RichTextInput,
                dynamicText = true,
                // The prompt goes to a language model, not to a Markdown renderer.
                dynamicTextFormat = TemplateOutputFormat.PlainText,
                allowedInputModes = {InputMode.Literal, InputMode.Variable, InputMode.NoCode, InputMode.LowCode}, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Anfrage an die KI"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Beschreiben Sie, was die KI bearbeiten soll. Dieser Anfragetext wird auch Prompt genannt. Binden Sie benötigte Angaben aus dem Vorgang über Variablen ein."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String prompt;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AiCompletionResponse {
        public String id;
        public List<AiCompletionChoice> choices;
        public Long created;
        public String object;
        public String model;
        public AiCompletionUsage usage;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AiCompletionChoice {
        @Nullable
        @JsonProperty("finish_reason")
        public String finishReason;
        @Nullable
        public Integer index;
        @Nullable
        public AiCompletionMessage message;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AiCompletionMessage {
        @Nullable
        public String role;
        @Nullable
        public String content;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AiCompletionUsage {
        @Nullable
        @JsonProperty("prompt_tokens")
        public Integer promptTokens;
        @Nullable
        @JsonProperty("completion_tokens")
        public Integer completionTokens;
        @Nullable
        @JsonProperty("total_tokens")
        public Integer totalTokens;
    }
}
