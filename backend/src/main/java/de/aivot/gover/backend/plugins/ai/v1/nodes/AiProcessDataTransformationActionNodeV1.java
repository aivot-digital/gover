package de.aivot.gover.backend.plugins.ai.v1.nodes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import de.aivot.gover.backend.core.exceptions.HttpConnectionException;
import de.aivot.gover.backend.core.models.HttpServiceHeaders;
import de.aivot.gover.backend.core.services.HttpService;
import de.aivot.gover.backend.core.services.ObjectMapperFactory;
import de.aivot.gover.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.gover.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.gover.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.gover.backend.elements.enums.OverrideFunctionType;
import de.aivot.gover.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.gover.backend.elements.models.AuthoredElementValues;
import de.aivot.gover.backend.elements.models.elements.ElementOverrideFunctions;
import de.aivot.gover.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.gover.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.gover.backend.elements.utils.ElementPOJOMapper;
import de.aivot.gover.backend.enums.ElementType;
import de.aivot.gover.backend.javascript.models.JavascriptCode;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.plugins.ai.AiPlugin;
import de.aivot.gover.backend.plugins.ai.properties.AiPluginProperties;
import de.aivot.gover.backend.process.entities.ProcessNodeEntity;
import de.aivot.gover.backend.process.enums.ProcessNodeType;
import de.aivot.gover.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.gover.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.gover.backend.process.exceptions.ProcessNodeExecutionExceptionMissingValue;
import de.aivot.gover.backend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.gover.backend.process.models.*;
import de.aivot.gover.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.gover.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.gover.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.gover.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.gover.backend.process.services.TemplateRenderService;
import de.aivot.gover.backend.secrets.entities.SecretEntity;
import de.aivot.gover.backend.secrets.repositories.SecretRepository;
import de.aivot.gover.backend.secrets.services.SecretService;
import de.aivot.gover.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Sends the full process execution data to an AI model and replaces the process data root with the returned JSON object.
 */
@Component
public class AiProcessDataTransformationActionNodeV1 implements ProcessNodeDefinition<AiProcessDataTransformationActionNodeV1.AiProcessDataTransformationActionNodeConfig> {
    public static final String NODE_KEY = "ai_process_data_transformation";

    private static final String SUCCESS_PORT = "success";

    private static final String OUTPUT_PROMPT = "prompt";
    private static final String OUTPUT_FINISH_REASON = "finishReason";
    private static final String OUTPUT_RESPONSE_MODEL = "responseModel";
    private static final String OUTPUT_USAGE = "usage";
    private static final String OUTPUT_TOP_LEVEL_KEYS = "topLevelKeys";

    private static final double DEFAULT_TEMPERATURE = 0.01d;
    private static final double DEFAULT_TOP_P = 0.9d;
    private static final int DEFAULT_N = 1;
    private static final boolean DEFAULT_STREAM = false;

    private static final String API_MODELS_PATH_SUFFIX = "/models";
    private static final String API_CHAT_COMPLETIONS_PATH_SUFFIX = "/chat/completions";

