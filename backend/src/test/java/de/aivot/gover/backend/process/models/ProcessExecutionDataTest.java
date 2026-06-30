package de.aivot.gover.backend.process.models;

import de.aivot.gover.backend.process.models.ProcessDataValueUtils;
import de.aivot.gover.backend.process.models.ProcessExecutionData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessExecutionDataTest {
    @Test
    void shouldResolveNestedObjectValueFromProcessDataRoot() {
        var processExecutionData = ProcessExecutionData.of(Map.of(
                ProcessExecutionData.PROCESS_DATA_KEY, Map.of(
                        "person", Map.of(
                                "vorname", "Ada"
                        )
                )
        ));

        var result = ProcessDataValueUtils.resolveProcessDataValue(processExecutionData, "person.vorname");

        assertEquals("Ada", result);
    }

    @Test
    void shouldResolveDestinationKeyValueFromArrayRoot() {
        var result = ProcessDataValueUtils.resolveDestinationKeyValue(
                List.of(
                        Map.of("vorname", "Ada"),
                        Map.of("vorname", "Grace")
                ),
                "1.vorname"
        );

        assertEquals("Grace", result);
    }

    @Test
    void shouldRejectImplicitWildcardReads() {
        var processExecutionData = ProcessExecutionData.of(Map.of(
                ProcessExecutionData.PROCESS_DATA_KEY, Map.of(
                        "personen", List.of(
                                Map.of("vorname", "Ada"),
                                Map.of("vorname", "Grace")
                        )
                )
        ));

        assertThrows(
                IllegalArgumentException.class,
                () -> ProcessDataValueUtils.resolveProcessDataValue(processExecutionData, "personen.*.vorname")
        );
    }

    @Test
    void shouldResolveExplicitArrayIndexFromDestinationKeyPath() {
        var processExecutionData = ProcessExecutionData.of(Map.of(
                ProcessExecutionData.PROCESS_DATA_KEY, Map.of(
                        "personen", List.of(
                                Map.of("vorname", "Ada"),
                                Map.of("vorname", "Grace")
                        )
                )
        ));

        var result = ProcessDataValueUtils.resolveProcessDataValue(processExecutionData, "personen.1.vorname");

        assertEquals("Grace", result);
    }

    @Test
    void shouldResolveNestedWildcardsUsingProvidedIndices() {
        var processExecutionData = ProcessExecutionData.of(Map.of(
                ProcessExecutionData.PROCESS_DATA_KEY, Map.of(
                        "personen", List.of(
                                Map.of("adressen", List.of(
                                        Map.of("strasse", "First Person Street 0"),
                                        Map.of("strasse", "First Person Street 1")
                                )),
                                Map.of("adressen", List.of(
                                        Map.of("strasse", "Second Person Street 0"),
                                        Map.of("strasse", "Second Person Street 1")
                                ))
                        )
                )
        ));

        var result = ProcessDataValueUtils.resolveProcessDataValue(
                processExecutionData,
                "personen.*.adressen.*.strasse",
                List.of(1, 0)
        );

        assertEquals("Second Person Street 0", result);
    }

    @Test
    void shouldResolveMixedExplicitAndWildcardArrayPathUsingProvidedIndices() {
        var processExecutionData = ProcessExecutionData.of(Map.of(
                ProcessExecutionData.PROCESS_DATA_KEY, Map.of(
                        "personen", List.of(
                                Map.of("adressen", List.of(
                                        Map.of("strasse", "First Person Street 0"),
                                        Map.of("strasse", "First Person Street 1")
                                )),
                                Map.of("adressen", List.of(
                                        Map.of("strasse", "Second Person Street 0"),
                                        Map.of("strasse", "Second Person Street 1")
                                ))
                        )
                )
        ));

        var result = ProcessDataValueUtils.resolveProcessDataValue(
                processExecutionData,
                "personen.1.adressen.*.strasse",
                List.of(1)
        );

        assertEquals("Second Person Street 1", result);
    }

    @Test
    void shouldResolveAllExistingWildcardBindings() {
        var processExecutionData = ProcessExecutionData.of(Map.of(
                ProcessExecutionData.PROCESS_DATA_KEY, Map.of(
                        "personen", List.of(
                                Map.of("vorname", "Ada"),
                                Map.of("vorname", "Grace")
                        )
                )
        ));

        var result = ProcessDataValueUtils.resolveMatchingProcessDataValues(
                processExecutionData,
                "personen.*.vorname"
        );

        assertEquals(
                List.of("personen.0.vorname", "personen.1.vorname"),
                result.stream().map(ProcessDataValueUtils.ResolvedProcessDataValue::destinationKey).toList()
        );
        assertEquals(
                List.of(List.of(0), List.of(1)),
                result.stream().map(ProcessDataValueUtils.ResolvedProcessDataValue::wildcardIndices).toList()
        );
        assertEquals(
                List.of("Ada", "Grace"),
                result.stream().map(ProcessDataValueUtils.ResolvedProcessDataValue::value).toList()
        );
    }

    @Test
    void shouldResolveAllExistingWildcardBindingsFromArrayRoot() {
        var result = ProcessDataValueUtils.resolveMatchingDestinationKeyValues(
                List.of(
                        Map.of("vorname", "Ada"),
                        Map.of("vorname", "Grace")
                ),
                "*.vorname"
        );

        assertEquals(
                List.of("0.vorname", "1.vorname"),
                result.stream().map(ProcessDataValueUtils.ResolvedDestinationKeyValue::destinationKey).toList()
        );
        assertEquals(
                List.of(List.of(0), List.of(1)),
                result.stream().map(ProcessDataValueUtils.ResolvedDestinationKeyValue::wildcardIndices).toList()
        );
        assertEquals(
                List.of("Ada", "Grace"),
                result.stream().map(ProcessDataValueUtils.ResolvedDestinationKeyValue::value).toList()
        );
    }

    @Test
    void shouldKeepWildcardBindingWhenLeafValueIsMissing() {
        var processExecutionData = ProcessExecutionData.of(Map.of(
                ProcessExecutionData.PROCESS_DATA_KEY, Map.of(
                        "personen", List.of(
                                Map.of("alter", 22),
                                Map.of("name", "Gerda")
                        )
                )
        ));

        var result = ProcessDataValueUtils.resolveMatchingProcessDataValues(
                processExecutionData,
                "personen.*.alter"
        );

        assertEquals(
                List.of(List.of(0), List.of(1)),
                result.stream().map(ProcessDataValueUtils.ResolvedProcessDataValue::wildcardIndices).toList()
        );
        assertEquals(
                java.util.Arrays.asList(22, null),
                result.stream().map(ProcessDataValueUtils.ResolvedProcessDataValue::value).toList()
        );
    }

    @Test
    void shouldReturnNullForMissingArrayIndexOrSegment() {
        var processExecutionData = ProcessExecutionData.of(Map.of(
                ProcessExecutionData.PROCESS_DATA_KEY, Map.of(
                        "personen", List.of(
                                Map.of("vorname", "Ada")
                        )
                )
        ));

        assertNull(ProcessDataValueUtils.resolveProcessDataValue(processExecutionData, "personen.1.vorname"));
        assertNull(ProcessDataValueUtils.resolveProcessDataValue(processExecutionData, "personen.0.nachname"));
    }

    @Test
    void shouldReturnNullForOutOfRangeWildcardIndex() {
        var processExecutionData = ProcessExecutionData.of(Map.of(
                ProcessExecutionData.PROCESS_DATA_KEY, Map.of(
                        "personen", List.of(
                                Map.of("vorname", "Ada")
                        )
                )
        ));

        var result = ProcessDataValueUtils.resolveProcessDataValue(
                processExecutionData,
                "personen.*.vorname",
                List.of(1)
        );

        assertNull(result);
    }

    @Test
    void shouldReturnFullProcessDataRootForBlankDestinationKey() {
        var processDataRoot = Map.of("person", Map.of("vorname", "Ada"));
        var processExecutionData = ProcessExecutionData.of(Map.of(
                ProcessExecutionData.PROCESS_DATA_KEY, processDataRoot
        ));

        var result = ProcessDataValueUtils.resolveProcessDataValue(processExecutionData, null);

        assertSame(processDataRoot, result);
    }

    @Test
    void shouldMaterializeWildcardDestinationKeyFromIndices() {
        var result = ProcessDataValueUtils.materializeDestinationKey(
                "personen.*.adressen.*.strasse",
                List.of(1, 0)
        );

        assertEquals("personen.1.adressen.0.strasse", result);
    }

    @Test
    void shouldThrowForInvalidReadWildcardIndices() {
        var processExecutionData = new ProcessExecutionData();

        assertThrows(
                IllegalArgumentException.class,
                () -> ProcessDataValueUtils.resolveProcessDataValue(processExecutionData, "personen.*.vorname", List.of())
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ProcessDataValueUtils.resolveProcessDataValue(processExecutionData, "personen.*.vorname", List.of(0, 1))
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ProcessDataValueUtils.resolveProcessDataValue(processExecutionData, "personen.*.vorname", List.of(-1))
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ProcessDataValueUtils.resolveProcessDataValue(processExecutionData, "person.vorname", List.of(0))
        );
    }

    @Test
    void shouldWriteNestedObjectValueIntoProcessDataRoot() {
        var processExecutionData = new ProcessExecutionData();

        ProcessDataValueUtils.writeProcessDataValue(processExecutionData, "person.vorname", "Ada");

        assertEquals(
                Map.of(
                        "person", Map.of(
                                "vorname", "Ada"
                        )
                ),
                processExecutionData.getProcessData()
        );
    }

    @Test
    void shouldWriteDestinationKeyValueIntoArrayRoot() {
        var result = ProcessDataValueUtils.writeDestinationKeyValue(null, "1.vorname", "Grace");

        assertEquals(
                java.util.Arrays.asList(
                        null,
                        Map.of("vorname", "Grace")
                ),
                result
        );
    }

    @Test
    void shouldWriteExplicitArrayIndexAndGrowSparseArray() {
        var processExecutionData = new ProcessExecutionData();

        ProcessDataValueUtils.writeProcessDataValue(processExecutionData, "personen.1.vorname", "Grace");

        assertEquals(2, ((List<?>) processExecutionData.getProcessData().get("personen")).size());

        @SuppressWarnings("unchecked")
        var personen = (List<Object>) processExecutionData.getProcessData().get("personen");
        assertNull(personen.get(0));
        assertEquals(Map.of("vorname", "Grace"), personen.get(1));
    }

    @Test
    void shouldRejectImplicitWildcardWrites() {
        var processExecutionData = ProcessExecutionData.of(Map.of(
                ProcessExecutionData.PROCESS_DATA_KEY, Map.of(
                        "personen", List.of(
                                new java.util.LinkedHashMap<>(Map.of("nachname", "Lovelace")),
                                new java.util.LinkedHashMap<>(Map.of("nachname", "Hopper"))
                        )
                )
        ));

        assertThrows(
                IllegalArgumentException.class,
                () -> ProcessDataValueUtils.writeProcessDataValue(processExecutionData, "personen.*.vorname", "Ada")
        );
    }

    @Test
    void shouldWriteWildcardValueUsingExplicitIndexBinding() {
        var processExecutionData = ProcessExecutionData.of(Map.of(
                ProcessExecutionData.PROCESS_DATA_KEY, Map.of(
                        "personen", List.of(
                                new java.util.LinkedHashMap<>(Map.of("vorname", "Ada")),
                                new java.util.LinkedHashMap<>(Map.of("vorname", "Grace"))
                        )
                )
        ));

        ProcessDataValueUtils.writeProcessDataValue(
                processExecutionData,
                "personen.*.vorname",
                "Updated",
                List.of(1)
        );

        assertEquals(
                List.of(
                        Map.of("vorname", "Ada"),
                        Map.of("vorname", "Updated")
                ),
                processExecutionData.getProcessData().get("personen")
        );
    }

    @Test
    void shouldWriteNestedWildcardValueUsingExplicitIndexBindings() {
        var processExecutionData = new ProcessExecutionData();

        ProcessDataValueUtils.writeProcessDataValue(
                processExecutionData,
                "personen.*.adressen.*.strasse",
                "Updated",
                List.of(1, 2)
        );

        @SuppressWarnings("unchecked")
        var personen = (List<Object>) processExecutionData.getProcessData().get("personen");
        assertEquals(2, personen.size());
        assertNull(personen.get(0));

        @SuppressWarnings("unchecked")
        var secondPerson = (Map<String, Object>) personen.get(1);
        @SuppressWarnings("unchecked")
        var adressen = (List<Object>) secondPerson.get("adressen");
        assertEquals(3, adressen.size());
        assertNull(adressen.get(0));
        assertNull(adressen.get(1));
        assertEquals(Map.of("strasse", "Updated"), adressen.get(2));
    }

    @Test
    void shouldRemoveNestedObjectValueWithoutCleaningParents() {
        var processExecutionData = ProcessExecutionData.of(Map.of(
                ProcessExecutionData.PROCESS_DATA_KEY, Map.of(
                        "person", Map.of(
                                "firstName", "Ada"
                        )
                )
        ));

        var removed = ProcessDataValueUtils.removeProcessDataValue(
                processExecutionData,
                "person.firstName",
                false
        );

        assertTrue(removed);
        assertEquals(
                Map.of(
                        "person", Map.of()
                ),
                processExecutionData.getProcessData()
        );
    }

    @Test
    void shouldRemoveNestedObjectValueAndPruneEmptyContainers() {
        var processExecutionData = ProcessExecutionData.of(Map.of(
                ProcessExecutionData.PROCESS_DATA_KEY, Map.of(
                        "person", Map.of(
                                "firstName", "Ada"
                        )
                )
        ));

        var removed = ProcessDataValueUtils.removeProcessDataValue(
                processExecutionData,
                "person.firstName",
                true
        );

        assertTrue(removed);
        assertEquals(
                Map.of(),
                processExecutionData.getProcessData()
        );
    }

    @Test
    void shouldRemoveExplicitArrayElementAndCompactList() {
        var processExecutionData = ProcessExecutionData.of(Map.of(
                ProcessExecutionData.PROCESS_DATA_KEY, Map.of(
                        "personen", List.of(
                                "Ada",
                                "Grace",
                                "Margaret"
                        )
                )
        ));

        var removed = ProcessDataValueUtils.removeProcessDataValue(
                processExecutionData,
                "personen.1",
                false
        );

        assertTrue(removed);
        assertEquals(
                Map.of(
                        "personen", List.of("Ada", "Margaret")
                ),
                processExecutionData.getProcessData()
        );
    }

    @Test
    void shouldRemoveWildcardLeafFromAllArrayItemsWithoutCleanup() {
        var processExecutionData = ProcessExecutionData.of(Map.of(
                ProcessExecutionData.PROCESS_DATA_KEY, Map.of(
                        "personen", List.of(
                                Map.of("alter", 22),
                                Map.of("alter", 41)
                        )
                )
        ));

        var removed = ProcessDataValueUtils.removeProcessDataValue(
                processExecutionData,
                "personen.*.alter",
                false
        );

        assertTrue(removed);
        assertEquals(
                Map.of(
                        "personen", List.of(
                                Map.of(),
                                Map.of()
                        )
                ),
                processExecutionData.getProcessData()
        );
    }

    @Test
    void shouldRemoveWildcardLeafFromAllArrayItemsAndPruneEmptyContainers() {
        var processExecutionData = ProcessExecutionData.of(Map.of(
                ProcessExecutionData.PROCESS_DATA_KEY, Map.of(
                        "personen", List.of(
                                Map.of("alter", 22),
                                Map.of("alter", 41)
                        )
                )
        ));

        var removed = ProcessDataValueUtils.removeProcessDataValue(
                processExecutionData,
                "personen.*.alter",
                true
        );

        assertTrue(removed);
        assertEquals(
                Map.of(),
                processExecutionData.getProcessData()
        );
    }

    @Test
    void shouldRemoveWildcardLeafFromListAndPruneParentContainer() {
        var processExecutionData = ProcessExecutionData.of(Map.of(
                ProcessExecutionData.PROCESS_DATA_KEY, Map.of(
                        "personen", List.of(
                                Map.of("name", "Ada"),
                                Map.of("name", "Grace")
                        )
                )
        ));

        var removed = ProcessDataValueUtils.removeProcessDataValue(
                processExecutionData,
                "personen.*",
                true
        );

        assertTrue(removed);
        assertEquals(
                Map.of(),
                processExecutionData.getProcessData()
        );
    }

    @Test
    void shouldRemoveWildcardValueUsingExplicitIndexBinding() {
        var processExecutionData = ProcessExecutionData.of(Map.of(
                ProcessExecutionData.PROCESS_DATA_KEY, Map.of(
                        "personen", List.of(
                                Map.of("alter", 22),
                                Map.of("alter", 41)
                        )
                )
        ));

        var removed = ProcessDataValueUtils.removeProcessDataValue(
                processExecutionData,
                "personen.*.alter",
                true,
                List.of(1)
        );

        assertTrue(removed);
        assertEquals(
                Map.of(
                        "personen", List.of(
                                Map.of("alter", 22)
                        )
                ),
                processExecutionData.getProcessData()
        );
    }

    @Test
    void shouldReturnFalseWhenRemovingMissingPath() {
        var processExecutionData = ProcessExecutionData.of(Map.of(
                ProcessExecutionData.PROCESS_DATA_KEY, Map.of(
                        "person", Map.of("firstName", "Ada")
                )
        ));

        var removed = ProcessDataValueUtils.removeProcessDataValue(
                processExecutionData,
                "person.lastName",
                true
        );

        assertFalse(removed);
        assertEquals(
                Map.of(
                        "person", Map.of("firstName", "Ada")
                ),
                processExecutionData.getProcessData()
        );
    }

    @Test
    void shouldThrowForInvalidPathSyntaxOrConflictingShapes() {
        var processExecutionData = ProcessExecutionData.of(Map.of(
                ProcessExecutionData.PROCESS_DATA_KEY, Map.of(
                        "person", "Ada",
                        "personen", Map.of("vorname", "Ada")
                )
        ));

        assertThrows(
                IllegalArgumentException.class,
                () -> ProcessDataValueUtils.writeProcessDataValue(processExecutionData, "personen[0].vorname", "Ada")
        );
        assertThrows(
                IllegalStateException.class,
                () -> ProcessDataValueUtils.writeProcessDataValue(processExecutionData, "person.vorname", "Ada")
        );
        assertThrows(
                IllegalStateException.class,
                () -> ProcessDataValueUtils.writeProcessDataValue(processExecutionData, "personen.0.vorname", "Ada")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ProcessDataValueUtils.writeProcessDataValue(processExecutionData, "person.vorname", "Ada", List.of(0))
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ProcessDataValueUtils.writeProcessDataValue(processExecutionData, "personen.*.vorname", "Ada", List.of())
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ProcessDataValueUtils.removeProcessDataValue(processExecutionData, "personen[0].vorname", true)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ProcessDataValueUtils.removeProcessDataValue(processExecutionData, "personen.*.vorname", true, List.of())
        );
    }
}
