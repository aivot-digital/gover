package de.aivot.prosuna.backend.plugins.core.v1.nodes.flow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.ReplicatingContainerLayoutElementElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.ReplicatingContainerRowIdPOJOBinding;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.elements.form.input.NoCodeInputElementItem;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.nocode.services.NoCodeEvaluationService;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionMissingValue;
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
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static de.aivot.prosuna.backend.plugins.core.v1.nodes.flow.FlowConditionSupport.CONDITION_LOW_CODE_FIELD_ID;
import static de.aivot.prosuna.backend.plugins.core.v1.nodes.flow.FlowConditionSupport.CONDITION_NO_CODE_FIELD_ID;
import static de.aivot.prosuna.backend.plugins.core.v1.nodes.flow.FlowConditionSupport.CONDITION_TYPE_FIELD_ID;
import static de.aivot.prosuna.backend.plugins.core.v1.nodes.flow.FlowConditionSupport.CONDITION_TYPE_VALUE_LOW_CODE;
import static de.aivot.prosuna.backend.plugins.core.v1.nodes.flow.FlowConditionSupport.CONDITION_TYPE_VALUE_NO_CODE;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.text;

@Component
public class SwitchFlowControlNodeV1 implements ProcessNodeDefinition<SwitchFlowControlNodeV1.SwitchFlowControlNodeConfig> {
    public static final String NODE_KEY = "switch";
    static final String CASES_FIELD_ID = "cases";
    static final String CASE_LABEL_FIELD_ID = "caseLabel";
    static final String DEFAULT_PORT_LABEL_FIELD_ID = "defaultPortLabel";

    static final String PORT_NAME_DEFAULT = "default";
    static final String DEFAULT_PORT_LABEL = "Standard";

    private static final String OUTPUT_NAME_MATCHED_CASE_ID = "matchedCaseId";
    private static final String OUTPUT_NAME_MATCHED_CASE_LABEL = "matchedCaseLabel";
    private static final String OUTPUT_NAME_DEFAULT_USED = "defaultUsed";

    private final JavascriptEngineFactoryService javascriptEngineFactoryService;
    private final NoCodeEvaluationService noCodeEvaluationService;

    public SwitchFlowControlNodeV1(JavascriptEngineFactoryService javascriptEngineFactoryService,
                                   NoCodeEvaluationService noCodeEvaluationService) {
        this.javascriptEngineFactoryService = javascriptEngineFactoryService;
        this.noCodeEvaluationService = noCodeEvaluationService;
    }

    @Nonnull
    @Override
    public String generateExecutionSummary(@Nonnull ProcessNodeExecutionSummaryContext<SwitchFlowControlNodeConfig> context) {
        var summary = new ExecutionSummaryMarkdown(context);
        if (Boolean.TRUE.equals(summary.data(OUTPUT_NAME_DEFAULT_USED))) {
            return "Keiner der Fälle wurde zu „Wahr“ ausgewertet. Der Vorgang wurde über den Standardausgang „"
                    + text(getDefaultPortLabel(context.configurationOfExecutingNode())) + "“ fortgesetzt.";
        }
        var caseLabel = summary.value(OUTPUT_NAME_MATCHED_CASE_LABEL);
        return "Der Fall „" + caseLabel + "“ wurde als erster zu „Wahr“ ausgewertet. Der Vorgang wurde über den Ausgang „"
                + caseLabel + "“ fortgesetzt.";
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
        return ProcessNodeType.FlowControl;
    }

    @Nonnull
    @Override
    public ProcessNodeExecutionType[] getExecutionTypes() {
        return new ProcessNodeExecutionType[]{ProcessNodeExecutionType.Automatic};
    }

