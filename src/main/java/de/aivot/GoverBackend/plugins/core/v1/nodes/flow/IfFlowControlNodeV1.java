package de.aivot.GoverBackend.plugins.core.v1.nodes.flow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.elements.enums.ValueFunctionType;
import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.elements.models.EffectiveElementValues;
import de.aivot.GoverBackend.elements.models.elements.ElementVisibilityFunctions;
import de.aivot.GoverBackend.elements.models.elements.ElementValueFunctions;
import de.aivot.GoverBackend.elements.models.elements.form.input.CodeInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.NoCodeInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.NoCodeInputElementItem;
import de.aivot.GoverBackend.elements.models.elements.form.input.RadioInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.RadioInputElementOption;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.javascript.models.JavascriptCode;
import de.aivot.GoverBackend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.nocode.models.NoCodeExpression;
import de.aivot.GoverBackend.nocode.models.NoCodeOperand;
import de.aivot.GoverBackend.nocode.models.NoCodeReference;
import de.aivot.GoverBackend.nocode.models.NoCodeStaticValue;
import de.aivot.GoverBackend.nocode.services.NoCodeEvaluationService;
import de.aivot.GoverBackend.plugins.core.Core;
import de.aivot.GoverBackend.plugins.core.v1.operators.bool.NoCodeOrOperator;
import de.aivot.GoverBackend.plugins.core.v1.operators.common.NoCodeEqualsOperator;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionMissingValue;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.GoverBackend.process.models.*;
import de.aivot.GoverBackend.process.services.ProcessDataService;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class IfFlowControlNodeV1 implements ProcessNodeDefinition {
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
        return Core.PLUGIN_KEY;
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
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionContextConfig context) throws ResponseException {
        var layout = new ConfigLayoutElement();
        layout.setId(getKey() + "-config");

        var conditionTypeField = new RadioInputElement();
        conditionTypeField.setId(CONDITION_TYPE_FIELD_ID);
        conditionTypeField.setLabel("Bedingungsart");
        conditionTypeField.setHint("Wählen Sie aus, mit welchem Typ/Editor die Bedingung definiert wird.");
        conditionTypeField.setRequired(false);
        conditionTypeField.setToggleButtons(true);
        conditionTypeField.setDisplayInline(true);
        conditionTypeField.setValue(new ElementValueFunctions()
                .setType(ValueFunctionType.NoCode)
                .setNoCode(new NoCodeStaticValue(CONDITION_TYPE_VALUE_NO_CODE))
        );
        conditionTypeField.setOptions(List.of(
                RadioInputElementOption.of(CONDITION_TYPE_VALUE_NO_CODE, "No-Code"),
                RadioInputElementOption.of(CONDITION_TYPE_VALUE_LOW_CODE, "Low-Code (JavaScript)")
        ));
        layout.addChild(conditionTypeField);

        var conditionLowCodeField = new CodeInputElement();
        conditionLowCodeField.setId(CONDITION_LOW_CODE_FIELD_ID);
        conditionLowCodeField.setLabel("Bedingung");
        conditionLowCodeField.setHint("JavaScript-Ausdruck oder -Funktion, der/die direkt zu true oder false ausgewertet wird.");
        conditionLowCodeField.setRequired(false);
        conditionLowCodeField.setEditorHeight(140);
        conditionLowCodeField.setWordWrap(true);
        conditionLowCodeField.setVisibility(
                ElementVisibilityFunctions
                        .of(NoCodeExpression.of(
                                NoCodeEqualsOperator.OPERATOR_ID,
                                new NoCodeReference(CONDITION_TYPE_FIELD_ID),
                                new NoCodeStaticValue(CONDITION_TYPE_VALUE_LOW_CODE)
                        ))
                        .recalculateReferencedIds()
        );
        layout.addChild(conditionLowCodeField);

        var conditionNoCodeField = new NoCodeInputElement();
        conditionNoCodeField.setId(CONDITION_NO_CODE_FIELD_ID);
        conditionNoCodeField.setLabel("Bedingung");
        conditionNoCodeField.setHint("No-Code-Ausdruck, der zu true oder false ausgewertet wird.");
        conditionNoCodeField.setRequired(false);
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
        layout.addChild(conditionNoCodeField);

        return layout;
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of(
                new ProcessNodePort(
                        PORT_NAME_TRUE,
                        "Wahr",
                        "Der Prozessfluss wird hier fortgesetzt, wenn die Bedingung erfüllt ist."
                ),
                new ProcessNodePort(
                        PORT_NAME_FALSE,
                        "Falsch",
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
                        "Der konfigurierte Bedingungsausdruck."
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_CONDITION_EVALUATED,
                        "Ausgewerteter Bedingungswert",
                        "Der als JavaScript ausgewertete Rückgabewert des Bedingungsausdrucks."
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_CONDITION_VALUE,
                        "Boolesches Ergebnis",
                        "Das boolesche Ergebnis der Bedingungsauswertung."
                )
        );
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionContextInit context) throws ProcessNodeExecutionException {
        var configuration = context.getConfiguration().getEffectiveValues();
        var lowCodeCondition = toNullableTrimmedString(configuration.get(CONDITION_LOW_CODE_FIELD_ID));

        var noCodeCondition = parseNoCodeCondition(configuration);

        var configuredConditionType = toNullableTrimmedString(configuration.get(CONDITION_TYPE_FIELD_ID));

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
    private ConditionEvaluationResult evaluateLowCodeCondition(@Nonnull ProcessNodeExecutionContextInit context,
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
                    .fillJsEngineWithData(context.getProcessData(), engine);

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
    private ConditionEvaluationResult evaluateNoCodeCondition(@Nonnull ProcessNodeExecutionContextInit context,
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
                            context.getProcessData()
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

    private NoCodeOperand parseNoCodeCondition(@Nonnull EffectiveElementValues configuration) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var rawValue = configuration.getOrDefault(CONDITION_NO_CODE_FIELD_ID, null);

        if (rawValue == null) {
            return null;
        }

        if (rawValue instanceof NoCodeInputElementItem item) {
            return item.getNoCode();
        }

        try {
            return ObjectMapperFactory
                    .getInstance()
                    .convertValue(rawValue, NoCodeInputElementItem.class)
                    .getNoCode();
        } catch (IllegalArgumentException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Die No-Code-Bedingung des If-Knotens hat ein ungültiges Datenformat."
            );
        }
    }

    @Nonnull
    private String serializeNoCodeOperand(@Nonnull NoCodeOperand noCodeCondition) {
        try {
            return ObjectMapperFactory
                    .getInstance()
                    .writeValueAsString(noCodeCondition);
        } catch (JsonProcessingException e) {
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
}
