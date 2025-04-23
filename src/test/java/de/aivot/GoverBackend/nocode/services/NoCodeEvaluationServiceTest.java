package de.aivot.GoverBackend.nocode.services;

import de.aivot.GoverBackend.elements.models.ElementDerivationData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.models.*;
import de.aivot.GoverBackend.nocode.providers.NoCodeOperatorServiceProvider;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NoCodeEvaluationServiceTest {

    @Test
    void evaluate() {
        var context = new ElementDerivationData(Map.of());

        var testProvider = getNoCodeOperatorSPI();

        var evalService = new NoCodeEvaluationService(List.of(testProvider));

        var res = evalService.evaluate(
                new NoCodeExpression(
                        "de.aivot.gover.test.and",
                        new NoCodeStaticValue(true),
                        new NoCodeStaticValue(true)
                ),
                context,
                null
        );

        assertEquals(NoCodeDataType.Boolean, res.getDataType());
        assertEquals(true, res.getValue());

        context.setValue("a", true);
        context.setValue("b", false);
        context.setValue("c", true);

        var result = evalService.evaluate(
                new NoCodeExpression(
                        "de.aivot.gover.test.and",
                        new NoCodeReference("a"),
                        new NoCodeReference("a")
                ),
                context,
                null
        );

        assertEquals(NoCodeDataType.Boolean, result.getDataType());
        assertEquals(true, result.getValue());

        result = evalService.evaluate(
                new NoCodeExpression(
                        "de.aivot.gover.test.and",
                        new NoCodeReference("a"),
                        new NoCodeReference("b")
                ),
                context,
                null
        );

        assertEquals(NoCodeDataType.Boolean, result.getDataType());
        assertEquals(false, result.getValue());

        result = evalService.evaluate(
                new NoCodeExpression(
                        "de.aivot.gover.test.and",
                        new NoCodeReference("a"),
                        new NoCodeReference("c")
                ),
                context,
                null
        );

        assertEquals(NoCodeDataType.Boolean, result.getDataType());
        assertEquals(false, result.getValue());

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
                context,
                null
        );

        assertEquals(NoCodeDataType.Boolean, result.getDataType());
        assertEquals(true, result.getValue());
    }

    private static NoCodeOperatorServiceProvider getNoCodeOperatorSPI() {
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
            public NoCodeParameter[] getParameters() {
                return new NoCodeParameter[]{
                        new NoCodeParameter(NoCodeDataType.Boolean, "a"),
                        new NoCodeParameter(NoCodeDataType.Boolean, "b"),
                };
            }

            @Override
            public NoCodeDataType getReturnType() {
                return null;
            }


            @Override
            public NoCodeResult performEvaluation(ElementDerivationData data, Object... args) throws NoCodeException {
                return new NoCodeResult(NoCodeDataType.Boolean, Objects.equals(args[0], args[1]));
            }
        };

        return new NoCodeOperatorServiceProvider() {
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