    @Nonnull
    @Override
    public String getName() {
        return "Fallbasierte Verzweigung (Wenn-Dann)";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Leitet den Vorgang über den Ausgang des ersten zutreffenden Falls weiter.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                Prüft beliebig viele Fälle in der konfigurierten Reihenfolge und führt den Prozess über den Ausgang des ersten Falls fort, dessen Bedingung zu „Wahr“ ausgewertet wird. Trifft kein Fall zu, wird der Prozess über den Standardausgang fortgesetzt.

                Jeder Fall erhält einen eigenen Ausgang. Die Bedingung eines Falls kann als Low-Code- oder No-Code-Ausdruck konfiguriert werden und auf die aktuellen Laufzeitdaten zugreifen. Der zutreffende Fall wird als Elementausgang bereitgestellt.
                """;
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        ConfigLayoutElement layout;
        try {
            layout = ElementPOJOMapper.createFromPOJO(SwitchFlowControlNodeConfig.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(e, "Fehler bei der Erstellung des Konfigurationslayouts: %s", e.getMessage());
        }

        FlowConditionSupport.configureConditionFields(layout);

        return layout;
    }

    @Nonnull
    @Override
    public Class<SwitchFlowControlNodeConfig> getNodeConfigurationClass() {
        return SwitchFlowControlNodeConfig.class;
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts(@Nonnull SwitchFlowControlNodeConfig configuration) {
        var cases = getCases(configuration);
        var ports = new ArrayList<ProcessNodePort>(cases.size() + 1);
        for (var i = 0; i < cases.size(); i++) {
            var caseConfig = cases.get(i);
            ports.add(new ProcessNodePort(
                    getPortKey(caseConfig),
                    getCaseLabel(caseConfig, i),
                    "Der Prozessfluss wird hier fortgesetzt, wenn dieser Fall als erster zu „Wahr“ ausgewertet wird."
            ));
        }
        // The default port is always the last one, so it reads as the fallback after all cases.
        ports.add(new ProcessNodePort(
                PORT_NAME_DEFAULT,
                getDefaultPortLabel(configuration),
                "Der Prozessfluss wird hier fortgesetzt, wenn keiner der Fälle zu „Wahr“ ausgewertet wird."
        ));
        return ports;
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(
                        OUTPUT_NAME_MATCHED_CASE_ID,
                        "ID des zutreffenden Falls",
                        "Die ID des ersten Falls, der zu „Wahr“ ausgewertet wurde, oder null, wenn kein Fall zutrifft.",
                        "string | null"
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_MATCHED_CASE_LABEL,
                        "Beschriftung des zutreffenden Falls",
                        "Die Beschriftung des ersten Falls, der zu „Wahr“ ausgewertet wurde, oder null, wenn kein Fall zutrifft.",
                        "string | null"
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_DEFAULT_USED,
                        "Standardausgang verwendet",
                        "Gibt an, ob der Vorgang über den Standardausgang fortgesetzt wurde.",
                        "boolean"
                )
        );
    }

    @Nullable
    @Override
    public Map<String, List<String>> validateConfiguration(@Nonnull ProcessNodeConfigurationValidationContext<SwitchFlowControlNodeConfig> context) {
        var configuration = context.configuration();
        var cases = getCases(configuration);

        // Port labels must stay distinguishable in the process model; the effective default label belongs to the default port.
        var labelOwners = new HashMap<String, String>();
        labelOwners.put(normalizeLabel(getDefaultPortLabel(configuration)), "den Standardausgang");

        var errors = new ArrayList<String>();
        for (var i = 0; i < cases.size(); i++) {
            var label = StringUtils.toNullableTrimmedString(cases.get(i).caseLabel);
            if (label == null) {
                // The required check of the label field reports missing labels.
                continue;
            }
            var owner = labelOwners.putIfAbsent(normalizeLabel(label), "Fall " + (i + 1));
            if (owner != null) {
                errors.add("Fall %d: Die Beschriftung %s wird bereits für %s verwendet. Vergeben Sie eine eindeutige Beschriftung.".formatted(
                        i + 1,
                        StringUtils.quote(label),
                        owner
                ));
            }
        }

        return errors.isEmpty() ? null : Map.of(CASES_FIELD_ID, errors);
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<SwitchFlowControlNodeConfig> context) throws ProcessNodeExecutionException {
        var cases = getCases(context.getConfigurationOfExecutingNode());

        // Only the first matching case selects the port; later cases are not evaluated.
        for (var i = 0; i < cases.size(); i++) {
            var caseConfig = cases.get(i);
            var label = getCaseLabel(caseConfig, i);
            if (evaluateCase(context, caseConfig, label)) {
                var nodeData = new HashMap<String, Object>();
                nodeData.put(OUTPUT_NAME_MATCHED_CASE_ID, caseConfig.id);
                nodeData.put(OUTPUT_NAME_MATCHED_CASE_LABEL, label);
                nodeData.put(OUTPUT_NAME_DEFAULT_USED, false);
                return new ProcessNodeExecutionResultTaskCompleted()
                        .setViaPort(getPortKey(caseConfig))
                        .setNodeData(nodeData);
            }
        }

        var nodeData = new HashMap<String, Object>();
        nodeData.put(OUTPUT_NAME_MATCHED_CASE_ID, null);
        nodeData.put(OUTPUT_NAME_MATCHED_CASE_LABEL, null);
        nodeData.put(OUTPUT_NAME_DEFAULT_USED, true);
        return new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(PORT_NAME_DEFAULT)
                .setNodeData(nodeData);
    }

    private boolean evaluateCase(@Nonnull ProcessNodeExecutionInitContext<SwitchFlowControlNodeConfig> context,
                                 @Nonnull SwitchCaseConfig caseConfig,
                                 @Nonnull String label) throws ProcessNodeExecutionException {
        var subject = "des Falls " + StringUtils.quote(label);
        var conditionType = Objects.requireNonNullElse(StringUtils.toNullableTrimmedString(caseConfig.conditionType), CONDITION_TYPE_VALUE_NO_CODE);

        return switch (conditionType) {
            case CONDITION_TYPE_VALUE_LOW_CODE -> {
                var lowCodeCondition = StringUtils.toNullableTrimmedString(caseConfig.condition);
                if (lowCodeCondition == null) {
                    throw new ProcessNodeExecutionExceptionMissingValue(
                            "Die Low-Code-Bedingung %s wurde nicht angegeben.",
                            subject
                    );
                }
                yield FlowConditionSupport
                        .evaluateLowCode(javascriptEngineFactoryService, context.getCurrentProcessExecutionData(), lowCodeCondition, subject)
                        .conditionValue();
            }
            case CONDITION_TYPE_VALUE_NO_CODE -> {
                var noCodeCondition = caseConfig.conditionNoCode != null ? caseConfig.conditionNoCode.getNoCode() : null;
                if (noCodeCondition == null) {
                    throw new ProcessNodeExecutionExceptionMissingValue(
                            "Die No-Code-Bedingung %s wurde nicht angegeben.",
                            subject
                    );
                }
                yield FlowConditionSupport
                        .evaluateNoCode(noCodeEvaluationService, context.getCurrentProcessExecutionData(), noCodeCondition, subject)
                        .conditionValue();
            }
            default -> throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die Bedingungsart %s ist ungültig. Übergeben wurde: %s",
                    subject,
                    StringUtils.quote(conditionType)
            );
        };
    }

    @Nonnull
    private static List<SwitchCaseConfig> getCases(@Nonnull SwitchFlowControlNodeConfig configuration) {
        return configuration.cases != null ? configuration.cases : List.of();
    }

    /**
     * Uses the stable row ID as port key, so edges survive label changes and reordering. Row IDs are UUIDs; without
     * hyphens they fit the 32-character limit of port keys. The port resolver reports rows without an ID as empty keys.
     */
    @Nonnull
    private static String getPortKey(@Nonnull SwitchCaseConfig caseConfig) {
        return caseConfig.id != null ? caseConfig.id.replace("-", "") : "";
    }

    /** Falls back to the case number while the required label is still missing, so the other ports stay resolvable. */
    @Nonnull
    private static String getCaseLabel(@Nonnull SwitchCaseConfig caseConfig, int index) {
        return Objects.requireNonNullElse(StringUtils.toNullableTrimmedString(caseConfig.caseLabel), "Fall " + (index + 1));
    }

    @Nonnull
    private static String getDefaultPortLabel(@Nonnull SwitchFlowControlNodeConfig configuration) {
        return Objects.requireNonNullElse(StringUtils.toNullableTrimmedString(configuration.defaultPortLabel), DEFAULT_PORT_LABEL);
    }

    @Nonnull
    private static String normalizeLabel(@Nonnull String label) {
        return label.trim().toLowerCase(Locale.ROOT);
    }

    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class SwitchFlowControlNodeConfig {
        public List<SwitchCaseConfig> cases;

        @InputElementPOJOBinding(id = DEFAULT_PORT_LABEL_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Beschriftung des Standardausgangs"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Der Standardausgang wird verwendet, wenn keiner der Fälle zutrifft. Ohne Eingabe lautet die Beschriftung „Standard“."),
                @ElementPOJOBindingProperty(key = "placeholder", strValue = DEFAULT_PORT_LABEL),
                @ElementPOJOBindingProperty(key = "maxCharacters", intValue = 64)
        })
        public String defaultPortLabel;
    }

    @ReplicatingContainerLayoutElementElementPOJOBinding(id = CASES_FIELD_ID, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "Fälle"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Die Fälle werden in der angegebenen Reihenfolge geprüft. Der Vorgang wird über den Ausgang des ersten Falls fortgesetzt, dessen Bedingung zu „Wahr“ ausgewertet wird. Weitere zutreffende Fälle werden nicht berücksichtigt."),
            @ElementPOJOBindingProperty(key = "required", boolValue = true),
            @ElementPOJOBindingProperty(key = "headlineTemplate", strValue = "Fall #"),
            @ElementPOJOBindingProperty(key = "addLabel", strValue = "Fall hinzufügen"),
            @ElementPOJOBindingProperty(key = "removeLabel", strValue = "Fall entfernen")
    })
    public static class SwitchCaseConfig {
        @ReplicatingContainerRowIdPOJOBinding
        public String id;

        @InputElementPOJOBinding(id = CASE_LABEL_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Beschriftung des Ausgangs"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Wird als Beschriftung des Ausgangs für diesen Fall angezeigt."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "maxCharacters", intValue = 64)
        })
        public String caseLabel;

        @InputElementPOJOBinding(id = CONDITION_TYPE_FIELD_ID, type = ElementType.Radio, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Bedingungsart"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Wählen Sie aus, mit welchem Typ/Editor die Bedingung definiert wird."),
                @ElementPOJOBindingProperty(key = "toggleButtons", boolValue = true),
                @ElementPOJOBindingProperty(key = "displayInline", boolValue = true),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String conditionType;

        @InputElementPOJOBinding(id = CONDITION_LOW_CODE_FIELD_ID, type = ElementType.CodeInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Bedingung"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "JavaScript-Ausdruck oder -Funktion, der/die direkt zu true oder false ausgewertet wird."),
                @ElementPOJOBindingProperty(key = "editorHeight", intValue = 140),
                @ElementPOJOBindingProperty(key = "wordWrap", boolValue = true),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String condition;

        @InputElementPOJOBinding(id = CONDITION_NO_CODE_FIELD_ID, type = ElementType.NoCodeInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Bedingung"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "No-Code-Ausdruck, der zu true oder false ausgewertet wird."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public NoCodeInputElementItem conditionNoCode;
    }
}
