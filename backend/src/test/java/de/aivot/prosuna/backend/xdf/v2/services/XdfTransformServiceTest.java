package de.aivot.prosuna.backend.xdf.v2.services;

import de.aivot.prosuna.backend.asset.repositories.AssetRepository;
import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.elements.enums.ValidationFunctionType;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.ElementDerivationRequest;
import de.aivot.prosuna.backend.elements.models.elements.form.input.NumberInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.prosuna.backend.elements.services.AssetSelectionValidationService;
import de.aivot.prosuna.backend.elements.services.AuthoredInputValueService;
import de.aivot.prosuna.backend.elements.services.CodeListElementOptionsService;
import de.aivot.prosuna.backend.elements.services.ElementDerivationService;
import de.aivot.prosuna.backend.elements.services.InputVariableResolver;
import de.aivot.prosuna.backend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.prosuna.backend.nocode.models.NoCodeExpression;
import de.aivot.prosuna.backend.nocode.models.NoCodeReference;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import de.aivot.prosuna.backend.nocode.services.NoCodeEvaluationService;
import de.aivot.prosuna.backend.plugins.core.v1.operators.CommonOperatorsV1;
import de.aivot.prosuna.backend.plugins.core.v1.operators.text.NoCodeRegexMatchOperator;
import de.aivot.prosuna.backend.submission.services.ElementDataTransformService;
import de.aivot.prosuna.backend.xdf.v2.data.XdfFieldType;
import de.aivot.prosuna.backend.xdf.v2.models.XdfCodeListeWrapper;
import de.aivot.prosuna.backend.xdf.v2.models.XdfDatenfeld;
import de.aivot.prosuna.backend.xdf.v2.models.XdfEnthaelt;
import de.aivot.prosuna.backend.xdf.v2.models.XdfIdentifikation;
import de.aivot.prosuna.backend.xdf.v2.models.XdfStammdatenschema;
import de.aivot.prosuna.backend.xdf.v2.models.XdfStammdatenschema0102;
import de.aivot.prosuna.backend.xdf.v2.models.XdfStruktur;
import de.aivot.prosuna.backend.xrepository.services.XRepositoryCodeListService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class XdfTransformServiceTest {
    @ParameterizedTest
    @ValueSource(strings = {XdfFieldType.NUMBER, XdfFieldType.INTEGER, XdfFieldType.CURRENCY})
    void importsInclusiveNumericBoundsAsPersistentNoCodeRules(String dataType) throws Exception {
        var root = numericForm(dataType, "{\"minValue\":-2,\"maxValue\":3}", false);
        var mapper = JsonMapperTestUtils.createMapper();
        root = mapper.readValue(mapper.writeValueAsString(root), FormLayoutElement.class);
        var validation = root.findChild("number_field", NumberInputElement.class).orElseThrow().getValidation();

        assertNotNull(validation);
        assertEquals(ValidationFunctionType.NoCode, validation.getType());
        assertNull(validation.getConditionSet());
        assertEquals(2, validation.getNoCodeList().size());
        assertEquals(List.of("number_field"), List.copyOf(validation.getReferencedIds()));

        assertValidationError(root, -2, null);
        assertValidationError(root, 0, null);
        assertValidationError(root, 3, null);
        var step = XdfFieldType.INTEGER.equals(dataType) ? BigDecimal.ONE : new BigDecimal("0.01");
        assertValidationError(root, new BigDecimal("-2").subtract(step),
                "Bitte geben Sie einen Wert größer oder gleich -2 ein.");
        assertValidationError(root, new BigDecimal("3").add(step),
                "Bitte geben Sie einen Wert kleiner oder gleich 3 ein.");
        if (!XdfFieldType.INTEGER.equals(dataType)) {
            assertValidationError(root, new BigDecimal("1.25"), null);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {XdfFieldType.NUMBER, XdfFieldType.INTEGER, XdfFieldType.CURRENCY})
    void importsSingleBoundsIncludingZero(String dataType) {
        var minimumOnly = numericForm(dataType, "{\"minValue\":0}", false);
        var maximumOnly = numericForm(dataType, "{\"maxValue\":0}", false);

        assertEquals(1, minimumOnly.findChild("number_field", NumberInputElement.class)
                .orElseThrow().getValidation().getNoCodeList().size());
        assertEquals(1, maximumOnly.findChild("number_field", NumberInputElement.class)
                .orElseThrow().getValidation().getNoCodeList().size());
        assertValidationError(minimumOnly, 0, null);
        assertValidationError(minimumOnly, 100, null);
        assertValidationError(minimumOnly, -1, "Bitte geben Sie einen Wert größer oder gleich 0 ein.");
        assertValidationError(maximumOnly, 0, null);
        assertValidationError(maximumOnly, -100, null);
        assertValidationError(maximumOnly, 1, "Bitte geben Sie einen Wert kleiner oder gleich 0 ein.");
    }

    @ParameterizedTest
    @ValueSource(strings = {XdfFieldType.NUMBER, XdfFieldType.INTEGER, XdfFieldType.CURRENCY})
    void preservesOptionalAndRequiredEmptyFieldBehavior(String dataType) {
        var optional = numericForm(dataType, "{\"minValue\":1,\"maxValue\":5}", false);
        assertValidationError(optional, null, null);

        var required = numericForm(dataType, "{\"minValue\":1,\"maxValue\":5}", true);
        var result = derivationService().derive(new ElementDerivationRequest(required, new AuthoredElementValues()));
        assertTrue(result.hasAnyError());
        assertNotNull(result.getElementStates().get("number_field").getError());
    }

    @ParameterizedTest
    @ValueSource(strings = {XdfFieldType.NUMBER, XdfFieldType.INTEGER, XdfFieldType.CURRENCY})
    void doesNotInventBoundsWhenPrecisionHasNone(String dataType) {
        for (var precision : new String[]{null, "{}"}) {
            var root = numericForm(dataType, precision, false);
            assertNull(root.findChild("number_field", NumberInputElement.class).orElseThrow().getValidation());
            assertValidationError(root, -100, null);
            assertValidationError(root, 100, null);
        }
    }

    @Test
    void transformsTextPatternIntoNoCodeRegexValidation() throws Exception {
        var regex = "^[A-Z]{2}\\d{3}$";
        var field = new XdfDatenfeld()
                .setIdentifikation(new XdfIdentifikation().setId("text_field"))
                .setName("Textfeld")
                .setBezeichnungEingabe("Kennung")
                .setFeldart(new XdfCodeListeWrapper().setCode(XdfFieldType.INPUT))
                .setDatentyp(new XdfCodeListeWrapper().setCode(XdfFieldType.TEXT))
                .setPraezisierung("{\"pattern\":\"^[A-Z]{2}\\\\d{3}$\"}");
        var schema = new XdfStammdatenschema()
                .setIdentifikation(new XdfIdentifikation().setId("schema"))
                .setName("Testformular")
                .setStruktur(List.of(
                        new XdfStruktur()
                                .setAnzahl("0:1")
                                .setEnthaelt(new XdfEnthaelt().setDatenfeld(field))
                ));
        var source = new XdfStammdatenschema0102().setStammdatenschema(schema);
        var service = new XdfTransformService(
                mock(XRepositoryCodeListService.class),
                JsonMapper.builder().build()
        );

        var result = service.transformToProsuna(source);

        assertNotNull(result);
        var textField = result.findChild("text_field", TextInputElement.class).orElseThrow();
        var validation = textField.getValidation();
        assertNotNull(validation);
        assertEquals(ValidationFunctionType.NoCode, validation.getType());
        assertEquals(List.of("text_field"), List.copyOf(validation.getReferencedIds()));

        var wrapper = validation.getNoCodeList().getFirst();
        assertEquals("Bitte geben Sie einen Wert an, der dem Muster \"%s\" entspricht.".formatted(regex), wrapper.getMessage());
        var expression = assertInstanceOf(NoCodeExpression.class, wrapper.getNoCode());
        assertEquals(NoCodeRegexMatchOperator.OPERATOR_ID, expression.getOperatorIdentifier());
        var operands = List.copyOf(expression.getOperands());
        assertEquals(NoCodeReference.of("text_field"), operands.getFirst());
        assertEquals(NoCodeStaticValue.of(regex), operands.get(1));

        var operator = new NoCodeRegexMatchOperator();
        assertTrue(operator.performEvaluation(DerivedRuntimeElementData.empty(), "AB123", regex).getValueAsBoolean());
        assertFalse(operator.performEvaluation(DerivedRuntimeElementData.empty(), "invalid", regex).getValueAsBoolean());
    }

    private static FormLayoutElement numericForm(String dataType, String precision, boolean required) {
        var field = new XdfDatenfeld()
                .setIdentifikation(new XdfIdentifikation().setId("number_field"))
                .setName("Zahlenfeld")
                .setBezeichnungEingabe("Wert")
                .setFeldart(new XdfCodeListeWrapper().setCode(XdfFieldType.INPUT))
                .setDatentyp(new XdfCodeListeWrapper().setCode(dataType))
                .setPraezisierung(precision);
        var schema = new XdfStammdatenschema()
                .setIdentifikation(new XdfIdentifikation().setId("schema"))
                .setName("Testformular")
                .setStruktur(List.of(new XdfStruktur()
                        .setAnzahl(required ? "1:1" : "0:1")
                        .setEnthaelt(new XdfEnthaelt().setDatenfeld(field))));
        return new XdfTransformService(mock(XRepositoryCodeListService.class), JsonMapperTestUtils.createMapper())
                .transformToProsuna(new XdfStammdatenschema0102().setStammdatenschema(schema));
    }

    private static void assertValidationError(FormLayoutElement root, Object value, String expectedError) {
        var values = new AuthoredElementValues();
        if (value != null) {
            values.putLiteral("number_field", value);
        }
        var result = derivationService().derive(new ElementDerivationRequest(root, values));
        assertEquals(expectedError, result.getElementStates().get("number_field").getError());
        assertEquals(expectedError != null, result.hasAnyError());
    }

    private static ElementDerivationService derivationService() {
        return new ElementDerivationService(
                new JavascriptEngineFactoryService(List.of()),
                new NoCodeEvaluationService(List.of(new CommonOperatorsV1(null, null, null))),
                new ElementDataTransformService(),
                new CodeListElementOptionsService(null, null),
                new AuthoredInputValueService(JsonMapperTestUtils.createMapper()),
                new InputVariableResolver(),
                new AssetSelectionValidationService(mock(AssetRepository.class))
        );
    }
}
