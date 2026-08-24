package de.aivot.prosuna.backend.plugins.core.v1.nodes.flow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.enums.ValueFunctionType;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.elements.ElementValueFunctions;
import de.aivot.prosuna.backend.elements.models.elements.ElementVisibilityFunctions;
import de.aivot.prosuna.backend.elements.models.elements.form.input.*;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.javascript.models.JavascriptCode;
import de.aivot.prosuna.backend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.nocode.models.NoCodeExpression;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperand;
import de.aivot.prosuna.backend.nocode.models.NoCodeReference;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import de.aivot.prosuna.backend.nocode.services.NoCodeEvaluationService;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.plugins.core.v1.operators.bool.NoCodeOrOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.common.NoCodeEqualsOperator;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionMissingValue;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodeOutput;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.services.ProcessDataService;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;

import java.util.List;
import java.util.Map;

@Component
public class IfFlowControlNodeV1 implements ProcessNodeDefinition<IfFlowControlNodeV1.IfFlowControlNodeConfig> {
    public static final String NODE_KEY = "if";
    private static final String CONDITION_TYPE_FIELD_ID = "conditionType";
    private static final String CONDITION_TYPE_VALUE_NO_CODE = "no-code";
    private static final String CONDITION_TYPE_VALUE_LOW_CODE = "low-code";
    private static final String CONDITION_LOW_CODE_FIELD_ID = "condition";
    private static final String CONDITION_NO_CODE_FIELD_ID = "conditionNoCode";
    private static final String NO_CODE_OPERATOR_IS_UNDEFINED = "is-undefined";

    private static final String PORT_NAME_TRUE = "true";
    private static final String PORT_NAME_FALSE = "false";

    private static final String OUTPUT_NAME_CONDITION_EXPRESSION = "conditionExpression";
    private static final String OUTPUT_NAME_CONDITION_EVALUATED = "conditionEvaluated";
    private static final String OUTPUT_NAME_CONDITION_VALUE = "conditionValue";

    private final JavascriptEngineFactoryService javascriptEngineFactoryService;
    private final NoCodeEvaluationService noCodeEvaluationService;

