package de.aivot.GoverBackend.process.models;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

        var result = ProcessExecutionData.resolveProcessDataValue(processExecutionData, "person.vorname");

        assertEquals("Ada", result);
    }

    @Test
    void shouldResolveFirstArrayEntryWhenWildcardIsUsed() {
        var processExecutionData = ProcessExecutionData.of(Map.of(
                ProcessExecutionData.PROCESS_DATA_KEY, Map.of(
                        "personen", List.of(
                                Map.of("vorname", "Ada"),
                                Map.of("vorname", "Grace")
                        )
                )
        ));

        var result = ProcessExecutionData.resolveProcessDataValue(processExecutionData, "personen.*.vorname");

        assertEquals("Ada", result);
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

        var result = ProcessExecutionData.resolveProcessDataValue(processExecutionData, "personen.1.vorname");

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

        var result = ProcessExecutionData.resolveProcessDataValue(
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

        var result = ProcessExecutionData.resolveProcessDataValue(
                processExecutionData,
                "personen.1.adressen.*.strasse",
                List.of(1)
        );

        assertEquals("Second Person Street 1", result);
    }

    @Test
    void shouldKeepWildcardReadCompatibilityWhenNoExplicitIndicesAreProvided() {
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

        var result = ProcessExecutionData.resolveProcessDataValue(
                processExecutionData,
                "personen.*.adressen.*.strasse"
        );

        assertEquals("First Person Street 0", result);
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

        assertNull(ProcessExecutionData.resolveProcessDataValue(processExecutionData, "personen.1.vorname"));
        assertNull(ProcessExecutionData.resolveProcessDataValue(processExecutionData, "personen.0.nachname"));
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

        var result = ProcessExecutionData.resolveProcessDataValue(
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

        var result = ProcessExecutionData.resolveProcessDataValue(processExecutionData, null);

        assertSame(processDataRoot, result);
    }

    @Test
    void shouldThrowForInvalidReadWildcardIndices() {
        var processExecutionData = new ProcessExecutionData();

        assertThrows(
                IllegalArgumentException.class,
                () -> ProcessExecutionData.resolveProcessDataValue(processExecutionData, "personen.*.vorname", List.of())
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ProcessExecutionData.resolveProcessDataValue(processExecutionData, "personen.*.vorname", List.of(0, 1))
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ProcessExecutionData.resolveProcessDataValue(processExecutionData, "personen.*.vorname", List.of(-1))
        );
    }

    @Test
    void shouldWriteNestedObjectValueIntoProcessDataRoot() {
        var processExecutionData = new ProcessExecutionData();

        ProcessExecutionData.writeProcessDataValue(processExecutionData, "person.vorname", "Ada");

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
    void shouldWriteExplicitArrayIndexAndGrowSparseArray() {
        var processExecutionData = new ProcessExecutionData();

        ProcessExecutionData.writeProcessDataValue(processExecutionData, "personen.1.vorname", "Grace");

        assertEquals(
                List.of(
                        null,
                        Map.of("vorname", "Grace")
                ),
                processExecutionData.getProcessData().get("personen")
        );
    }

    @Test
    void shouldBroadcastWildcardWriteToAllExistingArrayEntries() {
        var processExecutionData = ProcessExecutionData.of(Map.of(
                ProcessExecutionData.PROCESS_DATA_KEY, Map.of(
                        "personen", List.of(
                                new java.util.LinkedHashMap<>(Map.of("nachname", "Lovelace")),
                                new java.util.LinkedHashMap<>(Map.of("nachname", "Hopper"))
                        )
                )
        ));

        ProcessExecutionData.writeProcessDataValue(processExecutionData, "personen.*.vorname", "Ada");

        assertEquals(
                List.of(
                        Map.of("nachname", "Lovelace", "vorname", "Ada"),
                        Map.of("nachname", "Hopper", "vorname", "Ada")
                ),
                processExecutionData.getProcessData().get("personen")
        );
    }

    @Test
    void shouldWriteWildcardValueOnlyToSelectedIndices() {
        var processExecutionData = ProcessExecutionData.of(Map.of(
                ProcessExecutionData.PROCESS_DATA_KEY, Map.of(
                        "personen", List.of(
                                new java.util.LinkedHashMap<>(Map.of("vorname", "Ada")),
                                new java.util.LinkedHashMap<>(Map.of("vorname", "Grace"))
                        )
                )
        ));

        ProcessExecutionData.writeProcessDataValue(
                processExecutionData,
                "personen.*.vorname",
                "Updated",
                List.of(1, 3)
        );

        assertEquals(
                List.of(
                        Map.of("vorname", "Ada"),
                        Map.of("vorname", "Updated"),
                        null,
                        Map.of("vorname", "Updated")
                ),
                processExecutionData.getProcessData().get("personen")
        );
    }

    @Test
    void shouldLeaveEmptyArrayUntouchedWhenBroadcastingWildcardWrite() {
        var processExecutionData = ProcessExecutionData.of(Map.of(
                ProcessExecutionData.PROCESS_DATA_KEY, Map.of(
                        "personen", new java.util.ArrayList<>()
                )
        ));

        ProcessExecutionData.writeProcessDataValue(processExecutionData, "personen.*.vorname", "Ada");

        assertEquals(List.of(), processExecutionData.getProcessData().get("personen"));
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
                () -> ProcessExecutionData.writeProcessDataValue(processExecutionData, "personen[0].vorname", "Ada")
        );
        assertThrows(
                IllegalStateException.class,
                () -> ProcessExecutionData.writeProcessDataValue(processExecutionData, "person.vorname", "Ada")
        );
        assertThrows(
                IllegalStateException.class,
                () -> ProcessExecutionData.writeProcessDataValue(processExecutionData, "personen.0.vorname", "Ada")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ProcessExecutionData.writeProcessDataValue(processExecutionData, "person.vorname", "Ada", List.of(0))
        );
    }
}
