package de.aivot.prosuna.backend.xdf.v2.services;

import de.aivot.prosuna.backend.elements.enums.ValidationFunctionType;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.nocode.models.NoCodeExpression;
import de.aivot.prosuna.backend.nocode.models.NoCodeReference;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import de.aivot.prosuna.backend.plugins.core.v1.operators.text.NoCodeRegexMatchOperator;
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
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class XdfTransformServiceTest {
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
}
