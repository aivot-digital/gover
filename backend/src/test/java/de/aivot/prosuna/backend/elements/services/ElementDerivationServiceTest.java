package de.aivot.prosuna.backend.elements.services;

import de.aivot.prosuna.backend.elements.models.elements.form.input.DurationInputElement;
import de.aivot.prosuna.backend.enums.DurationPrecision;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.elements.models.elements.form.input.PaymentConfigElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.PaymentConfigElementValue;
import de.aivot.prosuna.backend.elements.models.elements.form.input.PaymentConfigElementValueItem;
import de.aivot.prosuna.backend.elements.enums.EffectiveValueSource;
import de.aivot.prosuna.backend.elements.enums.InputMode;
import de.aivot.prosuna.backend.elements.enums.InputModeEvaluationContext;
import de.aivot.prosuna.backend.elements.enums.InputVariableSource;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.ElementDerivationOptions;
import de.aivot.prosuna.backend.elements.models.ElementDerivationRequest;
import de.aivot.prosuna.backend.elements.models.elements.BaseFormElement;
import de.aivot.prosuna.backend.elements.models.elements.DynamicTextElement;
import de.aivot.prosuna.backend.elements.models.elements.ElementOverrideFunctions;
import de.aivot.prosuna.backend.elements.models.elements.ElementValidationFunctions;
import de.aivot.prosuna.backend.elements.models.elements.ElementValueFunctions;
import de.aivot.prosuna.backend.elements.models.elements.ElementVisibilityFunctions;
import de.aivot.prosuna.backend.elements.models.elements.ValidationNoCodeWrapper;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RichTextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.NumberInputElement;
import de.aivot.prosuna.backend.elements.models.input.InputModePolicy;
import de.aivot.prosuna.backend.elements.models.input.DynamicTextPolicy;
import de.aivot.prosuna.backend.elements.models.input.InputVariableReference;
import de.aivot.prosuna.backend.elements.models.input.LiteralAuthoredInputValue;
import de.aivot.prosuna.backend.elements.models.input.LowCodeAuthoredInputValue;
import de.aivot.prosuna.backend.elements.models.input.NoCodeAuthoredInputValue;
import de.aivot.prosuna.backend.elements.models.input.VariableAuthoredInputValue;
import de.aivot.prosuna.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.EffectiveReplicatingContainerLayoutElementValue;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElementValue;
import de.aivot.prosuna.backend.elements.models.elements.layout.SummaryLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.steps.BaseStepElement;
import de.aivot.prosuna.backend.elements.models.elements.steps.GenericStepElement;
import de.aivot.prosuna.backend.elements.services.ElementDerivationLogger;
import de.aivot.prosuna.backend.elements.services.ElementDerivationService;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import de.aivot.prosuna.backend.javascript.models.JavascriptCode;
import de.aivot.prosuna.backend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.prosuna.backend.nocode.models.NoCodeExpression;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperand;
import de.aivot.prosuna.backend.nocode.models.NoCodeProcessDataReference;
import de.aivot.prosuna.backend.nocode.models.NoCodeReference;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import de.aivot.prosuna.backend.nocode.services.NoCodeEvaluationService;
import de.aivot.prosuna.backend.nocode.services.NoCodeOperatorProviderService;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.plugins.core.v1.operators.CommonOperatorsV1;
import de.aivot.prosuna.backend.submission.services.ElementDataTransformService;
import de.aivot.prosuna.backend.utils.SpringContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementDerivationServiceTest {
    @Test
    void durationFormattingSupportsAuthoringAndRuntimeInputModes() {
        var previousContext = (ApplicationContext) ReflectionTestUtils.getField(SpringContext.class, "context");
        try (var context = new StaticApplicationContext()) {
            context.getBeanFactory().registerSingleton("noCodeOperatorProviderService",
                    new NoCodeOperatorProviderService(List.of(new CommonOperatorsV1(null, null, null))));
            new SpringContext().setApplicationContext(context);
            var field = dynamicTextField();
            var root = createRoot(List.of(field));
            var values = new AuthoredElementValues();
            values.put("field", new NoCodeAuthoredInputValue(
                    NoCodeExpression.of("format-duration", NoCodeStaticValue.of(99000), NoCodeStaticValue.of("hours"))));

            var authoring = derive(root, values, new ElementDerivationOptions(),
                    new ProcessExecutionData(), InputModeEvaluationContext.Authoring);
            assertFalse(authoring.hasAnyError());
            assertTrue(authoring.getElementStates().get("field").isInputValueDeferred());

            var runtime = derive(root, values, new ElementDerivationOptions(),
                    new ProcessExecutionData(), InputModeEvaluationContext.Runtime);
            assertFalse(runtime.hasAnyError());
            assertEquals("27 Stunden 30 Minuten", runtime.getEffectiveValues().get("field"));
        } finally {
            new SpringContext().setApplicationContext(previousContext);
        }
    }

    @Test
    void durationFieldsValidateAuthoredAndDerivedSecondsWithoutRounding() {
        var field = new DurationInputElement().setMinDuration(60L).setMaxDuration(100000L);
        field.setId("duration");
        var root = createRoot(List.of(field));
        for (Object invalid : List.of("invalid", "", -1, 1.5, 75, 100020)) {
            var result = derive(root, new AuthoredElementValues().putLiteral("duration", invalid), new ElementDerivationOptions());
            assertTrue(result.hasAnyError(), String.valueOf(invalid));
        }
        var result = derive(root, new AuthoredElementValues().putLiteral("duration", 99000), new ElementDerivationOptions());
        assertFalse(result.hasAnyError());
        assertEquals(99000, ((Number) result.getEffectiveValues().get("duration")).intValue());
        field.setPrecision(DurationPrecision.Second);
        result = derive(root, new AuthoredElementValues().putLiteral("duration", 75), new ElementDerivationOptions());
        assertFalse(result.hasAnyError());
        assertEquals(75, ((Number) result.getEffectiveValues().get("duration")).intValue());
        field.setValue(new ElementValueFunctions().setNoCode(NoCodeStaticValue.of(1.5)));
        result = derive(root, new AuthoredElementValues(), new ElementDerivationOptions());
        assertTrue(result.hasAnyError());
        assertEquals(1.5, ((Number) result.getEffectiveValues().get("duration")).doubleValue());
    }

    @Test
    void shouldReturnFieldErrorsWhenClearingAPreviouslyValidPaymentPurpose() {
        var field = new PaymentConfigElement();
        field.setId("payment");
        field.setRequired(true);
        var item = new PaymentConfigElementValueItem(
                PaymentConfigElementValueItem.IdType.AutoGeneratedUUID, null, "Position", "Reference",
                PaymentConfigElementValueItem.CostType.FixedCosts, java.math.BigDecimal.TEN, null, null, null,
                PaymentConfigElementValueItem.QuantityType.FixedQuantity, 1L, null, null, null, java.math.BigDecimal.ZERO, null);
        var key = java.util.UUID.randomUUID();
        var values = new AuthoredElementValues().putLiteral("payment", new PaymentConfigElementValue(
                key, "Purpose", "Description", false, null, List.of(item), null, null));
        var root = createRoot(List.of(field));
        assertFalse(derive(root, values, new ElementDerivationOptions(), new ProcessExecutionData(), InputModeEvaluationContext.Authoring).hasAnyError());

        values.putLiteral("payment", new PaymentConfigElementValue(key, null, "Description", false, null, List.of(item), null, null));
        var result = derive(root, values, new ElementDerivationOptions(), new ProcessExecutionData(), InputModeEvaluationContext.Authoring);
        assertTrue(result.hasAnyError());
        assertEquals(Map.of("purpose", "Geben Sie einen Verwendungszweck an.", "messages", List.of("Geben Sie einen Verwendungszweck an.")),
                result.getElementStates().get("payment").getErrorDetails());
    }

    @Test
    void shouldResolveLiteralValuesForPolicyEnabledFields() {
        var field = dynamicNumberField();
        var authoredValues = new AuthoredElementValues();
        authoredValues.putLiteral("field", 3);

        var result = derive(createRoot(List.of(field)), authoredValues, new ElementDerivationOptions());

        assertEquals(3, ((Number) result.getEffectiveValues().get("field")).intValue());
        assertNull(result.getElementStates().get("field").getError());
    }

    @Test
    void shouldRejectDynamicValuesInTheLiteralOnlyContext() {
        var field = dynamicNumberField();
        var authoredValues = new AuthoredElementValues();
        authoredValues.put("field", new VariableAuthoredInputValue(
                new InputVariableReference(InputVariableSource.ProcessData, "order.amount", null)
        ));

        var result = derive(createRoot(List.of(field)), authoredValues, new ElementDerivationOptions());

        assertFalse(result.getEffectiveValues().containsKey("field"));
        assertTrue(result.getElementStates().get("field").getError().contains("nicht freigegeben"));
    }

    @Test
    void shouldRejectDynamicValuesForFieldsWithoutAPolicy() {
        var field = new NumberInputElement();
        field.setId("field");
        var authoredValues = new AuthoredElementValues();
        authoredValues.put("field", new VariableAuthoredInputValue(
                new InputVariableReference(InputVariableSource.ProcessData, "order.amount", null)
        ));

        var result = derive(
                createRoot(List.of(field)),
                authoredValues,
                new ElementDerivationOptions(),
                new ProcessExecutionData(),
                InputModeEvaluationContext.Runtime
        );

        assertFalse(result.getEffectiveValues().containsKey("field"));
        assertTrue(result.getElementStates().get("field").getError().contains("keine dynamischen Eingabemodi"));
    }

    @Test
    void shouldNeverCopyWrappersIntoSkippedEffectiveValues() {
        var field = dynamicNumberField();
        var authoredValues = new AuthoredElementValues();
        authoredValues.put("field", new LiteralAuthoredInputValue(3));
        var options = new ElementDerivationOptions().setSkipValuesForElementIds(List.of("field"));

        var result = derive(
                createRoot(List.of(field)),
                authoredValues,
                options,
                new ProcessExecutionData(),
                InputModeEvaluationContext.Authoring
        );

        assertEquals(3, ((Number) result.getEffectiveValues().get("field")).intValue());
        assertFalse(result.getEffectiveValues().get("field") instanceof LiteralAuthoredInputValue);
    }

    @Test
    void shouldValidateDynamicValuesWithoutResolvingThemDuringAuthoring() {
        var field = dynamicNumberField();
        field.setRequired(true);
        var authoredValues = new AuthoredElementValues();
        authoredValues.put("field", new VariableAuthoredInputValue(
                new InputVariableReference(InputVariableSource.ProcessData, "order.amount", null)
        ));

        var result = derive(
                createRoot(List.of(field)),
                authoredValues,
                new ElementDerivationOptions(),
                new ProcessExecutionData(),
                InputModeEvaluationContext.Authoring
        );

        assertTrue(result.getEffectiveValues().containsKey("field"));
        assertNull(result.getEffectiveValues().get("field"));
        assertNull(result.getElementStates().get("field").getError());
        assertTrue(result.getElementStates().get("field").isInputValueDeferred());
    }

    @Test
    void shouldResolveAllDynamicModesAtRuntime() {
        var variableField = dynamicNumberField();
        variableField.setId("variable");
        var noCodeField = dynamicNumberField();
        noCodeField.setId("noCode");
        var lowCodeField = dynamicNumberField();
        lowCodeField.setId("lowCode");
        var values = new AuthoredElementValues();
        values.put("variable", new VariableAuthoredInputValue(
                new InputVariableReference(InputVariableSource.ProcessData, "order.amount", null)
        ));
        values.put("noCode", new NoCodeAuthoredInputValue(NoCodeStaticValue.of(4)));
        values.put("lowCode", new LowCodeAuthoredInputValue("$.order.amount + 2"));
        var processData = new ProcessExecutionData().addProcessData("order", java.util.Map.of("amount", 5));

        var result = derive(
                createRoot(List.of(variableField, noCodeField, lowCodeField)),
                values,
                new ElementDerivationOptions(),
                processData,
                InputModeEvaluationContext.Runtime
        );

        assertEquals(5, ((Number) result.getEffectiveValues().get("variable")).intValue());
        assertEquals(4, ((Number) result.getEffectiveValues().get("noCode")).intValue());
        assertEquals(7, ((Number) result.getEffectiveValues().get("lowCode")).intValue());
        assertFalse(result.hasAnyError());
        assertFalse(result.getElementStates().get("variable").isInputValueDeferred());
        assertFalse(result.getElementStates().get("noCode").isInputValueDeferred());
        assertFalse(result.getElementStates().get("lowCode").isInputValueDeferred());
    }

    @Test
    void shouldInterpolateDynamicLiteralTextOnlyAtRuntime() {
        var field = dynamicTextField();
        var authoredValues = new AuthoredElementValues();
        authoredValues.putLiteral("field", "Hello {{ $.person.name }}");
        var processData = new ProcessExecutionData().addProcessData("person", Map.of("name", "Ada"));

        var authoringResult = derive(
                createRoot(List.of(field)),
                authoredValues,
                new ElementDerivationOptions(),
                processData,
                InputModeEvaluationContext.Authoring
        );
        var runtimeResult = derive(
                createRoot(List.of(field)),
                authoredValues,
                new ElementDerivationOptions(),
                processData,
                InputModeEvaluationContext.Runtime
        );
        var literalOnlyResult = derive(
                createRoot(List.of(field)),
                authoredValues,
                new ElementDerivationOptions(),
                processData,
                InputModeEvaluationContext.LiteralOnly
        );

        assertEquals("Hello {{ $.person.name }}", authoringResult.getEffectiveValues().get("field"));
        assertEquals("Hello Ada", runtimeResult.getEffectiveValues().get("field"));
        assertEquals("Hello {{ $.person.name }}", literalOnlyResult.getEffectiveValues().get("field"));
    }

    @Test
    void shouldValidateDynamicLiteralTextDuringAuthoring() {
        var field = dynamicTextField();
        var authoredValues = new AuthoredElementValues();
        authoredValues.putLiteral("field", "{% if $.enabled %}missing end");

        var result = derive(
                createRoot(List.of(field)),
                authoredValues,
                new ElementDerivationOptions(),
                new ProcessExecutionData(),
                InputModeEvaluationContext.Authoring
        );

        assertFalse(result.getEffectiveValues().containsKey("field"));
        assertTrue(result.getElementStates().get("field").getError().contains("dynamische Text"));
    }

    @Test
    void shouldPreserveDeclaredDynamicTextPolicyAcrossOverrides() {
        for (var field : List.of(new TextInputElement(), new RichTextInputElement())) {
            field.setId("field");
            var policy = new DynamicTextPolicy(List.of(InputVariableSource.ProcessData));
            ((DynamicTextElement) field).setDynamicTextPolicy(policy);
            field.setOverride(new ElementOverrideFunctions().setJavascriptCode(
                    JavascriptCode.of("({ type: element.type, id: element.id, label: 'Overridden' })")
            ));
            var authoredValues = new AuthoredElementValues();
            authoredValues.putLiteral("field", "Hello {{ $.name }}");

            var result = derive(createRoot(List.of(field)), authoredValues, new ElementDerivationOptions(),
                    new ProcessExecutionData().addProcessData("name", "Ada"), InputModeEvaluationContext.Runtime);

            assertFalse(result.hasAnyError());
            assertEquals("Hello Ada", result.getEffectiveValues().get("field"));
            assertEquals(policy, assertInstanceOf(DynamicTextElement.class,
                    result.getElementStates().get("field").getOverride()).getDynamicTextPolicy());
        }
    }

    @Test
    void shouldNotEnableDynamicTextThroughOverrides() {
        for (var field : List.of(new TextInputElement(), new RichTextInputElement())) {
            field.setId("field");
            field.setOverride(new ElementOverrideFunctions().setJavascriptCode(JavascriptCode.of("""
                    ({
                        type: element.type,
                        id: element.id,
                        dynamicTextPolicy: { variableSuggestionSources: ['ProcessData'] }
                    })
                    """)));
            var authoredValues = new AuthoredElementValues();
            authoredValues.putLiteral("field", "Hello {{ $.name }}");

            var result = derive(createRoot(List.of(field)), authoredValues, new ElementDerivationOptions(),
                    new ProcessExecutionData().addProcessData("name", "Ada"), InputModeEvaluationContext.Runtime);

            assertFalse(result.hasAnyError());
            assertEquals("Hello {{ $.name }}", result.getEffectiveValues().get("field"));
            assertNull(assertInstanceOf(DynamicTextElement.class,
                    result.getElementStates().get("field").getOverride()).getDynamicTextPolicy());
        }
    }

    @Test
    void shouldNotInterpolateResultsOfNonLiteralInputModes() {
        var variableField = dynamicTextField();
        variableField.setId("variable");
        var noCodeField = dynamicTextField();
        noCodeField.setId("noCode");
        var lowCodeField = dynamicTextField();
        lowCodeField.setId("lowCode");
        var template = "{{ $.person.name }}";
        var authoredValues = new AuthoredElementValues();
        authoredValues.put("variable", new VariableAuthoredInputValue(
                new InputVariableReference(InputVariableSource.ProcessData, "template", null)
        ));
        authoredValues.put("noCode", new NoCodeAuthoredInputValue(NoCodeStaticValue.of(template)));
        authoredValues.put("lowCode", new LowCodeAuthoredInputValue("'{{ $.person.name }}'"));
        var processData = new ProcessExecutionData()
                .addProcessData("template", template)
                .addProcessData("person", Map.of("name", "Ada"));

        var result = derive(
                createRoot(List.of(variableField, noCodeField, lowCodeField)),
                authoredValues,
                new ElementDerivationOptions(),
                processData,
                InputModeEvaluationContext.Runtime
        );

        assertEquals(template, result.getEffectiveValues().get("variable"));
        assertEquals(template, result.getEffectiveValues().get("noCode"));
        assertEquals(template, result.getEffectiveValues().get("lowCode"));
    }

    @Test
    void shouldInterpolateDynamicLiteralTextInsideReplicatingRows() {
        var child = dynamicTextField();
        child.setId("message");
        var rows = new ReplicatingContainerLayoutElement();
        rows.setId("rows");
        rows.setChildren(List.of(child));
        var rowValues = new AuthoredElementValues();
        rowValues.putLiteral("message", "Hello {{ $.name }}");
        var authoredValues = new AuthoredElementValues();
        authoredValues.putLiteral("rows", List.of(
                new ReplicatingContainerLayoutElementValue().setId("row-1").setValues(rowValues)
        ));

        var result = derive(
                createRoot(List.of(rows)),
                authoredValues,
                new ElementDerivationOptions(),
                new ProcessExecutionData().addProcessData("name", "Ada"),
                InputModeEvaluationContext.Runtime
        );

        var effectiveRows = assertInstanceOf(List.class, result.getEffectiveValues().get("rows"));
        var effectiveRow = assertInstanceOf(EffectiveReplicatingContainerLayoutElementValue.class, effectiveRows.getFirst());
        assertEquals("Hello Ada", effectiveRow.getValues().get("message"));
    }

    @Test
    void shouldPreserveDeferredStateInsideReplicatingContainerRows() {
        var amount = dynamicNumberField();
        amount.setId("amount");
        var rows = new ReplicatingContainerLayoutElement();
        rows.setId("rows");
        rows.setChildren(List.of(amount));

        var rowValues = new AuthoredElementValues();
        rowValues.put("amount", new VariableAuthoredInputValue(
                new InputVariableReference(InputVariableSource.ProcessData, "order.amount", null)
        ));
        var authoredValues = new AuthoredElementValues();
        authoredValues.putLiteral("rows", List.of(
                new ReplicatingContainerLayoutElementValue().setId("row-1").setValues(rowValues)
        ));

        var result = derive(
                createRoot(List.of(rows)),
                authoredValues,
                new ElementDerivationOptions(),
                new ProcessExecutionData(),
                InputModeEvaluationContext.Authoring
        );

        var rowState = result.getElementStates().get("rows").getSubStates().getFirst();
        assertTrue(rowState.getStates().get("amount").isInputValueDeferred());
    }

    @Test
    void shouldReportUnavailableRuntimeVariablesInsteadOfFallingBack() {
        var field = dynamicNumberField();
        field.setValue(new ElementValueFunctions().setNoCode(NoCodeStaticValue.of(99)));
        var authoredValues = new AuthoredElementValues();
        authoredValues.put("field", new VariableAuthoredInputValue(
                new InputVariableReference(InputVariableSource.ProcessData, "missing.value", null)
        ));

        var result = derive(
                createRoot(List.of(field)),
                authoredValues,
                new ElementDerivationOptions(),
                new ProcessExecutionData(),
                InputModeEvaluationContext.Runtime
        );

        assertTrue(result.getElementStates().get("field").getError().contains("nicht verfügbar"));
        assertFalse(result.getEffectiveValues().containsKey("field"));
    }

    @Test
    void shouldNotResolveAnAuthoredDynamicValueWhenADisabledFieldDerivesItsValue() {
        var field = dynamicNumberField();
        field.setDisabled(true);
        field.setValue(new ElementValueFunctions().setNoCode(NoCodeStaticValue.of(99)));
        var authoredValues = new AuthoredElementValues();
        authoredValues.put("field", new VariableAuthoredInputValue(
                new InputVariableReference(InputVariableSource.ProcessData, "missing.value", null)
        ));

        var result = derive(
                createRoot(List.of(field)),
                authoredValues,
                new ElementDerivationOptions(),
                new ProcessExecutionData(),
                InputModeEvaluationContext.Runtime
        );

        assertEquals(99, ((Number) result.getEffectiveValues().get("field")).intValue());
        assertEquals(EffectiveValueSource.Derived, result.getElementStates().get("field").getValueSource());
        assertNull(result.getElementStates().get("field").getError());
    }

    @Test
    void shouldValidateAnExplicitlyResolvedNullAsAFieldValue() {
        var field = dynamicNumberField();
        field.setRequired(true);
        var authoredValues = new AuthoredElementValues();
        authoredValues.put("field", new VariableAuthoredInputValue(
                new InputVariableReference(InputVariableSource.ProcessData, "optional", null)
        ));
        var processData = new ProcessExecutionData().addProcessData("optional", null);

        var result = derive(
                createRoot(List.of(field)),
                authoredValues,
                new ElementDerivationOptions(),
                processData,
                InputModeEvaluationContext.Runtime
        );

        assertTrue(result.getEffectiveValues().containsKey("field"));
        assertNull(result.getEffectiveValues().get("field"));
        assertNotNull(result.getElementStates().get("field").getError());
    }

    @Test
    void shouldReportIncompatibleDynamicValuesAtRuntime() {
        var field = dynamicNumberField();
        var authoredValues = new AuthoredElementValues();
        authoredValues.put("field", new VariableAuthoredInputValue(
                new InputVariableReference(InputVariableSource.ProcessData, "person", null)
        ));
        var processData = new ProcessExecutionData().addProcessData("person", java.util.Map.of("name", "Ada"));

        var result = derive(
                createRoot(List.of(field)),
                authoredValues,
                new ElementDerivationOptions(),
                processData,
                InputModeEvaluationContext.Runtime
        );

        assertFalse(result.getEffectiveValues().containsKey("field"));
        assertTrue(result.getElementStates().get("field").getError().contains("nicht in den Typ"));
    }

    @Test
    void shouldDeriveNestedReplicatingContainersFromADynamicValue() {
        var detail = new TextInputElement();
        detail.setId("detail");
        var details = new ReplicatingContainerLayoutElement();
        details.setId("details");
        details.setChildren(List.of(detail));
        var name = new TextInputElement();
        name.setId("name");
        var rows = new ReplicatingContainerLayoutElement();
        rows.setId("rows");
        rows.setChildren(List.of(name, details));
        rows.setInputModePolicy(new InputModePolicy(
                List.of(InputMode.Literal, InputMode.NoCode),
                InputMode.Literal,
                List.of()
        ));
        var authoredValues = new AuthoredElementValues();
        authoredValues.put("rows", new NoCodeAuthoredInputValue(NoCodeStaticValue.of(List.of(Map.of(
                "id", "row-1",
                "values", Map.of(
                        "name", "Ada",
                        "details", List.of(Map.of(
                                "id", "detail-row-1",
                                "values", Map.of("detail", "nested")
                        ))
                )
        )))));

        var result = derive(
                createRoot(List.of(rows)),
                authoredValues,
                new ElementDerivationOptions(),
                new ProcessExecutionData(),
                InputModeEvaluationContext.Runtime
        );

        assertFalse(result.hasAnyError());
        var effectiveRows = assertInstanceOf(List.class, result.getEffectiveValues().get("rows"));
        var firstRow = assertInstanceOf(EffectiveReplicatingContainerLayoutElementValue.class, effectiveRows.getFirst());
        assertEquals("Ada", firstRow.getValues().get("name"));
        var effectiveDetails = assertInstanceOf(List.class, firstRow.getValues().get("details"));
        var firstDetail = assertInstanceOf(EffectiveReplicatingContainerLayoutElementValue.class, effectiveDetails.getFirst());
        assertEquals("nested", firstDetail.getValues().get("detail"));
    }

    @Test
    void shouldNotInterpolateTextReturnedInsideDynamicRows() {
        var message = dynamicTextField();
        message.setId("message");
        var details = new ReplicatingContainerLayoutElement();
        details.setId("details");
        details.setChildren(List.of(message));
        var rows = new ReplicatingContainerLayoutElement();
        rows.setId("rows");
        rows.setChildren(List.of(details));
        rows.setInputModePolicy(new InputModePolicy(List.of(InputMode.Literal, InputMode.Variable, InputMode.NoCode, InputMode.LowCode), null));
        var template = "{{ $.name }}";
        var rawRows = List.of(Map.of("details", List.of(Map.of("message", template))));
        var processData = new ProcessExecutionData().addProcessData("rows", rawRows).addProcessData("name", "Ada");

        for (var mode : List.of(
                new VariableAuthoredInputValue(new InputVariableReference(InputVariableSource.ProcessData, "rows", null)),
                new NoCodeAuthoredInputValue(NoCodeStaticValue.of(rawRows)),
                new LowCodeAuthoredInputValue("$.rows")
        )) {
            var values = new AuthoredElementValues();
            values.put("rows", mode);
            var result = derive(createRoot(List.of(rows)), values, new ElementDerivationOptions(), processData,
                    InputModeEvaluationContext.Runtime);

            assertFalse(result.hasAnyError());
            var effectiveRows = assertInstanceOf(List.class, result.getEffectiveValues().get("rows"));
            var row = assertInstanceOf(EffectiveReplicatingContainerLayoutElementValue.class, effectiveRows.getFirst());
            var effectiveDetails = assertInstanceOf(List.class, row.getValues().get("details"));
            var detail = assertInstanceOf(EffectiveReplicatingContainerLayoutElementValue.class, effectiveDetails.getFirst());
            assertEquals(template, detail.getValues().get("message"));
        }
    }

    @Test
    void shouldValidateEmptyDynamicRowsAsAnEmptyListRatherThanATypeMismatch() {
        var rows = new ReplicatingContainerLayoutElement();
        rows.setId("rows");
        rows.setChildren(List.of());
        rows.setInputModePolicy(new InputModePolicy(List.of(InputMode.Literal, InputMode.NoCode), null));
        var values = new AuthoredElementValues();
        values.put("rows", new NoCodeAuthoredInputValue(NoCodeStaticValue.of(List.of())));

        var optional = derive(createRoot(List.of(rows)), values, new ElementDerivationOptions(),
                new ProcessExecutionData(), InputModeEvaluationContext.Runtime);
        assertFalse(optional.hasAnyError());
        assertEquals(List.of(), optional.getEffectiveValues().get("rows"));

        rows.setRequired(true);
        var required = derive(createRoot(List.of(rows)), values, new ElementDerivationOptions(),
                new ProcessExecutionData(), InputModeEvaluationContext.Runtime);
        assertTrue(required.hasAnyError());
        assertFalse(required.getElementStates().get("rows").getError().contains("nicht in den Typ"));
    }

    @Test
    void shouldWrapRowsReturnedByExistingValueFunctionsBeforeDerivingChildren() {
        var name = new TextInputElement();
        name.setId("name");
        var rows = new ReplicatingContainerLayoutElement();
        rows.setId("rows");
        rows.setChildren(List.of(name));
        var rowData = List.of(Map.of("id", "row-1", "values", Map.of("name", "Ada")));

        for (var function : List.of(
                new ElementValueFunctions().setNoCode(NoCodeStaticValue.of(rowData)),
                new ElementValueFunctions().setJavascriptCode(JavascriptCode.of(
                        "[{id: 'row-1', values: {name: 'Ada'}}]"))
        )) {
            rows.setValue(function);
            var result = derive(createRoot(List.of(rows)), new AuthoredElementValues(), new ElementDerivationOptions());

            assertFalse(result.hasAnyError());
            var effectiveRows = assertInstanceOf(List.class, result.getEffectiveValues().get("rows"));
            var row = assertInstanceOf(EffectiveReplicatingContainerLayoutElementValue.class, effectiveRows.getFirst());
            assertEquals("row-1", row.getId());
            assertEquals("Ada", row.getValues().get("name"));
        }
    }

    @Test
    void shouldParseLowCodeWithoutExecutingItDuringAuthoring() {
        var field = dynamicNumberField();
        var authoredValues = new AuthoredElementValues();
        authoredValues.put("field", new LowCodeAuthoredInputValue("throw new Error('must not run')"));

        var result = derive(
                createRoot(List.of(field)),
                authoredValues,
                new ElementDerivationOptions(),
                new ProcessExecutionData(),
                InputModeEvaluationContext.Authoring
        );

        assertNull(result.getElementStates().get("field").getError());
        assertNull(result.getEffectiveValues().get("field"));
    }

    @Test
    void shouldProjectAuthoredValuesIntoRuntimeData() {
        var field = new TextInputElement();
        field.setId("field");

        var authoredValues = new AuthoredElementValues();
        authoredValues.putLiteral("field", "hello");

        var result = derive(
                createRoot(List.of(field)),
                authoredValues,
                new ElementDerivationOptions()
        );

        assertEquals("hello", result.getEffectiveValues().get("field"));
        assertNull(result.getEffectiveValues().get("root"));
        assertNull(result.getEffectiveValues().get("step"));

        var fieldState = result.getElementStates().get("field");
        assertNotNull(fieldState);
        assertTrue(fieldState.getVisible());
        assertNull(fieldState.getError());
        assertEquals(EffectiveValueSource.Authored, fieldState.getValueSource());
    }

    @Test
    void shouldMarkComputedValuesAsDerived() {
        var sourceField = new TextInputElement();
        sourceField.setId("source");

        var derivedField = new TextInputElement();
        derivedField.setId("derived");
        derivedField.setValue(new ElementValueFunctions().setNoCode(NoCodeReference.of("source")));

        var authoredValues = new AuthoredElementValues();
        authoredValues.putLiteral("source", "copied value");

        var result = derive(
                createRoot(List.of(sourceField, derivedField)),
                authoredValues,
                new ElementDerivationOptions()
        );

        assertEquals("copied value", result.getEffectiveValues().get("source"));
        assertEquals("copied value", result.getEffectiveValues().get("derived"));
        assertEquals(
                EffectiveValueSource.Derived,
                result.getElementStates().get("derived").getValueSource()
        );
    }

    @Test
    void shouldFormatNoCodeDateDerivedTextValue() {
        var derivedField = new TextInputElement();
        derivedField.setId("derived");
        derivedField.setValue(new ElementValueFunctions().setNoCode(
                NoCodeExpression.of(
                        "add-to-date",
                        NoCodeExpression.of(
                                "create-date",
                                NoCodeStaticValue.of(7),
                                NoCodeStaticValue.of(8),
                                NoCodeStaticValue.of(2026)
                        ),
                        NoCodeStaticValue.of(2),
                        NoCodeStaticValue.of("jahre")
                )
        ));

        var result = derive(
                createRoot(List.of(derivedField)),
                new AuthoredElementValues(),
                new ElementDerivationOptions()
        );

        assertEquals("07.08.2028", result.getEffectiveValues().get("derived"));
        assertEquals(
                EffectiveValueSource.Derived,
                result.getElementStates().get("derived").getValueSource()
        );
    }

    @Test
    void shouldSkipValidationErrorsForTechnicalFieldsButKeepEffectiveValues() {
        var technicalField = new TextInputElement();
        technicalField.setId("technical");
        technicalField.setTechnical(true);
        technicalField.setValue(new ElementValueFunctions().setNoCode(NoCodeStaticValue.of("derived value")));
        technicalField.setValidation(new ElementValidationFunctions().setNoCodeList(List.of(
                new ValidationNoCodeWrapper()
                        .setNoCode(NoCodeStaticValue.of(false))
                        .setMessage("Technical validation should be skipped.")
        )));

        var result = derive(
                createRoot(List.of(technicalField)),
                new AuthoredElementValues(),
                new ElementDerivationOptions()
        );

        assertEquals("derived value", result.getEffectiveValues().get("technical"));
        assertNull(result.getElementStates().get("technical").getError());
        assertEquals(
                EffectiveValueSource.Derived,
                result.getElementStates().get("technical").getValueSource()
        );
    }

    @Test
    void shouldComputeJavascriptValuesFromTheProcessDataOfTheExecution() {
        var technicalField = new TextInputElement();
        technicalField.setId("technical");
        technicalField.setTechnical(true);
        technicalField.setValue(new ElementValueFunctions().setJavascriptCode(JavascriptCode.of(
                "(function () { return $.approvedAmount ?? null; })();")));

        var result = derive(
                createRoot(List.of(technicalField)),
                new AuthoredElementValues(),
                new ElementDerivationOptions(),
                new ProcessExecutionData().addProcessData("approvedAmount", "100"),
                InputModeEvaluationContext.LiteralOnly
        );

        assertFalse(result.hasAnyError());
        assertEquals("100", result.getEffectiveValues().get("technical"));
    }

    @Test
    void shouldIgnoreCustomerValuesForTechnicalFieldsWithValueFunction() {
        var technicalField = new TextInputElement();
        technicalField.setId("fee");
        technicalField.setTechnical(true);
        technicalField.setValue(new ElementValueFunctions().setNoCode(NoCodeStaticValue.of("25")));
        var authoredValues = new AuthoredElementValues();
        authoredValues.putLiteral("fee", "0");

        var result = derive(
                createRoot(List.of(technicalField)),
                authoredValues,
                new ElementDerivationOptions().setIgnoreAuthoredValuesOfNonEditableInputs(true)
        );

        assertEquals("25", result.getEffectiveValues().get("fee"));
        assertEquals(EffectiveValueSource.Derived, result.getElementStates().get("fee").getValueSource());
    }

    @Test
    void shouldIgnoreCustomerValuesForNonEditableFieldsWithoutValueFunction() {
        var technicalField = new TextInputElement();
        technicalField.setId("route");
        technicalField.setTechnical(true);
        var disabledField = new TextInputElement();
        disabledField.setId("amount");
        disabledField.setDisabled(true);
        var editableField = new TextInputElement();
        editableField.setId("name");
        var authoredValues = new AuthoredElementValues();
        authoredValues.putLiteral("route", "forged");
        authoredValues.putLiteral("amount", "1");
        authoredValues.putLiteral("name", "Erika");

        var result = derive(
                createRoot(List.of(technicalField, disabledField, editableField)),
                authoredValues,
                new ElementDerivationOptions().setIgnoreAuthoredValuesOfNonEditableInputs(true)
        );

        assertNull(result.getEffectiveValues().get("route"));
        assertNull(result.getEffectiveValues().get("amount"));
        assertEquals("Erika", result.getEffectiveValues().get("name"));
    }

    @Test
    void shouldIgnoreCustomerValuesForNonEditableFieldsEvenWhenTheirValuesAreSkipped() {
        var disabledField = new TextInputElement();
        disabledField.setId("amount");
        disabledField.setDisabled(true);
        var authoredValues = new AuthoredElementValues();
        authoredValues.putLiteral("amount", "1");

        var result = derive(
                createRoot(List.of(disabledField)),
                authoredValues,
                new ElementDerivationOptions()
                        .setSkipValuesForElementIds(List.of("amount"))
                        .setIgnoreAuthoredValuesOfNonEditableInputs(true)
        );

        assertNull(result.getEffectiveValues().get("amount"));
    }

    @Test
    void shouldIgnoreCustomerValuesForNonEditableFieldsInsideCustomerRows() {
        var fee = new TextInputElement();
        fee.setId("fee");
        fee.setTechnical(true);
        var name = new TextInputElement();
        name.setId("name");
        var rows = new ReplicatingContainerLayoutElement();
        rows.setId("rows");
        rows.setChildren(List.of(fee, name));
        var rowValues = new AuthoredElementValues();
        rowValues.putLiteral("fee", "0");
        rowValues.putLiteral("name", "Ada");
        var authoredValues = new AuthoredElementValues();
        authoredValues.putLiteral("rows", List.of(
                new ReplicatingContainerLayoutElementValue().setId("row-1").setValues(rowValues)
        ));

        var result = derive(
                createRoot(List.of(rows)),
                authoredValues,
                new ElementDerivationOptions().setIgnoreAuthoredValuesOfNonEditableInputs(true)
        );

        var effectiveRows = assertInstanceOf(List.class, result.getEffectiveValues().get("rows"));
        var row = assertInstanceOf(EffectiveReplicatingContainerLayoutElementValue.class, effectiveRows.getFirst());
        assertNull(row.getValues().get("fee"));
        assertEquals("Ada", row.getValues().get("name"));
    }

    @Test
    void shouldKeepValuesOfNonEditableFieldsInsideRowsComputedByTheServer() {
        var fee = new TextInputElement();
        fee.setId("fee");
        fee.setTechnical(true);
        var rows = new ReplicatingContainerLayoutElement();
        rows.setId("rows");
        rows.setChildren(List.of(fee));
        rows.setValue(new ElementValueFunctions().setNoCode(NoCodeStaticValue.of(
                List.of(Map.of("id", "row-1", "values", Map.of("fee", "25")))
        )));

        var result = derive(
                createRoot(List.of(rows)),
                new AuthoredElementValues(),
                new ElementDerivationOptions().setIgnoreAuthoredValuesOfNonEditableInputs(true)
        );

        assertFalse(result.hasAnyError());
        var effectiveRows = assertInstanceOf(List.class, result.getEffectiveValues().get("rows"));
        var row = assertInstanceOf(EffectiveReplicatingContainerLayoutElementValue.class, effectiveRows.getFirst());
        assertEquals("25", row.getValues().get("fee"));
    }

    @Test
    void shouldMatchNumberFieldValuesInNoCodeEqualityThroughProcessDataAndFieldReferences() {
        var amount = new NumberInputElement();
        amount.setId("amount");
        amount.setDestinationKey("antrag.betrag");
        amount.setDecimalPlaces(2);
        var byProcessData = createVisibleIfEquals("byProcessData", new NoCodeProcessDataReference("antrag.betrag"), "5.5");
        var byField = createVisibleIfEquals("byField", new NoCodeReference("amount"), "5.5");
        var otherAmount = createVisibleIfEquals("otherAmount", new NoCodeProcessDataReference("antrag.betrag"), "6");
        var constantFirst = new TextInputElement();
        constantFirst.setId("constantFirst");
        constantFirst.setVisibility(ElementVisibilityFunctions.of(NoCodeExpression.of(
                "equals",
                NoCodeStaticValue.of("5.5"),
                new NoCodeProcessDataReference("antrag.betrag")
        )));
        var authoredValues = new AuthoredElementValues();
        authoredValues.putLiteral("amount", 5.5);

        var result = derive(
                createRoot(List.of(amount, byProcessData, byField, otherAmount, constantFirst)),
                authoredValues,
                new ElementDerivationOptions()
        );

        assertTrue(result.getElementStates().get("byProcessData").getVisible());
        assertTrue(result.getElementStates().get("byField").getVisible());
        assertFalse(result.getElementStates().get("otherAmount").getVisible());
        assertTrue(result.getElementStates().get("constantFirst").getVisible());
    }

    @Test
    void shouldMarkOnlyVisibleInputsWithoutComputedValueAsAdoptingTheirAuthoredValue() {
        var visibleField = new TextInputElement();
        visibleField.setId("visible");
        var hiddenField = new TextInputElement();
        hiddenField.setId("hidden");
        hiddenField.setVisibility(new ElementVisibilityFunctions().setNoCode(NoCodeStaticValue.of(false)));
        var computedField = new TextInputElement();
        computedField.setId("computed");
        computedField.setValue(new ElementValueFunctions().setNoCode(NoCodeStaticValue.of("computed value")));
        var authoredValues = new AuthoredElementValues();
        authoredValues.putLiteral("visible", "visible value");
        authoredValues.putLiteral("hidden", "hidden value");

        var result = derive(
                createRoot(List.of(visibleField, hiddenField, computedField)),
                authoredValues,
                new ElementDerivationOptions()
        );

        assertTrue(result.getElementStates().get("visible").isAuthoredValueAdopted());
        assertFalse(result.getElementStates().get("hidden").isAuthoredValueAdopted());
        assertFalse(result.getElementStates().get("computed").isAuthoredValueAdopted());
    }

    @Test
    void shouldMarkNonEditableInputsAsAdoptingTheirAuthoredValueOnlyOutsideCustomerInput() {
        var disabledField = new TextInputElement();
        disabledField.setId("disabled");
        disabledField.setDisabled(true);
        var technicalField = new TextInputElement();
        technicalField.setId("technical");
        technicalField.setTechnical(true);
        var editableField = new TextInputElement();
        editableField.setId("editable");
        var root = createRoot(List.of(disabledField, technicalField, editableField));
        var authoredValues = new AuthoredElementValues();
        authoredValues.putLiteral("disabled", "1");
        authoredValues.putLiteral("technical", "2");
        authoredValues.putLiteral("editable", "3");

        var customerResult = derive(
                root,
                authoredValues,
                new ElementDerivationOptions().setIgnoreAuthoredValuesOfNonEditableInputs(true)
        );
        var staffResult = derive(root, authoredValues, new ElementDerivationOptions());

        assertFalse(customerResult.getElementStates().get("disabled").isAuthoredValueAdopted());
        assertFalse(customerResult.getElementStates().get("technical").isAuthoredValueAdopted());
        assertTrue(customerResult.getElementStates().get("editable").isAuthoredValueAdopted());
        assertTrue(staffResult.getElementStates().get("disabled").isAuthoredValueAdopted());
        assertTrue(staffResult.getElementStates().get("technical").isAuthoredValueAdopted());
    }

    @Test
    void shouldMarkInputsInRowsAsAdoptingTheirAuthoredValueOnlyWhenTheRowsAreAuthored() {
        var authoredRowsName = new TextInputElement();
        authoredRowsName.setId("authoredName");
        var authoredRows = new ReplicatingContainerLayoutElement();
        authoredRows.setId("authoredRows");
        authoredRows.setChildren(List.of(authoredRowsName));

        var computedRowsName = new TextInputElement();
        computedRowsName.setId("computedName");
        var computedRows = new ReplicatingContainerLayoutElement();
        computedRows.setId("computedRows");
        computedRows.setChildren(List.of(computedRowsName));
        computedRows.setValue(new ElementValueFunctions().setNoCode(NoCodeStaticValue.of(
                List.of(Map.of("id", "row-1", "values", Map.of("computedName", "Ada")))
        )));

        // The name is editable, but the rows of the disabled container come from the trusted values.
        var protectedRowsName = new TextInputElement();
        protectedRowsName.setId("protectedName");
        var protectedRows = new ReplicatingContainerLayoutElement();
        protectedRows.setId("protectedRows");
        protectedRows.setDisabled(true);
        protectedRows.setChildren(List.of(protectedRowsName));

        var authoredValues = new AuthoredElementValues();
        authoredValues.putLiteral("authoredRows", List.of(rowValue("row-1", "authoredName", "Erika")));
        authoredValues.putLiteral("protectedRows", List.of(rowValue("row-1", "protectedName", "forged")));
        var trustedValues = new AuthoredElementValues();
        trustedValues.putLiteral("protectedRows", List.of(rowValue("row-1", "protectedName", "Grace")));

        var result = derive(
                createRoot(List.of(authoredRows, computedRows, protectedRows)),
                authoredValues,
                new ElementDerivationOptions()
                        .setIgnoreAuthoredValuesOfNonEditableInputs(true)
                        .setTrustedAuthoredValues(trustedValues)
        );

        assertTrue(result.getElementStates().get("authoredRows").getSubStates().getFirst().getStates()
                .get("authoredName").isAuthoredValueAdopted());
        assertFalse(result.getElementStates().get("computedRows").getSubStates().getFirst().getStates()
                .get("computedName").isAuthoredValueAdopted());
        assertFalse(result.getElementStates().get("protectedRows").isAuthoredValueAdopted());
        assertFalse(result.getElementStates().get("protectedRows").getSubStates().getFirst().getStates()
                .get("protectedName").isAuthoredValueAdopted());
    }

    @Test
    void shouldTakeValuesOfNonEditableFieldsFromTrustedValues() {
        var technicalField = new TextInputElement();
        technicalField.setId("route");
        technicalField.setTechnical(true);
        var disabledField = new TextInputElement();
        disabledField.setId("amount");
        disabledField.setDisabled(true);
        var untrustedField = new TextInputElement();
        untrustedField.setId("fee");
        untrustedField.setTechnical(true);
        var editableField = new TextInputElement();
        editableField.setId("name");
        var authoredValues = new AuthoredElementValues();
        authoredValues.putLiteral("route", "forged");
        authoredValues.putLiteral("amount", "1");
        authoredValues.putLiteral("fee", "0");
        authoredValues.putLiteral("name", "Erika");
        var trustedValues = new AuthoredElementValues();
        trustedValues.putLiteral("route", "internal");
        trustedValues.putLiteral("amount", "100");
        trustedValues.putLiteral("name", "Ada");

        var result = derive(
                createRoot(List.of(technicalField, disabledField, untrustedField, editableField)),
                authoredValues,
                new ElementDerivationOptions()
                        .setIgnoreAuthoredValuesOfNonEditableInputs(true)
                        .setTrustedAuthoredValues(trustedValues)
        );

        assertEquals("internal", result.getEffectiveValues().get("route"));
        assertEquals("100", result.getEffectiveValues().get("amount"));
        assertNull(result.getEffectiveValues().get("fee"));
        assertEquals("Erika", result.getEffectiveValues().get("name"));
    }

    @Test
    void shouldMatchTrustedValuesOfNonEditableFieldsInRowsByRowId() {
        var amount = new TextInputElement();
        amount.setId("amount");
        amount.setDisabled(true);
        var note = new TextInputElement();
        note.setId("note");
        var rows = new ReplicatingContainerLayoutElement();
        rows.setId("rows");
        rows.setChildren(List.of(amount, note));
        var trustedValues = new AuthoredElementValues();
        trustedValues.putLiteral("rows", List.of(
                rowValue("row-1", "amount", "100"),
                rowValue("row-2", "amount", "5")
        ));
        // The customer removed the first row, changed the amount of the second one and added a row.
        var authoredValues = new AuthoredElementValues();
        authoredValues.putLiteral("rows", List.of(
                rowValue("row-2", "amount", "999", "note", "B"),
                rowValue("row-new", "amount", "7", "note", "C")
        ));

        var result = derive(
                createRoot(List.of(rows)),
                authoredValues,
                new ElementDerivationOptions()
                        .setIgnoreAuthoredValuesOfNonEditableInputs(true)
                        .setTrustedAuthoredValues(trustedValues)
        );

        var effectiveRows = assertInstanceOf(List.class, result.getEffectiveValues().get("rows"));
        var keptRow = assertInstanceOf(EffectiveReplicatingContainerLayoutElementValue.class, effectiveRows.get(0));
        assertEquals("row-2", keptRow.getId());
        assertEquals("5", keptRow.getValues().get("amount"));
        assertEquals("B", keptRow.getValues().get("note"));
        var addedRow = assertInstanceOf(EffectiveReplicatingContainerLayoutElementValue.class, effectiveRows.get(1));
        assertNull(addedRow.getValues().get("amount"));
        assertEquals("C", addedRow.getValues().get("note"));
    }

    @Test
    void shouldGiveTrustedRowValuesOnlyToTheFirstRowWithTheRowId() {
        var amount = new TextInputElement();
        amount.setId("amount");
        amount.setDisabled(true);
        var note = new TextInputElement();
        note.setId("note");
        var rows = new ReplicatingContainerLayoutElement();
        rows.setId("rows");
        rows.setChildren(List.of(amount, note));
        var trustedValues = new AuthoredElementValues();
        trustedValues.putLiteral("rows", List.of(
                rowValue("row-1", "amount", "100"),
                rowValue("row-2", "amount", "5")
        ));
        // The customer sent the ID of the first row for the second row to repeat its amount.
        var authoredValues = new AuthoredElementValues();
        authoredValues.putLiteral("rows", List.of(
                rowValue("row-1", "amount", "100", "note", "A"),
                rowValue("row-1", "amount", "100", "note", "B")
        ));

        var result = derive(
                createRoot(List.of(rows)),
                authoredValues,
                new ElementDerivationOptions()
                        .setIgnoreAuthoredValuesOfNonEditableInputs(true)
                        .setTrustedAuthoredValues(trustedValues)
        );

        var effectiveRows = assertInstanceOf(List.class, result.getEffectiveValues().get("rows"));
        var firstRow = assertInstanceOf(EffectiveReplicatingContainerLayoutElementValue.class, effectiveRows.get(0));
        assertEquals("100", firstRow.getValues().get("amount"));
        assertEquals("A", firstRow.getValues().get("note"));
        var copiedRow = assertInstanceOf(EffectiveReplicatingContainerLayoutElementValue.class, effectiveRows.get(1));
        assertNull(copiedRow.getValues().get("amount"));
        assertEquals("B", copiedRow.getValues().get("note"));
    }

    @Test
    void shouldNotReadCustomerInputProtectionFromJson() throws Exception {
        var options = JsonMapperTestUtils.createMapper().readValue(
                "{\"ignoreAuthoredValuesOfNonEditableInputs\":true,\"trustedAuthoredValues\":{}}",
                ElementDerivationOptions.class
        );

        assertFalse(options.isIgnoreAuthoredValuesOfNonEditableInputs());
        assertNull(options.getTrustedAuthoredValues());
    }

    @Test
    void shouldKeepAuthoredValuesForTechnicalFieldsWithoutCustomerInputProtection() {
        var technicalField = new TextInputElement();
        technicalField.setId("route");
        technicalField.setTechnical(true);
        var authoredValues = new AuthoredElementValues();
        authoredValues.putLiteral("route", "stored");

        var result = derive(
                createRoot(List.of(technicalField)),
                authoredValues,
                new ElementDerivationOptions()
        );

        assertEquals("stored", result.getEffectiveValues().get("route"));
    }

    @Test
    void shouldPassCustomerInputProtectionToChildElements() {
        var trustedValues = new AuthoredElementValues();
        var options = new ElementDerivationOptions()
                .setIgnoreAuthoredValuesOfNonEditableInputs(true)
                .setTrustedAuthoredValues(trustedValues);

        var childOptions = options.copyForUseInChild("container");

        assertTrue(childOptions.isIgnoreAuthoredValuesOfNonEditableInputs());
        assertSame(trustedValues, childOptions.getTrustedAuthoredValues());
    }

    @Test
    void shouldKeepEmptyNoCodeValidationMessageAsErrorMarker() {
        var field = new TextInputElement();
        field.setId("field");
        field.setValidation(new ElementValidationFunctions().setNoCodeList(List.of(
                new ValidationNoCodeWrapper()
                        .setNoCode(NoCodeStaticValue.of(false))
                        .setMessage("")
        )));

        var authoredValues = new AuthoredElementValues();
        authoredValues.putLiteral("field", "invalid");

        var result = derive(
                createRoot(List.of(field)),
                authoredValues,
                new ElementDerivationOptions()
        );

        assertEquals("", result.getElementStates().get("field").getError());
    }

    @Test
    void shouldNormalizeNullNoCodeValidationMessageToEmptyErrorMarker() {
        var field = new TextInputElement();
        field.setId("field");
        field.setValidation(new ElementValidationFunctions().setNoCodeList(List.of(
                new ValidationNoCodeWrapper()
                        .setNoCode(NoCodeStaticValue.of(false))
        )));

        var authoredValues = new AuthoredElementValues();
        authoredValues.putLiteral("field", "invalid");

        var result = derive(
                createRoot(List.of(field)),
                authoredValues,
                new ElementDerivationOptions()
        );

        assertEquals("", result.getElementStates().get("field").getError());
    }

    @Test
    void shouldTreatPresentNullAsAuthoredClearInsteadOfDerivingValue() {
        var field = new TextInputElement();
        field.setId("field");
        field.setValue(new ElementValueFunctions().setNoCode(NoCodeStaticValue.of("derived value")));

        var authoredValues = new AuthoredElementValues();
        authoredValues.putLiteral("field", null);

        var result = derive(
                createRoot(List.of(field)),
                authoredValues,
                new ElementDerivationOptions()
        );

        assertTrue(result.getEffectiveValues().containsKey("field"));
        assertNull(result.getEffectiveValues().get("field"));
        assertEquals(
                EffectiveValueSource.Authored,
                result.getElementStates().get("field").getValueSource()
        );
    }

    @Test
    void shouldSkipVisibilitiesForSkippedElementsAndChildren() {
        var child = new TextInputElement();
        child.setId("child");
        child.setVisibility(new ElementVisibilityFunctions().setNoCode(NoCodeStaticValue.of(false)));

        var group = new GroupLayoutElement();
        group.setId("group");
        group.setVisibility(new ElementVisibilityFunctions().setNoCode(NoCodeStaticValue.of(false)));
        group.setChildren(new LinkedList<>(List.of(child)));

        var baselineResult = derive(
                createRoot(List.of(group)),
                new AuthoredElementValues(),
                new ElementDerivationOptions()
        );
        assertFalse(baselineResult.getElementStates().get("group").getVisible());
        assertFalse(baselineResult.getElementStates().get("child").getVisible());

        var skippedResult = derive(
                createRoot(List.of(group)),
                new AuthoredElementValues(),
                new ElementDerivationOptions().setSkipVisibilitiesForElementIds(List.of("group"))
        );

        assertTrue(skippedResult.getElementStates().get("group").getVisible());
        assertTrue(skippedResult.getElementStates().get("child").getVisible());
    }

    @Test
    void shouldSkipOverridesForChildrenOfSkippedElements() {
        var child = new TextInputElement();
        child.setId("child");
        child.setLabel("original");
        child.setOverride(new ElementOverrideFunctions().setJavascriptCode(
                JavascriptCode.of("({ type: element.type, id: element.id, label: 'overridden child' })")
        ));

        var group = new GroupLayoutElement();
        group.setId("group");
        group.setChildren(new LinkedList<>(List.of(child)));

        var baselineResult = derive(
                createRoot(List.of(group)),
                new AuthoredElementValues(),
                new ElementDerivationOptions()
        );
        var overrideElement = assertInstanceOf(
                TextInputElement.class,
                baselineResult.getElementStates().get("child").getOverride()
        );
        assertEquals("overridden child", overrideElement.getLabel());

        var skippedResult = derive(
                createRoot(List.of(group)),
                new AuthoredElementValues(),
                new ElementDerivationOptions().setSkipOverridesForElementIds(List.of("group"))
        );

        assertNull(skippedResult.getElementStates().get("child").getOverride());
    }

    @Test
    void shouldSkipValuesForChildrenOfSkippedElements() {
        var child = new TextInputElement();
        child.setId("child");
        child.setDisabled(true);
        child.setValue(new ElementValueFunctions().setNoCode(NoCodeStaticValue.of("derived value")));

        var group = new GroupLayoutElement();
        group.setId("group");
        group.setChildren(new LinkedList<>(List.of(child)));

        var authoredValues = new AuthoredElementValues();
        authoredValues.putLiteral("child", "authored value");

        var baselineResult = derive(
                createRoot(List.of(group)),
                authoredValues,
                new ElementDerivationOptions()
        );
        assertEquals("derived value", baselineResult.getEffectiveValues().get("child"));

        var skippedResult = derive(
                createRoot(List.of(group)),
                authoredValues,
                new ElementDerivationOptions().setSkipValuesForElementIds(List.of("group"))
        );

        assertEquals("authored value", skippedResult.getEffectiveValues().get("child"));
        assertEquals(
                EffectiveValueSource.Authored,
                skippedResult.getElementStates().get("child").getValueSource()
        );
    }

    @Test
    void shouldSkipErrorsForChildrenOfSkippedElements() {
        var child = new TextInputElement();
        child.setId("child");
        child.setRequired(true);

        var group = new GroupLayoutElement();
        group.setId("group");
        group.setChildren(new LinkedList<>(List.of(child)));

        var baselineResult = derive(
                createRoot(List.of(group)),
                new AuthoredElementValues(),
                new ElementDerivationOptions()
        );
        assertEquals(
                "Dieses Feld ist ein Pflichtfeld und darf nicht leer sein.",
                baselineResult.getElementStates().get("child").getError()
        );

        var skippedResult = derive(
                createRoot(List.of(group)),
                new AuthoredElementValues(),
                new ElementDerivationOptions().setSkipErrorsForElementIds(List.of("group"))
        );

        assertNull(skippedResult.getElementStates().get("child").getError());
    }

    @Test
    void shouldSkipErrorsForChildrenInsideSummaryLayout() {
        var summaryChild = new TextInputElement();
        summaryChild.setId("summaryChild");
        summaryChild.setRequired(true);

        var summary = new SummaryLayoutElement();
        summary.setId("summary");
        summary.setChildren(List.of(summaryChild));

        var regularChild = new TextInputElement();
        regularChild.setId("regularChild");
        regularChild.setRequired(true);

        var result = derive(
                createRoot(List.of(summary, regularChild)),
                new AuthoredElementValues(),
                new ElementDerivationOptions()
        );

        assertNull(result.getElementStates().get("summaryChild").getError());
        assertEquals(
                "Dieses Feld ist ein Pflichtfeld und darf nicht leer sein.",
                result.getElementStates().get("regularChild").getError()
        );
    }

    @Test
    void shouldSkipErrorsForNestedSummaryLayoutDescendants() {
        var nestedChild = new TextInputElement();
        nestedChild.setId("nestedChild");
        nestedChild.setRequired(true);

        var group = new GroupLayoutElement();
        group.setId("group");
        group.setChildren(new LinkedList<>(List.of(nestedChild)));

        var summary = new SummaryLayoutElement();
        summary.setId("summary");
        summary.setChildren(List.of(group));

        var result = derive(
                createRoot(List.of(summary)),
                new AuthoredElementValues(),
                new ElementDerivationOptions()
        );

        assertNull(result.getElementStates().get("nestedChild").getError());
    }

    @Test
    void shouldClearDependentSelectValueWhenParentSelectIsInOuterScope() {
        var parent = createGroupedSelect("parent", null, List.of(
                SelectInputElementOption.of("group_a", "Gruppe A"),
                SelectInputElementOption.of("group_b", "Gruppe B")
        ));

        var child = createGroupedSelect("child", "parent", List.of(
                SelectInputElementOption.of("option_a", "Option A", "group_a"),
                SelectInputElementOption.of("option_b", "Option B", "group_b")
        ));

        var rows = new ReplicatingContainerLayoutElement();
        rows.setId("rows");
        rows.setChildren(new LinkedList<>(List.of(child)));

        var rowValues = new AuthoredElementValues();
        rowValues.putLiteral("child", "option_a");

        var authoredValues = new AuthoredElementValues();
        authoredValues.putLiteral("parent", "group_b");
        authoredValues.putLiteral("rows", List.of(new ReplicatingContainerLayoutElementValue().setId("row-1").setValues(rowValues)));

        var result = derive(
                createRoot(List.of(parent, rows)),
                authoredValues,
                new ElementDerivationOptions()
        );

        var effectiveRows = assertInstanceOf(List.class, result.getEffectiveValues().get("rows"));
        var firstRow = assertInstanceOf(EffectiveReplicatingContainerLayoutElementValue.class, effectiveRows.get(0));

        assertEquals("group_b", result.getEffectiveValues().get("parent"));
        assertEquals("row-1", firstRow.getId());
        assertNull(firstRow.getValues().get("child"));
        assertEquals("row-1", result.getElementStates().get("rows").getSubStates().getFirst().getId());
        assertNull(result.getElementStates().get("rows").getSubStates().getFirst().getStates().get("child").getError());
    }

    @Test
    void shouldClearDependentSelectValueWhenParentSelectIsInCurrentReplicatingRow() {
        var rowParent = createGroupedSelect("row_parent", null, List.of(
                SelectInputElementOption.of("group_a", "Gruppe A"),
                SelectInputElementOption.of("group_b", "Gruppe B")
        ));

        var rowChild = createGroupedSelect("row_child", "row_parent", List.of(
                SelectInputElementOption.of("option_a", "Option A", "group_a"),
                SelectInputElementOption.of("option_b", "Option B", "group_b")
        ));

        var rows = new ReplicatingContainerLayoutElement();
        rows.setId("rows");
        rows.setChildren(new LinkedList<>(List.of(rowParent, rowChild)));

        var rowValues = new AuthoredElementValues();
        rowValues.putLiteral("row_parent", "group_b");
        rowValues.putLiteral("row_child", "option_a");

        var authoredValues = new AuthoredElementValues();
        authoredValues.putLiteral("rows", List.of(new ReplicatingContainerLayoutElementValue().setValues(rowValues)));

        var result = derive(
                createRoot(List.of(rows)),
                authoredValues,
                new ElementDerivationOptions()
        );

        var effectiveRows = assertInstanceOf(List.class, result.getEffectiveValues().get("rows"));
        var firstRow = assertInstanceOf(EffectiveReplicatingContainerLayoutElementValue.class, effectiveRows.get(0));

        assertEquals("group_b", firstRow.getValues().get("row_parent"));
        assertNull(firstRow.getValues().get("row_child"));
        assertNull(result.getElementStates().get("rows").getSubStates().getFirst().getStates().get("row_child").getError());
    }

    @Test
    void shouldNotFallBackToRootValueWhenReplicatingRowParentIsExplicitlyCleared() {
        var rowParent = createGroupedSelect("row_parent", null, List.of(
                SelectInputElementOption.of("group_a", "Gruppe A")
        ));

        var rowChild = createGroupedSelect("row_child", "row_parent", List.of(
                SelectInputElementOption.of("option_a", "Option A", "group_a")
        ));

        var rows = new ReplicatingContainerLayoutElement();
        rows.setId("rows");
        rows.setChildren(new LinkedList<>(List.of(rowParent, rowChild)));

        var rowValues = new AuthoredElementValues();
        rowValues.putLiteral("row_parent", null);
        rowValues.putLiteral("row_child", "option_a");

        var authoredValues = new AuthoredElementValues();
        authoredValues.putLiteral("row_parent", "group_a");
        authoredValues.putLiteral("rows", List.of(new ReplicatingContainerLayoutElementValue().setValues(rowValues)));

        var result = derive(
                createRoot(List.of(rows)),
                authoredValues,
                new ElementDerivationOptions()
        );

        var effectiveRows = assertInstanceOf(List.class, result.getEffectiveValues().get("rows"));
        var firstRow = assertInstanceOf(EffectiveReplicatingContainerLayoutElementValue.class, effectiveRows.get(0));

        assertNull(firstRow.getValues().get("row_parent"));
        assertNull(firstRow.getValues().get("row_child"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldIgnoreRowValuesUnderTheIdOfAReferencedSelectOutsideTheRow(boolean protectedParent) {
        var parent = createGroupSelect("parent");
        parent.setDisabled(protectedParent);
        var rows = createRows("rows", createGroupOptionSelect("child", "parent"));
        var authoredValues = new AuthoredElementValues();
        authoredValues.putLiteral("parent", "group_b");
        // The row claims a value for the select on the form level to make the option of group A valid.
        authoredValues.putLiteral("rows", List.of(rowValue("row-1", "child", "option_a", "parent", "group_a")));
        var trustedValues = new AuthoredElementValues();
        trustedValues.putLiteral("parent", "group_b");

        var result = derive(
                createRoot(List.of(parent, rows)),
                authoredValues,
                new ElementDerivationOptions()
                        .setIgnoreAuthoredValuesOfNonEditableInputs(true)
                        .setTrustedAuthoredValues(trustedValues)
        );

        assertEquals("group_b", result.getEffectiveValues().get("parent"));
        assertNull(effectiveRow(result.getEffectiveValues(), "rows", 0).getValues().get("child"));
    }

    @Test
    void shouldCheckDependentSelectsInNestedRowsAgainstTheParentRowAndTheFormLevel() {
        var parent = createGroupSelect("parent");
        var innerRows = createRows("inner_rows",
                createGroupOptionSelect("row_child", "row_parent"),
                createGroupOptionSelect("form_child", "parent"));
        var outerRows = createRows("outer_rows", createGroupSelect("row_parent"), innerRows);
        var authoredValues = new AuthoredElementValues();
        authoredValues.putLiteral("parent", "group_b");
        authoredValues.putLiteral("outer_rows", List.of(
                outerRowValue("outer-1", "group_a", rowValue("inner-1", "row_child", "option_a", "form_child", "option_b")),
                outerRowValue("outer-2", "group_b", rowValue("inner-2", "row_child", "option_a", "form_child", "option_a"))
        ));

        var result = derive(createRoot(List.of(parent, outerRows)), authoredValues, new ElementDerivationOptions());

        var matchingRow = effectiveRow(effectiveRow(result.getEffectiveValues(), "outer_rows", 0).getValues(), "inner_rows", 0);
        assertEquals("option_a", matchingRow.getValues().get("row_child"));
        assertEquals("option_b", matchingRow.getValues().get("form_child"));
        var mismatchingRow = effectiveRow(effectiveRow(result.getEffectiveValues(), "outer_rows", 1).getValues(), "inner_rows", 0);
        assertNull(mismatchingRow.getValues().get("row_child"));
        assertNull(mismatchingRow.getValues().get("form_child"));
    }

    @Test
    void shouldNotResolveReferencedSelectsInRowsOfAnotherContainer() {
        var otherRows = createRows("other_rows", createGroupSelect("parent"));
        var rows = createRows("rows", createGroupOptionSelect("child", "parent"));
        var authoredValues = new AuthoredElementValues();
        authoredValues.putLiteral("parent", "group_a");
        authoredValues.putLiteral("other_rows", List.of(rowValue("other-1", "parent", "group_a")));
        authoredValues.putLiteral("rows", List.of(rowValue("row-1", "child", "option_a")));

        var result = derive(createRoot(List.of(otherRows, rows)), authoredValues, new ElementDerivationOptions());

        assertNull(effectiveRow(result.getEffectiveValues(), "rows", 0).getValues().get("child"));
    }

    @Test
    void shouldIgnoreSubmittedValuesOfHiddenReferencedSelects() {
        var parent = createGroupSelect("parent");
        parent.setVisibility(ElementVisibilityFunctions.of(NoCodeStaticValue.of(false)));
        var authoredValues = new AuthoredElementValues();
        authoredValues.putLiteral("parent", "group_a");
        authoredValues.putLiteral("child", "option_a");

        var result = derive(
                createRoot(List.of(parent, createGroupOptionSelect("child", "parent"))),
                authoredValues,
                new ElementDerivationOptions()
        );

        assertNull(result.getEffectiveValues().get("child"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"disabled", "technical"})
    void shouldCheckAgainstTheTrustedValueOfAProtectedReferencedSelectPlacedLater(String protection) {
        var parent = createGroupSelect("parent");
        parent.setLabel("Gruppe");
        parent.setDisabled("disabled".equals(protection));
        parent.setTechnical("technical".equals(protection));
        var authoredValues = new AuthoredElementValues();
        authoredValues.putLiteral("parent", "group_a");
        authoredValues.putLiteral("child", "option_a");
        var trustedValues = new AuthoredElementValues();
        trustedValues.putLiteral("parent", "group_b");

        var result = derive(
                createRoot(List.of(createGroupOptionSelect("child", "parent"), parent)),
                authoredValues,
                new ElementDerivationOptions()
                        .setIgnoreAuthoredValuesOfNonEditableInputs(true)
                        .setTrustedAuthoredValues(trustedValues)
        );

        assertEquals("group_b", result.getEffectiveValues().get("parent"));
        assertEquals(
                "Die ausgewählte Option passt nicht zur Auswahl im Feld „Gruppe“. Wählen Sie eine passende Option aus.",
                result.getElementStates().get("child").getError()
        );
    }

    @Test
    void shouldCheckDependentSelectsAgainstReferencedSelectsPlacedLaterOnceTheyAreDerived() {
        var matchingValues = new AuthoredElementValues();
        matchingValues.putLiteral("child", "option_a");
        matchingValues.putLiteral("parent", "group_a");
        var mismatchingValues = new AuthoredElementValues();
        mismatchingValues.putLiteral("child", "option_a");
        mismatchingValues.putLiteral("parent", "group_b");
        var root = createRoot(List.of(createGroupOptionSelect("child", "parent"), createGroupSelect("parent")));

        var matching = derive(root, matchingValues, new ElementDerivationOptions());
        var mismatching = derive(root, mismatchingValues, new ElementDerivationOptions());
        var mismatchingWithoutErrors = derive(root, mismatchingValues,
                new ElementDerivationOptions().setSkipErrorsForElementIds(List.of("child")));

        assertFalse(matching.hasAnyError());
        assertEquals("option_a", matching.getEffectiveValues().get("child"));
        // Later elements may already have used the value, so it stays and the error blocks the submission.
        assertEquals("option_a", mismatching.getEffectiveValues().get("child"));
        assertEquals(
                "Die ausgewählte Option passt nicht zur Auswahl im übergeordneten Auswahlfeld. Wählen Sie eine passende Option aus.",
                mismatching.getElementStates().get("child").getError()
        );
        assertFalse(mismatchingWithoutErrors.hasAnyError());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldCheckTheTrimmedValueOfADependentSelectAgainstTheGroup(boolean parentPlacedLater) {
        var parent = createGroupSelect("parent");
        parent.setLabel("Gruppe");
        parent.setDisabled(true);
        var child = createGroupOptionSelect("child", "parent");
        var authoredValues = new AuthoredElementValues();
        // Padded, the value is no option of the select. Before validation, it is trimmed to the option of group A.
        authoredValues.putLiteral("child", " option_a ");
        var trustedValues = new AuthoredElementValues();
        trustedValues.putLiteral("parent", "group_b");

        var result = derive(
                createRoot(parentPlacedLater ? List.of(child, parent) : List.of(parent, child)),
                authoredValues,
                new ElementDerivationOptions()
                        .setIgnoreAuthoredValuesOfNonEditableInputs(true)
                        .setTrustedAuthoredValues(trustedValues)
        );

        if (parentPlacedLater) {
            assertEquals("option_a", result.getEffectiveValues().get("child"));
            assertEquals(
                    "Die ausgewählte Option passt nicht zur Auswahl im Feld „Gruppe“. Wählen Sie eine passende Option aus.",
                    result.getElementStates().get("child").getError()
            );
        } else {
            assertNull(result.getEffectiveValues().get("child"));
        }
    }

    @Test
    void shouldOmitHiddenReplicatingRowChildFromEffectiveValues() {
        var visibleChild = new TextInputElement();
        visibleChild.setId("visible_child");

        var hiddenChild = new TextInputElement();
        hiddenChild.setId("hidden_child");
        hiddenChild.setVisibility(new ElementVisibilityFunctions().setNoCode(NoCodeStaticValue.of(false)));

        var rows = new ReplicatingContainerLayoutElement();
        rows.setId("rows");
        rows.setChildren(new LinkedList<>(List.of(visibleChild, hiddenChild)));

        var rowValues = new AuthoredElementValues();
        rowValues.putLiteral("visible_child", "visible value");
        rowValues.putLiteral("hidden_child", "hidden value");

        var authoredValues = new AuthoredElementValues();
        authoredValues.putLiteral("rows", List.of(new ReplicatingContainerLayoutElementValue().setValues(rowValues)));

        var result = derive(
                createRoot(List.of(rows)),
                authoredValues,
                new ElementDerivationOptions()
        );

        var effectiveRows = assertInstanceOf(List.class, result.getEffectiveValues().get("rows"));
        var firstRow = assertInstanceOf(EffectiveReplicatingContainerLayoutElementValue.class, effectiveRows.get(0));

        assertEquals("visible value", firstRow.getValues().get("visible_child"));
        assertFalse(firstRow.getValues().containsKey("hidden_child"));
        assertFalse(result.getElementStates().get("rows").getSubStates().getFirst().getStates().get("hidden_child").getVisible());
    }

    private static DerivedRuntimeElementData derive(
            FormLayoutElement root,
            AuthoredElementValues authoredValues,
            ElementDerivationOptions options
    ) {
        return createService().derive(
                new ElementDerivationRequest(root, authoredValues, options),
                new IdentityDataMap(),
                new ElementDerivationLogger()
        );
    }

    private static DerivedRuntimeElementData derive(
            FormLayoutElement root,
            AuthoredElementValues authoredValues,
            ElementDerivationOptions options,
            ProcessExecutionData processExecutionData,
            InputModeEvaluationContext inputModeContext
    ) {
        return createService().derive(
                new ElementDerivationRequest(root, authoredValues, options, processExecutionData),
                new IdentityDataMap(),
                new ElementDerivationLogger(),
                inputModeContext
        );
    }

    private static NumberInputElement dynamicNumberField() {
        var field = new NumberInputElement();
        field.setId("field");
        field.setInputModePolicy(new InputModePolicy(
                List.of(InputMode.Literal, InputMode.Variable, InputMode.NoCode, InputMode.LowCode),
                InputMode.Literal,
                List.of(InputVariableSource.ProcessData)
        ));
        return field;
    }

    private static TextInputElement dynamicTextField() {
        var field = new TextInputElement();
        field.setId("field");
        field.setInputModePolicy(new InputModePolicy(
                List.of(InputMode.Literal, InputMode.Variable, InputMode.NoCode, InputMode.LowCode),
                InputMode.Literal,
                List.of(InputVariableSource.ProcessData)
        ));
        field.setDynamicTextPolicy(new DynamicTextPolicy());
        return field;
    }

    private static ElementDerivationService createService() {
        return new ElementDerivationService(
                new JavascriptEngineFactoryService(List.of()),
                new NoCodeEvaluationService(List.of(new CommonOperatorsV1(
                        null,
                        null,
                        new BusinessTime(
                                ZoneId.of("Europe/Berlin"),
                                Clock.fixed(Instant.parse("2026-08-07T10:00:00Z"), ZoneOffset.UTC)
                        )
                ))),
                new ElementDataTransformService(),
                new CodeListElementOptionsService(null, null),
                new AuthoredInputValueService(JsonMapperTestUtils.createMapper()),
                new InputVariableResolver()
        );
    }

    private static SelectInputElement createGroupedSelect(
            String id,
            String dependsOnSelectFieldId,
            List<SelectInputElementOption> options
    ) {
        var field = new SelectInputElement();
        field.setId(id);
        field.setOptions(options);
        field.setDependsOnSelectFieldId(dependsOnSelectFieldId);
        return field;
    }

    private static TextInputElement createVisibleIfEquals(String id, NoCodeOperand reference, String value) {
        var field = new TextInputElement();
        field.setId(id);
        field.setVisibility(ElementVisibilityFunctions.of(NoCodeExpression.of(
                "equals",
                reference,
                NoCodeStaticValue.of(value)
        )));
        return field;
    }

    private static FormLayoutElement createRoot(List<BaseFormElement> stepChildren) {
        var step = new GenericStepElement();
        step.setId("step");
        step.setChildren(new LinkedList<>(stepChildren));

        var root = new FormLayoutElement();
        root.setId("root");
        root.setChildren(new LinkedList<BaseStepElement>(List.of(step)));
        return root;
    }

    private static SelectInputElement createGroupSelect(String id) {
        return createGroupedSelect(id, null, List.of(
                SelectInputElementOption.of("group_a", "Gruppe A"),
                SelectInputElementOption.of("group_b", "Gruppe B")
        ));
    }

    private static SelectInputElement createGroupOptionSelect(String id, String dependsOnSelectFieldId) {
        return createGroupedSelect(id, dependsOnSelectFieldId, List.of(
                SelectInputElementOption.of("option_a", "Option A", "group_a"),
                SelectInputElementOption.of("option_b", "Option B", "group_b")
        ));
    }

    private static ReplicatingContainerLayoutElement createRows(String id, BaseFormElement... children) {
        var rows = new ReplicatingContainerLayoutElement();
        rows.setId(id);
        rows.setChildren(new LinkedList<>(List.of(children)));
        return rows;
    }

    private static ReplicatingContainerLayoutElementValue outerRowValue(String id, String rowParent,
                                                                        ReplicatingContainerLayoutElementValue innerRow) {
        var values = new AuthoredElementValues();
        values.putLiteral("row_parent", rowParent);
        values.putLiteral("inner_rows", List.of(innerRow));
        return new ReplicatingContainerLayoutElementValue().setId(id).setValues(values);
    }

    private static EffectiveReplicatingContainerLayoutElementValue effectiveRow(Map<String, Object> values, String containerId, int index) {
        var rows = assertInstanceOf(List.class, values.get(containerId));
        return assertInstanceOf(EffectiveReplicatingContainerLayoutElementValue.class, rows.get(index));
    }

    private static ReplicatingContainerLayoutElementValue rowValue(String id, String... keysAndValues) {
        var values = new AuthoredElementValues();
        for (var index = 0; index < keysAndValues.length; index += 2) {
            values.putLiteral(keysAndValues[index], keysAndValues[index + 1]);
        }
        return new ReplicatingContainerLayoutElementValue().setId(id).setValues(values);
    }
}
