package de.aivot.GoverBackend.nocode.services;

import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.models.*;
import de.aivot.GoverBackend.nocode.providers.NoCodeOperatorsProvider;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static de.aivot.GoverBackend.TestData.runtime;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
