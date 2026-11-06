package de.aivot.prosuna.backend.plugins.core.v1.nodes.flow;

import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.enums.ValueFunctionType;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.elements.ElementValueFunctions;
import de.aivot.prosuna.backend.elements.models.elements.ElementVisibilityFunctions;
import de.aivot.prosuna.backend.elements.models.elements.LayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.CodeInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.NoCodeInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RadioInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RadioInputElementOption;
import de.aivot.prosuna.backend.javascript.models.JavascriptCode;
import de.aivot.prosuna.backend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.prosuna.backend.nocode.models.NoCodeExpression;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperand;
import de.aivot.prosuna.backend.nocode.models.NoCodeReference;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import de.aivot.prosuna.backend.nocode.services.NoCodeEvaluationService;
import de.aivot.prosuna.backend.plugins.core.v1.operators.bool.NoCodeOrOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.common.NoCodeEqualsOperator;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.services.ProcessDataService;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import tools.jackson.core.JacksonException;

import java.util.List;

/**
 * Condition handling shared by flow control nodes: the No-Code/Low-Code selection in the configuration layout and the
 * evaluation of the selected condition against the current process data.
 */
final class FlowConditionSupport {
    static final String CONDITION_TYPE_FIELD_ID = "conditionType";
    static final String CONDITION_TYPE_VALUE_NO_CODE = "no-code";
    static final String CONDITION_TYPE_VALUE_LOW_CODE = "low-code";
    static final String CONDITION_LOW_CODE_FIELD_ID = "condition";
    static final String CONDITION_NO_CODE_FIELD_ID = "conditionNoCode";
    private static final String NO_CODE_OPERATOR_IS_UNDEFINED = "is-undefined";

    private FlowConditionSupport() {
    }

    /**
     * Configures the condition type selection and shows only the editor of the selected type. Inside a replicating
     * container, the visibility references resolve against the fields of the same row.
     */
    static void configureConditionFields(@Nonnull LayoutElement<?> layout) {
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
    }

    /**
     * @param subject The condition owner in genitive case for error messages, e.g. „des If-Knotens“.
     */
    @Nonnull
    static ConditionEvaluationResult evaluateLowCode(@Nonnull JavascriptEngineFactoryService javascriptEngineFactoryService,
                                                     @Nonnull ProcessExecutionData processExecutionData,
                                                     @Nonnull String lowCodeCondition,
                                                     @Nonnull String subject) throws ProcessNodeExecutionException {
        final String conditionEvaluated;
        final Boolean conditionValue;

        var jsCode = JavascriptCode
                .of(lowCodeCondition);

        try (var engine = javascriptEngineFactoryService.getEngine()) {
            ProcessDataService
                    .fillJsEngineWithData(processExecutionData, engine);

            try {
                var jsResult = engine
                        .evaluateCode(jsCode);

                conditionEvaluated = jsResult.toString();
                conditionValue = jsResult.asBoolean();
            } catch (Exception e) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        e,
                        "Die Bedingung %s konnte nicht als JavaScript ausgeführt werden: %s",
                        subject,
                        e.getMessage()
                );
            }
        } catch (ProcessNodeExecutionExceptionInvalidConfiguration e) {
            throw e;
        } catch (Exception e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Fehler beim Initialisieren der JavaScript-Engine für die Bedingung %s: %s",
                    subject,
                    e.getMessage()
            );
        }

        if (conditionValue == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die Bedingung %s muss einen booleschen JavaScript-Wert ergeben. Übergeben wurde: %s",
                    subject,
                    StringUtils.quote(conditionEvaluated)
            );
        }

        return new ConditionEvaluationResult(
                lowCodeCondition,
                conditionEvaluated,
                conditionValue
        );
    }

    /**
     * @param subject The condition owner in genitive case for error messages, e.g. „des If-Knotens“.
     */
    @Nonnull
    static ConditionEvaluationResult evaluateNoCode(@Nonnull NoCodeEvaluationService noCodeEvaluationService,
                                                    @Nonnull ProcessExecutionData processExecutionData,
                                                    @Nonnull NoCodeOperand noCodeCondition,
                                                    @Nonnull String subject) throws ProcessNodeExecutionException {
        final Object noCodeValue;
        final Boolean noCodeConditionValue;
        try {
            var noCodeResult = noCodeEvaluationService
                    .evaluate(
                            noCodeCondition,
                            new DerivedRuntimeElementData(),
                            processExecutionData
                    );
            noCodeValue = noCodeResult.getValue();
            noCodeConditionValue = noCodeResult.getValueAsBoolean();
        } catch (RuntimeException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Die No-Code-Bedingung %s konnte nicht ausgewertet werden: %s",
                    subject,
                    e.getMessage()
            );
        }

        return new ConditionEvaluationResult(
                serializeNoCodeOperand(noCodeCondition),
                noCodeValue != null ? noCodeValue.toString() : "null",
                noCodeConditionValue
        );
    }

    @Nonnull
    private static String serializeNoCodeOperand(@Nonnull NoCodeOperand noCodeCondition) {
        try {
            return JsonMapperFactory
                    .getInstance()
                    .writeValueAsString(noCodeCondition);
        } catch (JacksonException e) {
            return "No-Code-Ausdruck";
        }
    }

    record ConditionEvaluationResult(
            @Nonnull String conditionExpression,
            @Nonnull String conditionEvaluated,
            @Nonnull Boolean conditionValue
    ) {
    }
}
