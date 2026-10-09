package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionMissingValue;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodeOutput;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeConfigurationValidationContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionSummaryContext;
import de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.detail;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.section;

/**
 * Executes a prompt against the centrally configured AI model and exposes the response as node outputs.
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

    private static final String LEGACY_ENDPOINT_URL_FIELD_ID = "endpointUrl";
    private static final String LEGACY_API_KEY_SECRET_FIELD_ID = "apiKeySecret";
    private static final String LEGACY_MODEL_FIELD_ID = "model";

    private final ChatClient chatClient;

    public AiCompletionActionNodeV1(ChatClient.Builder chatClientBuilder) {
        chatClient = chatClientBuilder.build();
    }

    @Nonnull
    @Override
    public String generateExecutionSummary(@Nonnull ProcessNodeExecutionSummaryContext<AiCompletionActionNodeConfig> context) {
        var summary = new ExecutionSummaryMarkdown(context);
        return "Die KI-Anfrage wurde erfolgreich ausgeführt."
                + detail("Verwendetes Modell", summary.data(OUTPUT_RESPONSE_MODEL))
                + detail("Kurzbeschreibung", context.thisNode().getDescription())
                + section("Antwort der KI", summary.data(OUTPUT_COMPLETION));
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
        return CorePlugin.PLUGIN_KEY;
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
        return "KI-Anfrage";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Sendet einen Prompt an das zentral konfigurierte KI-Modell und stellt die Antwort als Knotenausgänge bereit.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                Sendet einen konfigurierten Prompt an das zentral für Prosuna eingerichtete KI-Modell und stellt die erzeugte Antwort für nachfolgende Prozesselemente bereit.
                
                Der Prompt wird in der Elementkonfiguration festgelegt. Neben dem ersten Antworttext liefert das Element den Abschlussgrund, das tatsächlich verwendete Modell und die gemeldeten Nutzungsinformationen als Ausgänge.
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
                        "KI-Antwort erhalten",
                        "Der Prozess wird hier fortgesetzt, nachdem die KI-Antwort erfolgreich abgerufen wurde."
                )
        );
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(OUTPUT_PROMPT, "Eingabe", "Der Anfragetext für das KI-Modell.", "string"),
                new ProcessNodeOutput(OUTPUT_COMPLETION, "Completion", "Die erste erzeugte Completion als Text.", "string"),
                new ProcessNodeOutput(OUTPUT_FINISH_REASON, "Finish Reason", "Der Abschlussgrund der ersten Choice.", "string | null"),
                new ProcessNodeOutput(OUTPUT_RESPONSE_MODEL, "Antwort-Modell", "Das Modell, das die Antwort erzeugt hat.", "string | null"),
                new ProcessNodeOutput(OUTPUT_USAGE, "Nutzung", "Die Token-Nutzungsinformationen der KI-Antwort.", OUTPUT_USAGE_TYPE_DEFINITION)
        );
    }

    @Nullable
    @Override
    public Map<String, List<String>> validateConfiguration(
            @Nonnull ProcessNodeConfigurationValidationContext<AiCompletionActionNodeConfig> context
    ) throws ResponseException {
        var configuration = context.configuration();
        if (!context.isDeferred(AiCompletionActionNodeConfig.PROMPT_FIELD_ID)
                && StringUtils.isNullOrEmpty(configuration.prompt)) {
            return Map.of(
                    AiCompletionActionNodeConfig.PROMPT_FIELD_ID,
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
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<AiCompletionActionNodeConfig> context) throws ProcessNodeExecutionException {
        var configuration = context.getConfigurationOfExecutingNode();
        var renderedPrompt = configuration.prompt;
        if (StringUtils.isNullOrEmpty(renderedPrompt)) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Das Prompt ist nach dem Rendern leer. Bitte überprüfen Sie die Vorlage und die Vorgangsdaten."
            );
        }

        ChatResponse response;
        try {
            response = chatClient
                    .prompt()
                    .user(renderedPrompt)
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
        var nodeData = createNodeData(renderedPrompt, completionText, response);

        return ProcessNodeExecutionResultTaskCompleted.of(SUCCESS_PORT)
                .setNodeData(nodeData);
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
    private Map<String, Object> createNodeData(@Nonnull String renderedPrompt,
                                               @Nonnull String completionText,
                                               @Nonnull ChatResponse response) {
        var generation = response.getResult();
        var nodeData = new LinkedHashMap<String, Object>();
        nodeData.put(OUTPUT_PROMPT, renderedPrompt);
        nodeData.put(OUTPUT_COMPLETION, completionText);
        nodeData.put(
                OUTPUT_FINISH_REASON,
                normalizeFinishReason(generation != null ? generation.getMetadata().getFinishReason() : null)
        );
        nodeData.put(OUTPUT_RESPONSE_MODEL, StringUtils.toNullableTrimmedString(response.getMetadata().getModel()));
        nodeData.put(OUTPUT_USAGE, createUsageData(response.getMetadata().getUsage()));
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

    /**
     * Configuration of the AI completion node.
     */
    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class AiCompletionActionNodeConfig {
        public static final String PROMPT_FIELD_ID = "prompt";

        /**
         * Template-based prompt text that is rendered against the current process data before the AI request is sent.
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
