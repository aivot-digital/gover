package de.aivot.prosuna.backend.elements.services;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.elements.enums.EffectiveValueSource;
import de.aivot.prosuna.backend.elements.enums.InputMode;
import de.aivot.prosuna.backend.elements.enums.InputModeEvaluationContext;
import de.aivot.prosuna.backend.elements.enums.InputVariableSource;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.ElementDerivationOptions;
import de.aivot.prosuna.backend.elements.models.ElementDerivationRequest;
import de.aivot.prosuna.backend.elements.models.elements.BaseFormElement;
import de.aivot.prosuna.backend.elements.models.elements.ElementOverrideFunctions;
import de.aivot.prosuna.backend.elements.models.elements.ElementValidationFunctions;
import de.aivot.prosuna.backend.elements.models.elements.ElementValueFunctions;
import de.aivot.prosuna.backend.elements.models.elements.ElementVisibilityFunctions;
import de.aivot.prosuna.backend.elements.models.elements.ValidationNoCodeWrapper;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
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
import de.aivot.prosuna.backend.nocode.models.NoCodeReference;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import de.aivot.prosuna.backend.nocode.services.NoCodeEvaluationService;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.plugins.core.v1.operators.CommonOperatorsV1;
import de.aivot.prosuna.backend.submission.services.ElementDataTransformService;
import org.junit.jupiter.api.Test;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementDerivationServiceTest {
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

    private static FormLayoutElement createRoot(List<BaseFormElement> stepChildren) {
        var step = new GenericStepElement();
        step.setId("step");
        step.setChildren(new LinkedList<>(stepChildren));

        var root = new FormLayoutElement();
        root.setId("root");
        root.setChildren(new LinkedList<BaseStepElement>(List.of(step)));
        return root;
    }
}