    private static final Pattern JSON_CODE_FENCE_PATTERN = Pattern.compile("^```(?:json)?\\s*(.*?)\\s*```$", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private static final String SYSTEM_PROMPT = """
            You transform Gover process data.
            You will receive a rendered task prompt and the full ProcessExecutionData JSON with the roots "$", "$$", and "_".
            Return exactly one valid JSON object that will become the new value of "$".
            Rules:
            - The top-level value must be a JSON object.
            - Return JSON only.
            - Do not use markdown code fences.
            - Do not add explanations or comments.
            - Do not wrap the result inside "$", "$$", "_" or any other envelope.
            - If no change is required, return the unchanged "$" object.
            """;

    private final HttpService httpService;
    private final TemplateRenderService templateRenderService;
    private final SecretRepository secretRepository;
    private final SecretService secretService;
    private final AiPluginProperties aiPluginProperties;

    public AiProcessDataTransformationActionNodeV1(HttpService httpService,
                                                   TemplateRenderService templateRenderService,
                                                   SecretRepository secretRepository,
                                                   SecretService secretService,
                                                   AiPluginProperties aiPluginProperties) {
        this.httpService = httpService;
        this.templateRenderService = templateRenderService;
        this.secretRepository = secretRepository;
        this.secretService = secretService;
        this.aiPluginProperties = aiPluginProperties;
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
    public String getName() {
        return "Vorgangsdaten mit KI transformieren";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Sendet die vollständigen Laufzeitdaten eines Vorgangs an eine KI und ersetzt die Vorgangsdaten durch das zurückgegebene JSON-Objekt.";
    }

    @Nonnull
    @Override
    public Class<AiProcessDataTransformationActionNodeConfig> getNodeConfigurationClass() {
        return AiProcessDataTransformationActionNodeConfig.class;
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        ConfigLayoutElement layout;
        try {
            layout = ElementPOJOMapper.createFromPOJO(AiProcessDataTransformationActionNodeConfig.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(
                    e,
                    "Fehler bei der Erstellung des Konfigurationslayouts: %s",
                    e.getMessage()
            );
        }

        layout.findChild(AiProcessDataTransformationActionNodeConfig.API_KEY_SECRET_FIELD_ID, SelectInputElement.class)
                .ifPresent(field -> field.setOptions(secretRepository
                        .findAll()
                        .stream()
                        .map(secret -> SelectInputElementOption.of(secret.getKey().toString(), secret.getName()))
                        .toList()));

        var modelSelectOverride = new ElementOverrideFunctions();
        modelSelectOverride.setType(OverrideFunctionType.Javascript);
        modelSelectOverride.setJavascriptCode(JavascriptCode.of("""
                        (function() {
                            const endpointUrl = ctx.effectiveValues.%s;
                            if (endpointUrl == null) {
                                return element;
                            }
                        
                            const secretKey = ctx.effectiveValues.%s;
                            if (secretKey == null) {
                                return element;
                            }
                        
                            const apiToken = _secrets_v1.get(secretKey);
                        
                            const fullUrl = endpointUrl + '%s';
                        
                            const response = _http_v1.get(fullUrl, {
                                Authorization: 'Bearer ' + apiToken,
                            });
                        
                            const availableModels = JSON.parse(response.body);
                            const options = availableModels.data.map(d => ({
                                label: d.id,
                                value: d.id,
                            }));
                        
                            return {
                                ...element,
                                options: options,
                            };
                        })()
                        """,
                AiProcessDataTransformationActionNodeConfig.ENDPOINT_URL_FIELD_ID,
                AiProcessDataTransformationActionNodeConfig.API_KEY_SECRET_FIELD_ID,
                API_MODELS_PATH_SUFFIX
        ));
        modelSelectOverride.setReferencedIds(List.of(
                AiProcessDataTransformationActionNodeConfig.ENDPOINT_URL_FIELD_ID,
                AiProcessDataTransformationActionNodeConfig.API_KEY_SECRET_FIELD_ID
        ));

        layout.findChild(AiProcessDataTransformationActionNodeConfig.MODEL_FIELD_ID, SelectInputElement.class)
                .ifPresent(field -> field.setOverride(modelSelectOverride));

        return layout;
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of(
                new ProcessNodePort(
                        SUCCESS_PORT,
                        "Vorgangsdaten transformiert",
                        "Der Prozess wird hier fortgesetzt, nachdem die Vorgangsdaten erfolgreich transformiert wurden."
                )
        );
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(OUTPUT_PROMPT, "Eingabe", "Der gerenderte Anfragetext für das KI-Modell."),
                new ProcessNodeOutput(OUTPUT_FINISH_REASON, "Finish Reason", "Der Abschlussgrund der ersten Choice."),
                new ProcessNodeOutput(OUTPUT_RESPONSE_MODEL, "Antwort-Modell", "Das Modell, das die Antwort erzeugt hat."),
                new ProcessNodeOutput(OUTPUT_USAGE, "Nutzung", "Die Token-Nutzungsinformationen der API-Antwort."),
                new ProcessNodeOutput(OUTPUT_TOP_LEVEL_KEYS, "Top-Level-Schlüssel", "Die obersten Schlüssel des neu erzeugten Prozessdatenobjekts.")
        );
    }

    @Nonnull
    @Override
    public ProcessNodeDefinitionMetadata getMetadata(@Nonnull ProcessNodeEntity processNodeEntity,
                                                     @Nonnull AiProcessDataTransformationActionNodeConfig configuration,
                                                     @Nonnull ProcessNodeDefinitionMetadata previousMetadata) {
        // Reset the forwarded data keys because this node might completely clear the process data layer
        return new ProcessNodeDefinitionMetadata(
                new LinkedList<>(previousMetadata.reusableUiDefinitions()),
                new LinkedList<>(previousMetadata.forwardedAttachments()),
                new LinkedList<>(),
                new LinkedList<>(previousMetadata.forwardedIdentities())
        );
    }

    @Nullable
    @Override
    public Map<String, List<String>> validateConfiguration(@Nonnull ProcessNodeEntity processNodeEntity,
                                                           @Nonnull AiProcessDataTransformationActionNodeConfig configuration) throws ResponseException {
        var errors = new LinkedHashMap<String, List<String>>();

        if (StringUtils.isNullOrEmpty(configuration.endpointUrl)) {
            errors.put(AiProcessDataTransformationActionNodeConfig.ENDPOINT_URL_FIELD_ID, List.of("Die Endpoint-URL muss angegeben werden."));
        } else {
            try {
                parseEndpointUri(configuration.endpointUrl);
            } catch (ProcessNodeExecutionExceptionInvalidConfiguration e) {
                errors.put(AiProcessDataTransformationActionNodeConfig.ENDPOINT_URL_FIELD_ID, List.of(e.getMessage()));
            }
        }

        if (StringUtils.isNullOrEmpty(configuration.apiKeySecret)) {
            errors.put(AiProcessDataTransformationActionNodeConfig.API_KEY_SECRET_FIELD_ID, List.of("Das Secret für den API-Schlüssel muss ausgewählt werden."));
        } else {
            try {
                var secretId = UUID.fromString(configuration.apiKeySecret.trim());
                if (secretService.retrieve(secretId).isEmpty()) {
                    errors.put(AiProcessDataTransformationActionNodeConfig.API_KEY_SECRET_FIELD_ID, List.of("Das ausgewählte Secret für den API-Schlüssel wurde nicht gefunden."));
                }
            } catch (IllegalArgumentException e) {
                errors.put(AiProcessDataTransformationActionNodeConfig.API_KEY_SECRET_FIELD_ID, List.of("Das ausgewählte Secret für den API-Schlüssel ist ungültig."));
            }
        }

        if (StringUtils.isNullOrEmpty(configuration.model)) {
            errors.put(AiProcessDataTransformationActionNodeConfig.MODEL_FIELD_ID, List.of("Das Modell muss angegeben werden."));
        }

        if (StringUtils.isNullOrEmpty(configuration.prompt)) {
            errors.put(AiProcessDataTransformationActionNodeConfig.PROMPT_FIELD_ID, List.of("Das Prompt muss angegeben werden."));
        } else {
            var diagnostics = templateRenderService.validateInterpolationSyntax(configuration.prompt);
            if (!diagnostics.isEmpty()) {
                errors.put(
                        AiProcessDataTransformationActionNodeConfig.PROMPT_FIELD_ID,
                        diagnostics.stream()
                                .map(diagnostic -> "Zeile %d: %s".formatted(diagnostic.lineNumber(), diagnostic.message()))
                                .toList()
                );
            }
        }

        return errors.isEmpty() ? null : errors;
    }

    @Nonnull
    @Override
    public AuthoredElementValues cleanConfigurationForExport(@Nonnull AuthoredElementValues configuration) {
        configuration.remove(AiProcessDataTransformationActionNodeConfig.API_KEY_SECRET_FIELD_ID);
        return configuration;
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<AiProcessDataTransformationActionNodeConfig> context) throws ProcessNodeExecutionException {
        var configuration = context.getConfigurationOfExecutingNode();

        if (StringUtils.isNullOrEmpty(configuration.endpointUrl)) {
            throw new ProcessNodeExecutionExceptionMissingValue("Die Endpoint-URL für den KI-Aufruf wurde nicht angegeben.");
        }
        if (StringUtils.isNullOrEmpty(configuration.apiKeySecret)) {
            throw new ProcessNodeExecutionExceptionMissingValue("Das Secret für den API-Schlüssel wurde nicht angegeben.");
        }
        if (StringUtils.isNullOrEmpty(configuration.model)) {
            throw new ProcessNodeExecutionExceptionMissingValue("Das Modell für den KI-Aufruf wurde nicht angegeben.");
        }
        if (StringUtils.isNullOrEmpty(configuration.prompt)) {
            throw new ProcessNodeExecutionExceptionMissingValue("Das Prompt für den KI-Aufruf wurde nicht angegeben.");
        }

        var endpointUri = parseEndpointUri(configuration.endpointUrl);
        var apiKey = resolveApiKey(configuration.apiKeySecret);
        var renderedPrompt = renderPrompt(context, configuration.prompt);
        if (StringUtils.isNullOrEmpty(renderedPrompt)) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Das Prompt ist nach dem Rendern leer. Bitte überprüfen Sie die Vorlage und die Vorgangsdaten."
            );
        }

        var serializedExecutionData = serializeExecutionData(context.getCurrentProcessExecutionData());
        var requestBodyJson = serializeRequestBody(createRequestBody(configuration.model, renderedPrompt, serializedExecutionData));
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
                    "Die API konnte nicht erreicht werden: %s",
                    e.getMessage()
            );
        }

        var statusCode = response.getStatusCode().value();
        var rawBody = decodeResponseBody(response);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    "Die API hat mit dem HTTP-Status %d geantwortet. Antwort: %s",
                    statusCode,
                    StringUtils.quote(truncateForError(rawBody))
            );
        }

        ChatCompletionResponse completionResponse;
        try {
            completionResponse = parseResponse(rawBody);
        } catch (Exception e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Die Antwort der KI konnte nicht verarbeitet werden: %s",
                    e.getMessage()
            );
        }

        var completionTexts = extractCompletionTexts(completionResponse.choices);
        if (completionTexts.isEmpty()) {
            throw new ProcessNodeExecutionExceptionUnknown("Die Antwort der KI enthält keinen Texte.");
        }

        var transformedProcessData = parseTransformedProcessData(completionTexts.getFirst());
        var nodeData = createNodeData(renderedPrompt, transformedProcessData, completionResponse);

        return ProcessNodeExecutionResultTaskCompleted.of(SUCCESS_PORT)
                .setProcessData(transformedProcessData)
                .setNodeData(nodeData);
    }

    @Nonnull
    private URI parseEndpointUri(@Nullable String endpointUrl) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var normalizedUrl = StringUtils.toNullableTrimmedString(endpointUrl);
        if (normalizedUrl == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Die Endpoint-URL für den KI-Aufruf ist leer.");
        }

        try {
            var uri = UriComponentsBuilder
                    .fromUriString(normalizedUrl)
                    .path(API_CHAT_COMPLETIONS_PATH_SUFFIX)
                    .build(true)
                    .toUri();

            if (!uri.isAbsolute() || StringUtils.isNullOrEmpty(uri.getScheme()) || StringUtils.isNullOrEmpty(uri.getHost())) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration("Die Endpoint-URL muss eine absolute URL sein.");
            }

            return uri;
        } catch (ProcessNodeExecutionExceptionInvalidConfiguration e) {
            throw e;
        } catch (Exception e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Die Endpoint-URL ist ungültig: %s",
                    normalizedUrl
            );
        }
    }

    @Nonnull
    private String resolveApiKey(@Nullable String apiKeySecretKey) throws ProcessNodeExecutionException {
        var normalizedSecretKey = StringUtils.toNullableTrimmedString(apiKeySecretKey);
        if (normalizedSecretKey == null) {
            throw new ProcessNodeExecutionExceptionMissingValue("Das Geheimnis für den API-Schlüssel wurde nicht angegeben.");
        }

        UUID secretId;
        try {
            secretId = UUID.fromString(normalizedSecretKey);
        } catch (IllegalArgumentException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Das ausgewählte Secret für den API-Schlüssel ist ungültig.");
        }

        SecretEntity secret = secretService
                .retrieve(secretId)
                .orElseThrow(() -> new ProcessNodeExecutionExceptionInvalidConfiguration("Das ausgewählte Secret für den API-Schlüssel wurde nicht gefunden."));

        try {
            var decryptedSecret = secretService.decrypt(secret);
            if (StringUtils.isNullOrEmpty(decryptedSecret)) {
                throw new ProcessNodeExecutionExceptionMissingValue("Das ausgewählte Secret für den API-Schlüssel ist leer.");
            }
            return decryptedSecret;
        } catch (ProcessNodeExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Der API-Schlüssel konnte nicht aus dem ausgewählten Secret entschlüsselt werden: %s",
                    e.getMessage()
            );
        }
    }

    @Nonnull
    private String renderPrompt(@Nonnull ProcessNodeExecutionInitContext<AiProcessDataTransformationActionNodeConfig> context,
                                @Nonnull String promptTemplate) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        try {
            return templateRenderService.interpolate(context.getCurrentProcessExecutionData(), promptTemplate);
        } catch (RuntimeException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Das Prompt-Template konnte nicht gerendert werden: %s",
                    e.getMessage()
            );
        }
    }

    @Nonnull
    private String serializeExecutionData(@Nonnull ProcessExecutionData processExecutionData) throws ProcessNodeExecutionExceptionUnknown {
        try {
            return ObjectMapperFactory
                    .getNullPreservingInstance()
                    .writeValueAsString(processExecutionData);
        } catch (Exception e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Die Vorgangsdaten konnten nicht für den KI-Aufruf serialisiert werden: %s",
                    e.getMessage()
            );
        }
    }

    @Nonnull
    private Map<String, Object> createRequestBody(@Nonnull String model,
                                                  @Nonnull String renderedPrompt,
                                                  @Nonnull String serializedExecutionData) {
        var body = new LinkedHashMap<String, Object>();
        body.put("model", model.trim());
        body.put("messages", List.of(
                Map.of(
                        "role", "system",
                        "content", SYSTEM_PROMPT
                ),
                Map.of(
                        "role", "user",
                        "content", createUserMessage(renderedPrompt, serializedExecutionData)
                )
        ));
        body.put("temperature", DEFAULT_TEMPERATURE);
        body.put("top_p", DEFAULT_TOP_P);
        body.put("n", DEFAULT_N);
        body.put("stream", DEFAULT_STREAM);
        body.put("max_tokens", aiPluginProperties.getProcessDataTransformationMaxTokens());
        return body;
    }

    @Nonnull
    private String createUserMessage(@Nonnull String renderedPrompt,
                                     @Nonnull String serializedExecutionData) {
        return """
                Rendered prompt:
                %s
                
                Current ProcessExecutionData JSON:
                %s
                """.formatted(renderedPrompt, serializedExecutionData);
    }

    @Nonnull
    private String serializeRequestBody(@Nonnull Map<String, Object> requestBody) throws ProcessNodeExecutionExceptionUnknown {
        try {
            return ObjectMapperFactory.getInstance().writeValueAsString(requestBody);
        } catch (Exception e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Der Request-Body für den KI-Aufruf konnte nicht serialisiert werden: %s",
                    e.getMessage()
            );
        }
    }

    @Nonnull
    private ChatCompletionResponse parseResponse(@Nullable String rawBody) throws Exception {
        if (StringUtils.isNullOrEmpty(rawBody)) {
            throw new IllegalArgumentException("Die API-Antwort ist leer.");
        }
        return ObjectMapperFactory
                .getInstance()
                .readValue(rawBody, ChatCompletionResponse.class);
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseTransformedProcessData(@Nonnull String completionText) throws ProcessNodeExecutionExceptionUnknown {
        var normalizedCompletion = StringUtils.toNullableTrimmedString(completionText);
        if (normalizedCompletion == null) {
            throw new ProcessNodeExecutionExceptionUnknown("Die Antwort der KI enthält keinen Texte.");
        }

        var jsonPayload = unwrapJsonCodeFence(normalizedCompletion);
        try {
            JsonNode parsedNode = ObjectMapperFactory
                    .getNullPreservingInstance()
                    .readTree(jsonPayload);

            if (parsedNode == null || !parsedNode.isObject()) {
                throw new IllegalArgumentException("Die KI-Antwort muss ein JSON-Objekt sein.");
            }

            return ObjectMapperFactory
                    .getNullPreservingInstance()
                    .convertValue(parsedNode, Map.class);
        } catch (IllegalArgumentException e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Die KI-Antwort enthält kein gültiges JSON-Objekt. Antwort: %s",
                    StringUtils.quote(truncateForError(completionText))
            );
        } catch (Exception e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Die KI-Antwort enthält kein gültiges JSON-Objekt. Antwort: %s",
                    StringUtils.quote(truncateForError(completionText))
            );
        }
    }

    @Nonnull
    private String unwrapJsonCodeFence(@Nonnull String completionText) {
        var matcher = JSON_CODE_FENCE_PATTERN.matcher(completionText.trim());
        if (matcher.matches()) {
            return matcher.group(1).trim();
        }
        return completionText.trim();
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
                                               @Nonnull Map<String, Object> transformedProcessData,
                                               @Nonnull ChatCompletionResponse response) {
        var nodeData = new LinkedHashMap<String, Object>();
        var usage = response.usage != null
                ? ObjectMapperFactory.getInstance().convertValue(response.usage, Map.class)
                : null;

        nodeData.put(OUTPUT_PROMPT, renderedPrompt);
        nodeData.put(OUTPUT_FINISH_REASON, response.choices != null && !response.choices.isEmpty() ? response.choices.get(0).finishReason : null);
        nodeData.put(OUTPUT_RESPONSE_MODEL, response.model);
        nodeData.put(OUTPUT_USAGE, usage);
        nodeData.put(OUTPUT_TOP_LEVEL_KEYS, List.copyOf(transformedProcessData.keySet()));
        return nodeData;
    }

    @Nonnull
    private List<String> extractCompletionTexts(@Nullable List<ChatCompletionChoice> choices) {
        if (choices == null) {
            return List.of();
        }

        return choices.stream()
                .map(choice -> choice.message != null ? choice.message.content : null)
                .filter(StringUtils::isNotNullOrEmpty)
                .toList();
    }

    @Nullable
    private static String truncateForError(@Nullable String rawBody) {
        if (rawBody == null) {
            return null;
        }
        if (rawBody.length() <= 300) {
            return rawBody;
        }
        return rawBody.substring(0, 300).trim() + "...";
    }

    /**
     * Configuration of the AI process data transformation node.
     */
    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class AiProcessDataTransformationActionNodeConfig {
        public static final String ENDPOINT_URL_FIELD_ID = "endpointUrl";
        public static final String API_KEY_SECRET_FIELD_ID = "apiKeySecret";
        public static final String MODEL_FIELD_ID = "model";
        public static final String PROMPT_FIELD_ID = "prompt";

        /**
         * Absolute URL of the AI completions endpoint. The node uses this URL as-is and does not render it as a template.
         */
        @InputElementPOJOBinding(id = ENDPOINT_URL_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "HTTP-Endpoint"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Absolute URL des KI-Endpunkts für Completions."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0)
        })
        public String endpointUrl;

        /**
         * Reference to a stored secret that contains the bearer token for the AI request. The selected secret is decrypted only during execution.
         */
        @InputElementPOJOBinding(id = API_KEY_SECRET_FIELD_ID, type = ElementType.Select, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "API-Schlüssel"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Wählen Sie ein hinterlegtes Geheimnis aus, das den Bearer-Token für die KI enthält."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0)
        })
        public String apiKeySecret;

        /**
         * Identifier of the AI model that should generate the process data transformation.
         */
        @InputElementPOJOBinding(id = MODEL_FIELD_ID, type = ElementType.Select, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Modellname"),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0)
        })
        public String model;

        /**
         * Template-based prompt text that is rendered against the current process execution data before the API request is sent.
         */
        @InputElementPOJOBinding(id = PROMPT_FIELD_ID, type = ElementType.RichTextInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Prompt"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Prompt-Vorlage mit Template-Ausdrücken. Die Vorlage wird vor dem API-Aufruf mit den aktuellen Vorgangsdaten gerendert."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String prompt;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ChatCompletionResponse {
        public String id;
        public List<ChatCompletionChoice> choices;
        public Long created;
        public String object;
        public String model;
        public ChatCompletionUsage usage;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ChatCompletionChoice {
        @Nullable
        @JsonProperty("finish_reason")
        public String finishReason;
        @Nullable
        public Integer index;
        @Nullable
        public ChatCompletionMessage message;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ChatCompletionMessage {
        @Nullable
        public String role;
        @Nullable
        public String content;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ChatCompletionUsage {
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