    public IfFlowControlNodeV1(JavascriptEngineFactoryService javascriptEngineFactoryService,
                               NoCodeEvaluationService noCodeEvaluationService) {
        this.javascriptEngineFactoryService = javascriptEngineFactoryService;
        this.noCodeEvaluationService = noCodeEvaluationService;
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
    public String getName() {
        return "Konditionelle Verzweigung (Wenn-Dann-Sonst)";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Leitet den Vorgang basierend auf einer Bedingung in unterschiedliche Pfade ein.";
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        ConfigLayoutElement layout;
        try {
            layout = ElementPOJOMapper.createFromPOJO(IfFlowControlNodeConfig.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(e, "Fehler bei der Erstellung des Konfigurationslayouts: %s", e.getMessage());
        }

        layout
                .findChild(CONDITION_TYPE_FIELD_ID, RadioInputElement.class)
                .ifPresent(conditionTypeField -> {
                    conditionTypeField.setValue(new ElementValueFunctions()
                            .setType(ValueFunctionType.NoCode)
                            .setNoCode(new NoCodeStaticValue(CONDITION_TYPE_VALUE_NO_CODE)));
                    conditionTypeField.setOptions(List.of(
                            RadioInputElementOption.of(CONDITION_TYPE_VALUE_NO_CODE, "No-Code"),
                            RadioInputElementOption.of(CONDITION_TYPE_VALUE_LOW_CODE, "Low-Code (JavaScript)")
                    ));
                });

        layout
                .findChild(CONDITION_LOW_CODE_FIELD_ID, CodeInputElement.class)
                .ifPresent(conditionLowCodeField -> conditionLowCodeField.setVisibility(
                        ElementVisibilityFunctions
                                .of(NoCodeExpression.of(
                                        NoCodeEqualsOperator.OPERATOR_ID,
                                        new NoCodeReference(CONDITION_TYPE_FIELD_ID),
                                        new NoCodeStaticValue(CONDITION_TYPE_VALUE_LOW_CODE)
                                ))
                                .recalculateReferencedIds()
                ));

        layout
                .findChild(CONDITION_NO_CODE_FIELD_ID, NoCodeInputElement.class)
                .ifPresent(conditionNoCodeField -> {
                    conditionNoCodeField.setReturnType(NoCodeInputElement.NoCodeInputReturnType.BOOLEAN);
                    conditionNoCodeField.setVisibility(
                            ElementVisibilityFunctions
                                    .of(NoCodeExpression.of(
                                            NoCodeOrOperator.OPERATOR_ID,
                                            NoCodeExpression.of(
                                                    NoCodeEqualsOperator.OPERATOR_ID,
                                                    new NoCodeReference(CONDITION_TYPE_FIELD_ID),
                                                    new NoCodeStaticValue(CONDITION_TYPE_VALUE_NO_CODE)
                                            ),
                                            NoCodeExpression.of(
                                                    NO_CODE_OPERATOR_IS_UNDEFINED,
                                                    new NoCodeReference(CONDITION_TYPE_FIELD_ID)
                                            )
                                    ))
                                    .recalculateReferencedIds()
                    );
                });

        return layout;
    }

    @Nonnull
    @Override
    public Class<IfFlowControlNodeConfig> getNodeConfigurationClass() {
        return IfFlowControlNodeConfig.class;
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of(
                new ProcessNodePort(
                        PORT_NAME_TRUE,
                        "Bedingung erfüllt",
                        "Der Prozessfluss wird hier fortgesetzt, wenn die Bedingung erfüllt ist."
                ),
                new ProcessNodePort(
                        PORT_NAME_FALSE,
                        "Bedingung nicht erfüllt",
                        "Der Prozessfluss wird hier fortgesetzt, wenn die Bedingung nicht erfüllt ist."
                )
        );
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(
                        OUTPUT_NAME_CONDITION_EXPRESSION,
                        "Bedingungsausdruck",
                        "Der konfigurierte Bedingungsausdruck.",
                        "string"
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_CONDITION_EVALUATED,
                        "Ausgewerteter Bedingungswert",
                        "Der als JavaScript ausgewertete Rückgabewert des Bedingungsausdrucks.",
                        "string"
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_CONDITION_VALUE,
                        "Boolesches Ergebnis",
                        "Das boolesche Ergebnis der Bedingungsauswertung.",
                        "boolean"
                )
        );
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<IfFlowControlNodeConfig> context) throws ProcessNodeExecutionException {
        var configuration = context.getConfigurationOfExecutingNode();
        var lowCodeCondition = toNullableTrimmedString(configuration.condition);

        var noCodeCondition = parseNoCodeCondition(configuration);

        var configuredConditionType = toNullableTrimmedString(configuration.conditionType);

        var conditionType = resolveConditionType(
                configuredConditionType,
                lowCodeCondition,
                noCodeCondition
        );

        final ConditionEvaluationResult evaluationResult = switch (conditionType) {
            case LOW_CODE -> evaluateLowCodeCondition(context, lowCodeCondition);
            case NO_CODE -> evaluateNoCodeCondition(context, noCodeCondition);
        };

        return new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(evaluationResult.conditionValue() ? PORT_NAME_TRUE : PORT_NAME_FALSE)
                .setNodeData(Map.of(
                        OUTPUT_NAME_CONDITION_EXPRESSION, evaluationResult.conditionExpression(),
                        OUTPUT_NAME_CONDITION_EVALUATED, evaluationResult.conditionEvaluated(),
                        OUTPUT_NAME_CONDITION_VALUE, evaluationResult.conditionValue()
                ));
    }

    @Nonnull
    private ConditionType resolveConditionType(String configuredConditionType,
                                               String lowCodeCondition,
                                               NoCodeOperand noCodeCondition) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (StringUtils.isNullOrEmpty(configuredConditionType)) {
            // Backward compatibility for older If-nodes without explicit condition type.
            if (StringUtils.isNotNullOrEmpty(lowCodeCondition) && noCodeCondition == null) {
                return ConditionType.LOW_CODE;
            }
            return ConditionType.NO_CODE;
        }

        return switch (configuredConditionType) {
            case CONDITION_TYPE_VALUE_LOW_CODE -> ConditionType.LOW_CODE;
            case CONDITION_TYPE_VALUE_NO_CODE -> ConditionType.NO_CODE;
            default -> throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Der Modus der If-Bedingung ist ungültig. Übergeben wurde: %s",
                    StringUtils.quote(configuredConditionType)
            );
        };
    }

    @Nonnull
    private ConditionEvaluationResult evaluateLowCodeCondition(@Nonnull ProcessNodeExecutionInitContext<IfFlowControlNodeConfig> context,
                                                               String lowCodeCondition) throws ProcessNodeExecutionException {
        if (StringUtils.isNullOrEmpty(lowCodeCondition)) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Die Low-Code-Bedingung für den If-Knoten wurde nicht angegeben."
            );
        }

        final String conditionEvaluated;
        final Boolean conditionValue;

        var jsCode = JavascriptCode
                .of(lowCodeCondition);

        try (var engine = javascriptEngineFactoryService.getEngine()) {
            ProcessDataService
                    .fillJsEngineWithData(context.getCurrentProcessExecutionData(), engine);

            try {
                var jsResult = engine
                        .evaluateCode(jsCode);

                conditionEvaluated = jsResult.toString();
                conditionValue = jsResult.asBoolean();
            } catch (Exception e) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        e,
                        "Die Bedingung des If-Knotens konnte nicht als JavaScript ausgeführt werden: %s",
                        e.getMessage()
                );
            }
        } catch (ProcessNodeExecutionExceptionInvalidConfiguration e) {
            throw e;
        } catch (Exception e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Fehler beim Initialisieren der JavaScript-Engine für den If-Knoten: %s",
                    e.getMessage()
            );
        }

        if (conditionValue == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die Bedingung des If-Knotens muss einen booleschen JavaScript-Wert ergeben. Übergeben wurde: %s",
                    StringUtils.quote(conditionEvaluated)
            );
        }

        return new ConditionEvaluationResult(
                lowCodeCondition,
                conditionEvaluated,
                conditionValue
        );
    }

    @Nonnull
    private ConditionEvaluationResult evaluateNoCodeCondition(@Nonnull ProcessNodeExecutionInitContext<IfFlowControlNodeConfig> context,
                                                              NoCodeOperand noCodeCondition) throws ProcessNodeExecutionException {
        if (noCodeCondition == null) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Die No-Code-Bedingung für den If-Knoten wurde nicht angegeben."
            );
        }

        final Object noCodeValue;
        final Boolean noCodeConditionValue;
        try {
            var noCodeResult = noCodeEvaluationService
                    .evaluate(
                            noCodeCondition,
                            new DerivedRuntimeElementData(),
                            context.getCurrentProcessExecutionData()
                    );
            noCodeValue = noCodeResult.getValue();
            noCodeConditionValue = noCodeResult.getValueAsBoolean();
        } catch (RuntimeException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Die No-Code-Bedingung des If-Knotens konnte nicht ausgewertet werden: %s",
                    e.getMessage()
            );
        }

        return new ConditionEvaluationResult(
                serializeNoCodeOperand(noCodeCondition),
                noCodeValue != null ? noCodeValue.toString() : "null",
                noCodeConditionValue
        );
    }

    private NoCodeOperand parseNoCodeCondition(@Nonnull IfFlowControlNodeConfig configuration) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (configuration.conditionNoCode == null) {
            return null;
        }

        return configuration.conditionNoCode.getNoCode();
    }

    @Nonnull
    private String serializeNoCodeOperand(@Nonnull NoCodeOperand noCodeCondition) {
        try {
            return JsonMapperFactory
                    .getInstance()
                    .writeValueAsString(noCodeCondition);
        } catch (JacksonException e) {
            return "No-Code-Ausdruck";
        }
    }

    private static String toNullableTrimmedString(Object value) {
        if (value == null) {
            return null;
        }
        var str = value.toString().trim();
        return str.isEmpty() ? null : str;
    }

    private enum ConditionType {
        LOW_CODE,
        NO_CODE
    }

    private record ConditionEvaluationResult(
            @Nonnull String conditionExpression,
            @Nonnull String conditionEvaluated,
            @Nonnull Boolean conditionValue
    ) {
    }

    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class IfFlowControlNodeConfig {
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
