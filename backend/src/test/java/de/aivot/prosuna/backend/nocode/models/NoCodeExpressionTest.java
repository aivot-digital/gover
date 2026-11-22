package de.aivot.prosuna.backend.nocode.models;

import de.aivot.prosuna.backend.nocode.services.NoCodeOperatorProviderService;
import de.aivot.prosuna.backend.plugins.core.v1.operators.CommonOperatorsV1;
import de.aivot.prosuna.backend.utils.SpringContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class NoCodeExpressionTest {
    private ApplicationContext previousContext;
    private StaticApplicationContext context;

    @BeforeEach
    void registerOperators() {
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
    void shouldExplainAnEmptyParameterAsAMissingExpression() {
        var expression = NoCodeExpression.of("equals", NoCodeStaticValue.of("text"), null);

        var error = expression.validate();

        assertFalse(error.isValid());
        assertEquals("Alle Parameter des Ausdrucks müssen konfiguriert sein.", error.error());
        assertEquals("Legen Sie einen Ausdruck fest.", error.subErrors().get(1).error());
        assertNull(error.subErrors().get(1).operand());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "missing")
    void shouldExplainAnUnavailableFunctionWithoutChangingValidationPrecedence(String identifier) {
        var expression = new NoCodeExpression().setOperatorIdentifier(identifier);

        var error = expression.validate();

        assertFalse(error.isValid());
        assertEquals("Die ausgewählte Funktion ist nicht verfügbar.", error.error());
    }
}
