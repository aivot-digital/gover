package de.aivot.prosuna.backend.nocode.models;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class NoCodePathReferenceTest {
    @ParameterizedTest
    @ValueSource(strings = {"person.name", "people[0].name", "matrix[0][1]"})
    void shouldAcceptConcretePathsInEverySource(String path) {
        assertTrue(new NoCodeProcessDataReference(path).validate().isValid());
        assertTrue(new NoCodeInstanceDataReference(path).validate().isValid());
        assertTrue(new NoCodeNodeDataReference("previous", path).validate().isValid());
    }

    @ParameterizedTest
    @ValueSource(strings = {"people.0.name", "people.*.name", "people[01]", "people[index]"})
    void shouldRejectTheOldOrComputedSyntaxInEverySource(String path) {
        assertFalse(new NoCodeProcessDataReference(path).validate().isValid());
        assertFalse(new NoCodeInstanceDataReference(path).validate().isValid());
        assertFalse(new NoCodeNodeDataReference("previous", path).validate().isValid());
    }

    @Test
    void shouldKeepWildcardBindingsAndArrayRootsSourceSpecific() {
        assertTrue(new NoCodeProcessDataReference("people[*].name").validate().isValid());
        assertFalse(new NoCodeInstanceDataReference("people[*].name").validate().isValid());
        assertFalse(new NoCodeNodeDataReference("previous", "people[*].name").validate().isValid());
        assertTrue(new NoCodeNodeDataReference("previous", "[0].name").validate().isValid());
        assertFalse(new NoCodeProcessDataReference("[0].name").validate().isValid());
    }
}
