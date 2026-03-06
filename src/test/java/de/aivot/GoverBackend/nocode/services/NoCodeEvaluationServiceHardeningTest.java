package de.aivot.GoverBackend.nocode.services;

import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.models.NoCodeExpression;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeReference;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;
import de.aivot.GoverBackend.nocode.models.NoCodeSignatur;
import de.aivot.GoverBackend.nocode.models.NoCodeStaticValue;
import de.aivot.GoverBackend.nocode.providers.NoCodeOperatorsProvider;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoCodeEvaluationServiceHardeningTest {
    @Test
    void shouldReturnNullForMissingReference() {
        var service = new NoCodeEvaluationService(List.of());
        var result = service.evaluate(new NoCodeReference("missing"), new ElementData());

        assertNull(result.getValue());
    }

    @Test
    void shouldAllowVariableArgumentCountWhenOperatorSupportsIt() {
        var service = new NoCodeEvaluationService(List.of(new NoCodeOperatorsProvider() {
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
                return "test";
            }

            @Nonnull
            @Override
            public String getDescription() {
                return "test";
            }

            @Override
            public NoCodeOperator[] getOperators() {
                return new NoCodeOperator[]{
                        new NoCodeOperator() {
                            @Override
                            public String getIdentifier() {
                                return "count-args";
                            }

                            @Override
                            public String getLabel() {
                                return "count";
                            }

                            @Override
                            public String getAbstract() {
                                return "count";
                            }

                            @Override
                            public String getDescription() {
                                return "count";
                            }

                            @Override
                            public NoCodeSignatur[] getSignatures() {
                                return NoCodeSignatur.of(
                                        NoCodeSignatur.of(
                                                NoCodeDataType.Number,
                                                new NoCodeParameter(NoCodeDataType.Runtime, "a", ""),
                                                new NoCodeParameter(NoCodeDataType.Runtime, "b", "")
                                        )
                                );
                            }

                            @Override
                            protected boolean supportsVariableArgumentCount() {
                                return true;
                            }

                            @Override
                            protected NoCodeResult performEvaluation(ElementData data, Object... args) {
                                return new NoCodeResult(args.length);
                            }
                        }
                };
            }
        }));

        var result = service.evaluate(
                new NoCodeExpression(
                        "count-args",
                        new NoCodeStaticValue(1),
                        new NoCodeStaticValue(2),
                        new NoCodeStaticValue(3)
                ),
                new ElementData()
        );

        assertEquals(3, result.getValue());
    }

    @Test
    void shouldWrapOperatorEvaluationWithContext() {
        var service = new NoCodeEvaluationService(List.of(new NoCodeOperatorsProvider() {
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
                return "test";
            }

            @Nonnull
            @Override
            public String getDescription() {
                return "test";
            }

            @Override
            public NoCodeOperator[] getOperators() {
                return new NoCodeOperator[]{
                        new NoCodeOperator() {
                            @Override
                            public String getIdentifier() {
                                return "explode";
                            }

                            @Override
                            public String getLabel() {
                                return "explode";
                            }

                            @Override
                            public String getAbstract() {
                                return "explode";
                            }

                            @Override
                            public String getDescription() {
                                return "explode";
                            }

                            @Override
                            public NoCodeSignatur[] getSignatures() {
                                return NoCodeSignatur.of(
                                        NoCodeSignatur.of(
                                                NoCodeDataType.Boolean,
                                                new NoCodeParameter(NoCodeDataType.Runtime, "a", "")
                                        )
                                );
                            }

                            @Override
                            protected NoCodeResult performEvaluation(ElementData data, Object... args) throws NoCodeException {
                                throw new NoCodeException("boom");
                            }
                        }
                };
            }
        }));

        var ex = assertThrows(
                IllegalStateException.class,
                () -> service.evaluate(
                        new NoCodeExpression(
                                "explode",
                                new NoCodeStaticValue(true)
                        ),
                        new ElementData()
                )
        );

        assertTrue(ex.getMessage().contains("explode"));
        assertTrue(ex.getCause() instanceof NoCodeException);
    }
}
