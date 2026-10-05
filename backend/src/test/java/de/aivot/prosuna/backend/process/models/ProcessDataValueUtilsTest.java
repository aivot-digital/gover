package de.aivot.prosuna.backend.process.models;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessDataValueUtilsTest {
    @ParameterizedTest
    @ValueSource(strings = {"person.name", "people[0].name", "matrix[1][2].value",
            "people[*].addresses[*].street", "$value._name"})
    void shouldRoundTripCanonicalPaths(String path) {
        var segments = ProcessDataValueUtils.parseDestinationKeySegments(path, false);
        assertEquals(path, ProcessDataValueUtils.formatDestinationKeySegments(segments));
    }

    @ParameterizedTest
    @ValueSource(strings = {"people.0.name", "people.*.name", "people[01]", "people[-1]", "people[1.5]",
            "people[]", "people[0", "people[0]name", "people..name", "people.", "people[index]",
            "people[\"name\"]", "people[0];alert(1)", "1name", "person-name", "people[2147483648]"})
    void shouldRejectUnsupportedSyntax(String path) {
        assertThrows(IllegalArgumentException.class,
                () -> ProcessDataValueUtils.parseDestinationKeySegments(path, false));
    }

    @Test
    void shouldNormalizeWhitespaceAndRespectRootAndWildcardPolicies() {
        assertEquals("people[0].name", ProcessDataValueUtils.formatDestinationKeySegments(
                ProcessDataValueUtils.parseDestinationKeySegments(" people [ 0 ] . name ", false)));
        assertFalse(ProcessDataValueUtils.isValidDestinationKey("[0].name", false, false));
        assertTrue(ProcessDataValueUtils.isValidDestinationKey("[0].name", true, false));
        assertFalse(ProcessDataValueUtils.isValidDestinationKey("people[*].name", false, false));
        assertTrue(ProcessDataValueUtils.isValidDestinationKey("people[*].name", false, true));
        assertFalse(ProcessDataValueUtils.isValidDestinationKey("", false, false));
    }

    @Test
    void shouldReadWriteAndMaterializeNestedArrays() {
        var root = ProcessDataValueUtils.writeDestinationKeyValue(null, "[1][0].name", "Ada");
        assertEquals("Ada", ProcessDataValueUtils.resolveDestinationKeyValue(root, "[1][0].name"));
        assertNull(ProcessDataValueUtils.resolveDestinationKeyValue(root, "[0]"));
        assertEquals("[1][0].name", ProcessDataValueUtils.materializeDestinationKey("[*][*].name", List.of(1, 0), true));
        var matches = ProcessDataValueUtils.resolveMatchingDestinationKeyValues(
                Map.of("people", List.of(Map.of("name", "Ada"))), "people[*].name");
        assertEquals("people[0].name", matches.getFirst().destinationKey());
        assertEquals("Ada", matches.getFirst().value());
    }
}
