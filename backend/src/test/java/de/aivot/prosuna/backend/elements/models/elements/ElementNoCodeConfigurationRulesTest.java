package de.aivot.prosuna.backend.elements.models.elements;

import de.aivot.prosuna.backend.elements.enums.ValidationFunctionType;
import de.aivot.prosuna.backend.elements.enums.ValueFunctionType;
import de.aivot.prosuna.backend.elements.enums.VisibilityFunctionType;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.nocode.models.NoCodeExpression;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperand;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import de.aivot.prosuna.backend.nocode.services.NoCodeOperatorProviderService;
import de.aivot.prosuna.backend.plugins.core.v1.operators.CommonOperatorsV1;
import de.aivot.prosuna.backend.utils.SpringContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ElementNoCodeConfigurationRulesTest {
    private ApplicationContext previousContext;
    private StaticApplicationContext context;

    @BeforeEach
    void registerOperators() {
        // Expressions look up their operators to check the number of operands.
        previousContext = (ApplicationContext) ReflectionTestUtils.getField(SpringContext.class, "context");
        context = new StaticApplicationContext();
        context.getBeanFactory().registerSingleton(
                "noCodeOperatorProviderService",
                new NoCodeOperatorProviderService(List.of(new CommonOperatorsV1(null, null, null)))
        );
        new SpringContext().setApplicationContext(context);
    }

    @AfterEach
    void restoreContext() {
        new SpringContext().setApplicationContext(previousContext);
        context.close();
    }

    @Test
    void shouldAcceptCompleteNoCodeExpressions() {
        assertEquals(Map.of(), field(complete(), complete(), List.of(complete())).validateConfiguration());
    }

    @Test
    void shouldReportMissingNoCodeExpressions() {
        assertEquals(Map.of(
                "visibility", List.of("Legen Sie einen No-Code-Ausdruck für die Sichtbarkeit fest."),
                "value", List.of("Legen Sie einen No-Code-Ausdruck für den dynamischen Wert fest."),
                "validation", List.of("Legen Sie einen No-Code-Ausdruck für die Validierung fest.")
        ), field(null, null, List.of()).validateConfiguration());

        var missingRule = field(complete(), complete(), Arrays.asList(complete(), null));
        assertEquals(Map.of("validation", List.of("Legen Sie einen No-Code-Ausdruck für die Validierung fest.")),
                missingRule.validateConfiguration());
    }

    @Test
    void shouldReportIncompleteNoCodeExpressions() {
        assertEquals(Map.of(
                "visibility", List.of("Vervollständigen Sie den No-Code-Ausdruck für die Sichtbarkeit."),
                "value", List.of("Vervollständigen Sie den No-Code-Ausdruck für den dynamischen Wert."),
                "validation", List.of("Vervollständigen Sie den No-Code-Ausdruck für die Validierung.")
        ), field(incomplete(), incomplete(), List.of(complete(), incomplete())).validateConfiguration());
    }

    @Test
    void shouldIgnoreNoCodeExpressionsOfOtherFunctionTypes() {
        var element = field(null, null, List.of());
        element.getVisibility().setType(VisibilityFunctionType.Javascript);
        element.getValue().setType(ValueFunctionType.Javascript);
        element.getValidation().setType(ValidationFunctionType.Javascript);

        assertEquals(Map.of(), element.validateConfiguration());
    }

    private static NoCodeOperand complete() {
        return NoCodeExpression.of("equals", NoCodeStaticValue.of("a"), NoCodeStaticValue.of("a"));
    }

    // The second operand of "equals" is missing.
    private static NoCodeOperand incomplete() {
        return NoCodeExpression.of("equals", NoCodeStaticValue.of("a"));
    }

    private static TextInputElement field(NoCodeOperand visibility, NoCodeOperand value, List<NoCodeOperand> validations) {
        var element = new TextInputElement();
        element.setLabel("Feld");
        element.setVisibility(new ElementVisibilityFunctions().setType(VisibilityFunctionType.NoCode).setNoCode(visibility));
        element.setValue(new ElementValueFunctions().setType(ValueFunctionType.NoCode).setNoCode(value));
        element.setValidation(new ElementValidationFunctions()
                .setType(ValidationFunctionType.NoCode)
                .setNoCodeList(validations.stream()
                        .map(noCode -> new ValidationNoCodeWrapper().setNoCode(noCode).setMessage("Ungültig"))
                        .toList()));
        return element;
    }
}
