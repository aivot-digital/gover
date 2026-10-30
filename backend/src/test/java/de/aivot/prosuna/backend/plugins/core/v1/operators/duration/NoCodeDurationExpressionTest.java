package de.aivot.prosuna.backend.plugins.core.v1.operators.duration;

import de.aivot.prosuna.backend.nocode.models.NoCodeExpression;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import de.aivot.prosuna.backend.nocode.services.NoCodeEvaluationService;
import de.aivot.prosuna.backend.nocode.services.NoCodeOperatorProviderService;
import de.aivot.prosuna.backend.plugins.core.v1.operators.CommonOperatorsV1;
import de.aivot.prosuna.backend.utils.SpringContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static de.aivot.prosuna.backend.TestData.runtime;
import static org.junit.jupiter.api.Assertions.*;

class NoCodeDurationExpressionTest {
    private ApplicationContext previousContext;
    private StaticApplicationContext context;
    private NoCodeEvaluationService evaluationService;

    @BeforeEach
    void registerOperators() {
        previousContext = (ApplicationContext) ReflectionTestUtils.getField(SpringContext.class, "context");
        var providers = List.of(new CommonOperatorsV1(null, null, null));
        context = new StaticApplicationContext();
        context.getBeanFactory().registerSingleton("noCodeOperatorProviderService",
                new NoCodeOperatorProviderService(List.copyOf(providers)));
        new SpringContext().setApplicationContext(context);
        evaluationService = new NoCodeEvaluationService(List.copyOf(providers));
    }

    @AfterEach
    void restoreContext() {
        new SpringContext().setApplicationContext(previousContext);
        context.close();
    }

    @Test
    void rejectsAnOmittedUnit() {
        var expression = NoCodeExpression.of("format-duration", NoCodeStaticValue.of(99000));
        assertFalse(expression.validate().isValid());
    }

    @Test
    void validatesAndEvaluatesAnExplicitUnit() {
        var expression = NoCodeExpression.of("format-duration", NoCodeStaticValue.of(99000), NoCodeStaticValue.of("days"));
        assertTrue(expression.validate().isValid());
        assertEquals("1 Tag 3 Stunden 30 Minuten", evaluationService.evaluate(expression, runtime()).getValue());
    }

    @Test
    void preservesExplicitEmptyLiteralConversion() {
        for (Object value : new Object[]{null, ""}) {
            var expression = NoCodeExpression.of("format-duration", NoCodeStaticValue.of(99000), NoCodeStaticValue.of(value));
            assertTrue(expression.validate().isValid());
            assertEquals("27 Stunden 30 Minuten", evaluationService.evaluate(expression, runtime()).getValue());
        }
    }

    @Test
    void rejectsClearedSlots() {
        var expression = NoCodeExpression.of("format-duration", NoCodeStaticValue.of(99000), null);
        var error = expression.validate();
        assertFalse(error.isValid());
        assertFalse(error.subErrors().get(1).isValid());
        assertFalse(NoCodeExpression.of("format-duration", null, NoCodeStaticValue.of("hours")).validate().isValid());
    }

    @Test
    void rejectsMissingRequiredAndExcessOperands() {
        assertFalse(NoCodeExpression.of("format-duration").validate().isValid());
        assertFalse(NoCodeExpression.of("create-duration", NoCodeStaticValue.of(1)).validate().isValid());
        assertFalse(NoCodeExpression.of("format-duration", NoCodeStaticValue.of(60),
                NoCodeStaticValue.of("hours"), NoCodeStaticValue.of("extra")).validate().isValid());
    }

    @Test
    void validatesNestedDurationExpressionsAndRejectsInvalidChildren() {
        var valid = NoCodeExpression.of("format-duration", NoCodeStaticValue.of(60), NoCodeStaticValue.of("hours"));
        var expression = NoCodeExpression.of("equals", valid, NoCodeStaticValue.of("1 Minute"));
        assertTrue(expression.validate().isValid());
        assertEquals(true, evaluationService.evaluate(expression, runtime()).getValue());
        assertFalse(NoCodeExpression.of("equals", NoCodeExpression.of("format-duration"),
                NoCodeStaticValue.of("1 Minute")).validate().isValid());
    }
}
