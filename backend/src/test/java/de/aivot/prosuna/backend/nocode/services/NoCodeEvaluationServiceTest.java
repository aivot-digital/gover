package de.aivot.prosuna.backend.nocode.services;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.*;
import de.aivot.prosuna.backend.nocode.providers.NoCodeOperatorsProvider;
import de.aivot.prosuna.backend.plugins.core.v1.operators.bool.NoCodeAndOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.bool.NoCodeOrOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.common.NoCodeIfOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.common.NoCodeValueOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.math.NoCodeDivideOperator;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Objects;

import static de.aivot.prosuna.backend.TestData.runtime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NoCodeEvaluationServiceTest {

    @Test
    void evaluate() {
        var context = new DerivedRuntimeElementData();

        var testProvider = getNoCodeOperatorSPI();

        var evalService = new NoCodeEvaluationService(List.of(testProvider));

        var res = evalService.evaluate(
                new NoCodeExpression(
                        "and",
                        new NoCodeStaticValue(true),
                        new NoCodeStaticValue(true)
                ),
                context
        );

        assertEquals(true, res.getValue());

        context = runtime(
                "a", true,
                "b", false,
                "c", true
        );

        var result = evalService.evaluate(
                new NoCodeExpression(
                        "and",
                        new NoCodeReference("a"),
                        new NoCodeReference("a")
                ),
                context
        );

        assertEquals(true, result.getValue());

        result = evalService.evaluate(
                new NoCodeExpression(
                        "and",
                        new NoCodeReference("a"),
                        new NoCodeReference("b")
                ),
                context
        );

        assertEquals(false, result.getValue());

        result = evalService.evaluate(
                new NoCodeExpression(
                        "and",
                        new NoCodeReference("a"),
                        new NoCodeReference("c")
                ),
                context
        );

        assertEquals(true, result.getValue());

        result = evalService.evaluate(
                new NoCodeExpression(
                        "and",
                        new NoCodeStaticValue(true),
                        new NoCodeExpression(
                                "and",
                                new NoCodeStaticValue(true),
                                new NoCodeExpression(
                                        "and",
                                        new NoCodeStaticValue(true),
                                        new NoCodeReference("a")
                                )
                        )
                ),
                context
        );

        assertEquals(true, result.getValue());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void ifEvaluatesItsConditionOnceAndOnlyCallsTheSelectedBranch(boolean condition) throws NoCodeException {
        var valueOperator = spy(new NoCodeValueOperator());
        var service = serviceWithConditionalOperators(valueOperator);
        var data = new DerivedRuntimeElementData();
        var conditionText = condition ? "wahr" : "falsch";
        var expression = new NoCodeExpression("if", value(conditionText), value("then"), value("else"));

        assertEquals(condition ? "then" : "else", service.evaluate(expression, data).getValue());
        var calls = inOrder(valueOperator);
        calls.verify(valueOperator).evaluate(data, conditionText);
        calls.verify(valueOperator).evaluate(data, condition ? "then" : "else");
        verify(valueOperator, never()).evaluate(data, condition ? "else" : "then");
    }

    @Test
    void ifPropagatesAConditionFailureWithoutCallingEitherBranch() throws NoCodeException {
        var valueOperator = spy(new NoCodeValueOperator());
        var expression = new NoCodeExpression("if", divisionByZero(), value("then"), value("else"));
        var service = serviceWithConditionalOperators(valueOperator);

        var error = assertThrows(IllegalStateException.class, () -> service.evaluate(expression, new DerivedRuntimeElementData()));
        assertInstanceOf(NoCodeException.class, error.getCause());
        verify(valueOperator, never()).evaluate(any(), any(Object[].class));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void ifPropagatesAFailureInTheSelectedBranchWithoutCallingTheOther(boolean condition) throws NoCodeException {
        var valueOperator = spy(new NoCodeValueOperator());
        var unused = value("unused");
        var expression = new NoCodeExpression("if", new NoCodeStaticValue(condition),
                condition ? divisionByZero() : unused, condition ? unused : divisionByZero());
        var service = serviceWithConditionalOperators(valueOperator);

        var error = assertThrows(IllegalStateException.class, () -> service.evaluate(expression, new DerivedRuntimeElementData()));
        assertInstanceOf(NoCodeException.class, error.getCause());
        verify(valueOperator, never()).evaluate(any(), any(Object[].class));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void andOnlyCallsTheSecondConditionAfterATruthyFirstCondition(boolean first) throws NoCodeException {
        var valueOperator = spy(new NoCodeValueOperator());
        var service = serviceWithConditionalOperators(valueOperator);
        var data = new DerivedRuntimeElementData();
        var conditionText = first ? "wahr" : "falsch";
        var expression = new NoCodeExpression("and", value(conditionText), value("true"));

        assertEquals(first, service.evaluate(expression, data).getValue());
        var calls = inOrder(valueOperator);
        calls.verify(valueOperator).evaluate(data, conditionText);
        if (first) {
            calls.verify(valueOperator).evaluate(data, "true");
        } else {
            verify(valueOperator, never()).evaluate(data, "true");
        }
    }

    @Test
    void andPropagatesTheErrorFromEitherReachedCondition() throws NoCodeException {
        var valueOperator = spy(new NoCodeValueOperator());
        var service = serviceWithConditionalOperators(valueOperator);
        var data = new DerivedRuntimeElementData();
        var firstFails = new NoCodeExpression("and", divisionByZero(), value("unreachable"));
        var secondFails = new NoCodeExpression("and", new NoCodeStaticValue(true), divisionByZero());

        for (var expression : List.of(firstFails, secondFails)) {
            var error = assertThrows(IllegalStateException.class, () -> service.evaluate(expression, data));
            var cause = assertInstanceOf(NoCodeException.class, error.getCause());
            assertEquals("„Teiler“ darf nicht 0 sein. Geben Sie eine andere Zahl ein.", cause.getMessage());
        }
        verify(valueOperator, never()).evaluate(data, "unreachable");
    }

    @Test
    void orEvaluatesEachConditionOnceInOrderUntilTheFirstTruthyValue() throws NoCodeException {
        var valueOperator = spy(new NoCodeValueOperator());
        var service = serviceWithConditionalOperators(valueOperator);
        var data = new DerivedRuntimeElementData();
        var expression = new NoCodeExpression("or", value("false"), value("falsch"), value("wahr"), value("unused"));

        assertEquals(true, service.evaluate(expression, data).getValue());
        var calls = inOrder(valueOperator);
        calls.verify(valueOperator).evaluate(data, "false");
        calls.verify(valueOperator).evaluate(data, "falsch");
        calls.verify(valueOperator).evaluate(data, "wahr");
        verify(valueOperator, times(3)).evaluate(eq(data), any(Object[].class));
    }

    @Test
    void orPropagatesAnErrorBeforeTheNextConditionCanBeCalled() throws NoCodeException {
        var valueOperator = spy(new NoCodeValueOperator());
        var service = serviceWithConditionalOperators(valueOperator);
        var data = new DerivedRuntimeElementData();
        var expression = new NoCodeExpression("or", new NoCodeStaticValue(false), divisionByZero(), value("unreachable"));

        var error = assertThrows(IllegalStateException.class, () -> service.evaluate(expression, data));
        var cause = assertInstanceOf(NoCodeException.class, error.getCause());
        assertEquals("„Teiler“ darf nicht 0 sein. Geben Sie eine andere Zahl ein.", cause.getMessage());
        verify(valueOperator, never()).evaluate(data, "unreachable");
    }

    private static NoCodeEvaluationService serviceWithConditionalOperators(NoCodeValueOperator valueOperator) {
        var provider = mock(NoCodeOperatorsProvider.class);
        when(provider.getOperators()).thenReturn(new NoCodeOperator[]{
                new NoCodeIfOperator(), new NoCodeAndOperator(), new NoCodeOrOperator(), valueOperator, new NoCodeDivideOperator()
        });
        return new NoCodeEvaluationService(List.of(provider));
    }

    private static NoCodeExpression value(String value) {
        return new NoCodeExpression("value", new NoCodeStaticValue(value));
    }

    private static NoCodeExpression divisionByZero() {
        return new NoCodeExpression("divide", new NoCodeStaticValue(1), new NoCodeStaticValue(0));
    }

    private static NoCodeOperatorsProvider getNoCodeOperatorSPI() {
        var testOperator = new NoCodeOperator() {
            @Override
            public String getIdentifier() {
                return "and";
            }

            @Override
            public String getLabel() {
                return "and";
            }

            @Override
            public String getAbstract() {
                return "add";
            }

            @Override
            public String getDescription() {
                return "and";
            }

            @Override
            public NoCodeSignatur[] getSignatures() {
                return NoCodeSignatur.of(
                        NoCodeSignatur.of(
                                NoCodeDataType.Boolean,
                                new NoCodeParameter(NoCodeDataType.Boolean, "a", ""),
                                new NoCodeParameter(NoCodeDataType.Boolean, "b", "")
                        )
                );
            }

            @Override
            public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
                return new NoCodeResult(Objects.equals(args[0], args[1]));
            }
        };

        return new NoCodeOperatorsProvider() {
            @Nonnull
            @Override
            public String getParentPluginKey() {
                return "de.aivot";
            }

            @Nonnull
            @Override
            public String getComponentKey() {
                return "test";
            }

            @Nonnull
            @Override
            public String getComponentVersion() {
                return "1.0.0";
            }

            @Nonnull
            @Override
            public String getName() {
                return "";
            }

            @Nonnull
            @Override
            public String getAbstract() {
                return "";
            }

            @Nonnull
            @Override
            public String getDescription() {
                return "";
            }

            @Override
            public NoCodeOperator[] getOperators() {
                return new NoCodeOperator[]{
                        testOperator,
                };
            }
        };
    }
}
