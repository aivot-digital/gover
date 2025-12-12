package de.aivot.GoverBackend.nocode.services;

import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.models.*;
import de.aivot.GoverBackend.nocode.providers.NoCodeOperatorsProvider;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NoCodeEvaluationServiceTest {

    @Test
    void evaluate() {
        var context = new ElementData();

        var testProvider = getNoCodeOperatorSPI();

        var evalService = new NoCodeEvaluationService(List.of(testProvider));

        var res = evalService.evaluate(
                new NoCodeExpression(
                        "de.aivot.gover.test.and",
                        new NoCodeStaticValue(true),
                        new NoCodeStaticValue(true)
                ),
                context
        );

        assertEquals(true, res.getValue());

        context.putInputValue("a", ElementType.Checkbox, true);
        context.putInputValue("b", ElementType.Checkbox, false);
        context.putInputValue("c", ElementType.Checkbox, true);

        var result = evalService.evaluate(
                new NoCodeExpression(
                        "de.aivot.gover.test.and",
                        new NoCodeReference("a"),
                        new NoCodeReference("a")
                ),
                context
        );

        assertEquals(true, result.getValue());

        result = evalService.evaluate(
                new NoCodeExpression(
                        "de.aivot.gover.test.and",
                        new NoCodeReference("a"),
                        new NoCodeReference("b")
                ),
                context
        );

        assertEquals(false, result.getValue());

        result = evalService.evaluate(
                new NoCodeExpression(
                        "de.aivot.gover.test.and",
                        new NoCodeReference("a"),
                        new NoCodeReference("c")
                ),
                context
        );

        assertEquals(true, result.getValue());

        result = evalService.evaluate(
                new NoCodeExpression(
                        "de.aivot.gover.test.and",
                        new NoCodeStaticValue(true),
                        new NoCodeExpression(
                                "de.aivot.gover.test.and",
                                new NoCodeStaticValue(true),
                                new NoCodeExpression(
                                        "de.aivot.gover.test.and",
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
            public NoCodeResult performEvaluation(ElementData data, Object... args) throws NoCodeException {
                return new NoCodeResult(Objects.equals(args[0], args[1]));
            }
        };

        return new NoCodeOperatorsProvider() {
            @Override
            public String getPackageName() {
                return "de.aivot.gover.test";
            }

            @Override
            public String getLabel() {
                return "test";
            }

            @Override
            public String getDescription() {
                return "test";
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