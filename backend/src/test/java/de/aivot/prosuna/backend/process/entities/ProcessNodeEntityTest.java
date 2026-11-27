package de.aivot.prosuna.backend.process.entities;

import de.aivot.prosuna.backend.process.utils.ProcessNodeDataKeyUtils;
import jakarta.validation.Validation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessNodeEntityTest {
    @ParameterizedTest
    @ValueSource(strings = {"name", "Name_2", "_name", "$name", "name$2", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
    void acceptsIdentifiersUsableInDotNotation(String key) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validateValue(ProcessNodeEntity.class, "dataKey", key).isEmpty());
        }
        assertNull(ProcessNodeDataKeyUtils.getValidationError(key));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "name-2", "12name", "person.name", "items[0]", "name space", "Änderung", "name@", "name\n", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
    void rejectsInvalidIdentifiersAtTheApiBoundaryAndInServiceValidation(String key) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertFalse(factory.getValidator().validateValue(ProcessNodeEntity.class, "dataKey", key).isEmpty());
        }
        assertNotNull(ProcessNodeDataKeyUtils.getValidationError(key));
    }
}
