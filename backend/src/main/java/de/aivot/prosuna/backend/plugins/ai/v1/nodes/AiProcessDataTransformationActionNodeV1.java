package de.aivot.prosuna.backend.plugins.ai.v1.nodes;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionMissingValue;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodeOutput;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeConfigurationValidationContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Sends the full process execution data to the centrally configured AI model and replaces the process data root with the returned JSON object.
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
    private static final String OUTPUT_USAGE_TYPE_DEFINITION =
            "{ prompt_tokens: number | null; completion_tokens: number | null; total_tokens: number | null; } | null";

    private static final String LEGACY_ENDPOINT_URL_FIELD_ID = "endpointUrl";
    private static final String LEGACY_API_KEY_SECRET_FIELD_ID = "apiKeySecret";
    private static final String LEGACY_MODEL_FIELD_ID = "model";

    private static final double DEFAULT_TEMPERATURE = 0.01d;
    private static final double DEFAULT_TOP_P = 0.9d;
    private static final int DEFAULT_N = 1;

    private static final Pattern JSON_CODE_FENCE_PATTERN = Pattern.compile("^```(?:json)?\\s*(.*?)\\s*```$", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private static final String SYSTEM_PROMPT = """
            You transform Prosuna process data.
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

    private final ChatClient chatClient;

    public AiProcessDataTransformationActionNodeV1(ChatClient.Builder chatClientBuilder,
                                                   AiPluginProperties aiPluginProperties,
                                                   @Value("${spring.ai.openai.chat.timeout}") Duration chatTimeout) {
        chatClient = chatClientBuilder
                .defaultOptions(OpenAiChatOptions.builder()
                        .temperature(DEFAULT_TEMPERATURE)
                        .topP(DEFAULT_TOP_P)
                        .n(DEFAULT_N)
                        .maxTokens(aiPluginProperties.getProcessDataTransformationMaxTokens())
                        .timeout(chatTimeout))
                .build();
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
        return "Vorgangsdaten mit KI transformieren";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Sendet die vollständigen Laufzeitdaten eines Vorgangs an das zentral konfigurierte KI-Modell und ersetzt die Vorgangsdaten durch das zurückgegebene JSON-Objekt.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                Verwendet das zentral für Prosuna eingerichtete KI-Modell, um die vollständigen Laufzeitdaten eines Vorgangs in ein neues JSON-Objekt zu transformieren.

                Der Prompt wird in der Elementkonfiguration festgelegt und kann die aktuellen Vorgangsdaten einbeziehen. Das von der KI zurückgegebene JSON-Objekt ersetzt anschließend die bisherigen Vorgangsdaten vollständig. Informationen zu Modell, Abschlussgrund, Nutzung und erzeugten Top-Level-Schlüsseln stehen als Ausgänge zur Verfügung.
                """;
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
        try {
            return ElementPOJOMapper.createFromPOJO(AiProcessDataTransformationActionNodeConfig.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(
                    e,
                    "Fehler bei der Erstellung des Konfigurationslayouts: %s",
                    e.getMessage()
            );
        }
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
                new ProcessNodeOutput(OUTPUT_PROMPT, "Eingabe", "Der gerenderte Anfragetext für das KI-Modell.", "string"),
                new ProcessNodeOutput(OUTPUT_FINISH_REASON, "Finish Reason", "Der Abschlussgrund der ersten Choice.", "string | null"),
                new ProcessNodeOutput(OUTPUT_RESPONSE_MODEL, "Antwort-Modell", "Das Modell, das die Antwort erzeugt hat.", "string | null"),
                new ProcessNodeOutput(OUTPUT_USAGE, "Nutzung", "Die Token-Nutzungsinformationen der KI-Antwort.", OUTPUT_USAGE_TYPE_DEFINITION),
                new ProcessNodeOutput(OUTPUT_TOP_LEVEL_KEYS, "Top-Level-Schlüssel", "Die obersten Schlüssel des neu erzeugten Prozessdatenobjekts.", "Array<string>")
        );
    }

    @Nullable
    @Override
    public Map<String, List<String>> validateConfiguration(
            @Nonnull ProcessNodeConfigurationValidationContext<AiProcessDataTransformationActionNodeConfig> context
    ) throws ResponseException {
        var configuration = context.configuration();
        if (!context.isDeferred(AiProcessDataTransformationActionNodeConfig.PROMPT_FIELD_ID)
                && StringUtils.isNullOrEmpty(configuration.prompt)) {
            return Map.of(
                    AiProcessDataTransformationActionNodeConfig.PROMPT_FIELD_ID,
                    List.of("Das Prompt muss angegeben werden.")
            );
        }
        return null;
    }

    @Nonnull
    @Override
    public AuthoredElementValues cleanConfigurationForExport(@Nonnull AuthoredElementValues configuration) {
        configuration.remove(LEGACY_ENDPOINT_URL_FIELD_ID);
        configuration.remove(LEGACY_API_KEY_SECRET_FIELD_ID);
        configuration.remove(LEGACY_MODEL_FIELD_ID);
        return configuration;
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<AiProcessDataTransformationActionNodeConfig> context) throws ProcessNodeExecutionException {
        var configuration = context.getConfigurationOfExecutingNode();
        var renderedPrompt = configuration.prompt;
        if (StringUtils.isNullOrEmpty(renderedPrompt)) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Das Prompt ist nach dem Rendern leer. Bitte überprüfen Sie die Vorlage und die Vorgangsdaten."
            );
        }

        var serializedExecutionData = serializeExecutionData(context.getCurrentProcessExecutionData());
        ChatResponse response;
        try {
            response = chatClient
                    .prompt()
                    .system(SYSTEM_PROMPT)
                    .user(createUserMessage(renderedPrompt, serializedExecutionData))
                    .call()
                    .chatResponse();
        } catch (RuntimeException e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Die KI-Anfrage konnte nicht ausgeführt werden."
            );
        }

        if (response == null) {
            throw new ProcessNodeExecutionExceptionUnknown("Die KI hat keinen Antworttext zurückgegeben.");
        }

        var completionText = extractCompletionText(response);
        var transformedProcessData = parseTransformedProcessData(completionText);
        var nodeData = createNodeData(renderedPrompt, transformedProcessData, response);

        return ProcessNodeExecutionResultTaskCompleted.of(SUCCESS_PORT)
                .setProcessData(transformedProcessData)
                .setNodeData(nodeData);
    }

    @Nonnull
    private String serializeExecutionData(@Nonnull ProcessExecutionData processExecutionData) throws ProcessNodeExecutionExceptionUnknown {
        try {
            return JsonMapperFactory
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
    private String extractCompletionText(@Nonnull ChatResponse response) throws ProcessNodeExecutionExceptionUnknown {
        var generation = response.getResult();
        var completionText = generation != null ? generation.getOutput().getText() : null;
        if (StringUtils.isNullOrEmpty(completionText)) {
            throw new ProcessNodeExecutionExceptionUnknown("Die KI hat keinen Antworttext zurückgegeben.");
        }
        return completionText;
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseTransformedProcessData(@Nonnull String completionText) throws ProcessNodeExecutionExceptionUnknown {
        var normalizedCompletion = StringUtils.toNullableTrimmedString(completionText);
        if (normalizedCompletion == null) {
            throw new ProcessNodeExecutionExceptionUnknown("Die KI hat keinen Antworttext zurückgegeben.");
        }

        var jsonPayload = unwrapJsonCodeFence(normalizedCompletion);
        try {
            JsonNode parsedNode = JsonMapperFactory
                    .getNullPreservingInstance()
                    .readTree(jsonPayload);

            if (parsedNode == null || !parsedNode.isObject()) {
                throw new IllegalArgumentException("Die KI-Antwort muss ein JSON-Objekt sein.");
            }

            return JsonMapperFactory
                    .getNullPreservingInstance()
                    .convertValue(parsedNode, Map.class);
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

    @Nonnull
    private Map<String, Object> createNodeData(@Nonnull String renderedPrompt,
                                               @Nonnull Map<String, Object> transformedProcessData,
                                               @Nonnull ChatResponse response) {
        var generation = response.getResult();
        var nodeData = new LinkedHashMap<String, Object>();
        nodeData.put(OUTPUT_PROMPT, renderedPrompt);
        nodeData.put(
                OUTPUT_FINISH_REASON,
                normalizeFinishReason(generation != null ? generation.getMetadata().getFinishReason() : null)
        );
        nodeData.put(OUTPUT_RESPONSE_MODEL, StringUtils.toNullableTrimmedString(response.getMetadata().getModel()));
        nodeData.put(OUTPUT_USAGE, createUsageData(response.getMetadata().getUsage()));
        nodeData.put(OUTPUT_TOP_LEVEL_KEYS, List.copyOf(transformedProcessData.keySet()));
        return nodeData;
    }

    @Nullable
    private String normalizeFinishReason(@Nullable String finishReason) {
        var normalized = StringUtils.toNullableTrimmedString(finishReason);
        return normalized != null ? normalized.toLowerCase(Locale.ROOT) : null;
    }

    @Nullable
    private Map<String, Object> createUsageData(@Nullable Usage usage) {
        if (usage == null || usage instanceof EmptyUsage) {
            return null;
        }

        var usageData = new LinkedHashMap<String, Object>();
        usageData.put("prompt_tokens", usage.getPromptTokens());
        usageData.put("completion_tokens", usage.getCompletionTokens());
        usageData.put("total_tokens", usage.getTotalTokens());
        return usageData;
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
        public static final String PROMPT_FIELD_ID = "prompt";

        /**
         * Template-based prompt text that is rendered against the current process execution data before the AI request is sent.
         */
        @InputElementPOJOBinding(id = PROMPT_FIELD_ID, type = ElementType.RichTextInput,
                dynamicText = true,
                allowedInputModes = {InputMode.Literal, InputMode.Variable, InputMode.NoCode, InputMode.LowCode}, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Prompt"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Prompt-Vorlage mit Template-Ausdrücken. Die Vorlage wird vor der KI-Anfrage mit den aktuellen Vorgangsdaten gerendert."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String prompt;
    }
}
