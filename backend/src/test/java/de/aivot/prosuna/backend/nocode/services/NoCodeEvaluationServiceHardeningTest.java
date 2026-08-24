package de.aivot.prosuna.backend.nocode.services;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeExpression;
import de.aivot.prosuna.backend.nocode.models.NoCodeInstanceDataReference;
import de.aivot.prosuna.backend.nocode.models.NoCodeNodeDataReference;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeProcessDataReference;
import de.aivot.prosuna.backend.nocode.models.NoCodeReference;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import de.aivot.prosuna.backend.nocode.providers.NoCodeOperatorsProvider;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.List;

import static de.aivot.prosuna.backend.TestData.runtime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoCodeEvaluationServiceHardeningTest {
    @Test
    void shouldReturnNullForMissingReference() {
        var service = new NoCodeEvaluationService(List.of());
        var result = service.evaluate(new NoCodeReference("missing"), new DerivedRuntimeElementData());

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
            public String getAbstract() {
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
                            protected NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) {
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
                new DerivedRuntimeElementData()
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
            public String getAbstract() {
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
                            protected NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
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
                        new DerivedRuntimeElementData()
                )
        );

        assertTrue(ex.getMessage().contains("explode"));
        assertTrue(ex.getCause() instanceof NoCodeException);
    }

    @Test
    void shouldResolveProcessDataReferencesFromContext() {
        var service = new NoCodeEvaluationService(List.of());
        var context = ProcessExecutionData.of(Map.<String, Object>of(
                "$", Map.of("applicant", Map.of("name", "Ada")),
                "$$", Map.of("instanceId", "i-1"),
                "_", Map.of(
                        "nodeA", Map.of("score", 7),
                        "nodeB", List.of("x", "y")
                )
        ));

        var processResult = service.evaluate(
                new NoCodeProcessDataReference()
                        .setPath("applicant.name"),
                new DerivedRuntimeElementData(),
                context
        );
        assertEquals("Ada", processResult.getValue());

        var instanceResult = service.evaluate(
                new NoCodeInstanceDataReference()
                        .setPath("instanceId"),
                new DerivedRuntimeElementData(),
                context
        );
        assertEquals("i-1", instanceResult.getValue());

        var nodeResult = service.evaluate(
                new NoCodeNodeDataReference()
                        .setNodeDataKey("nodeA")
                        .setPath("score"),
                new DerivedRuntimeElementData(),
                context
        );
        assertEquals(7, nodeResult.getValue());

        var nodeListIndexResult = service.evaluate(
                new NoCodeNodeDataReference()
                        .setNodeDataKey("nodeB")
                        .setPath("1"),
                new DerivedRuntimeElementData(),
                context
        );
        assertEquals("y", nodeListIndexResult.getValue());
    }

    @Test
    void shouldResolveWildcardReferencesUsingProvidedBinding() {
        var service = new NoCodeEvaluationService(List.of());
        var context = ProcessExecutionData.of(Map.of(
                "$", Map.of(
                        "personen", List.of(
                                Map.of("alter", 22),
                                Map.of("alter", 41)
                        )
                )
        ));

        var result = service.evaluate(
                new NoCodeProcessDataReference().setPath("personen.*.alter"),
                new DerivedRuntimeElementData(),
                context,
                List.of(1)
        );

        assertEquals(41, result.getValue());
    }

    @Test
    void shouldRejectImplicitWildcardReferenceWithoutBinding() {
        var service = new NoCodeEvaluationService(List.of());
        var context = ProcessExecutionData.of(Map.of(
                "$", Map.of(
                        "personen", List.of(
                                Map.of("alter", 22)
                        )
                )
        ));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.evaluate(
                        new NoCodeProcessDataReference().setPath("personen.*.alter"),
                        new DerivedRuntimeElementData(),
                        context
                )
        );
    }
}
