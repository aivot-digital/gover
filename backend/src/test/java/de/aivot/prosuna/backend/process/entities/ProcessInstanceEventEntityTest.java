package de.aivot.prosuna.backend.process.entities;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessInstanceEventEntityTest {
    @Test
    void historyRelevanceDefaultsToFalseAndCannotBeNull() {
        var event = new ProcessInstanceEventEntity();

        assertFalse(event.getHistoryRelevant());
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validateProperty(event, "isHistoryRelevant").isEmpty());
            assertEquals(1, validator.validateValue(ProcessInstanceEventEntity.class, "isHistoryRelevant", null).size());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"concernedUserId", "concernedIdentityId", "concernedIdentityTitle"})
    void concernedReferencesMayBeAbsent(String property) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validateProperty(new ProcessInstanceEventEntity(), property).isEmpty());
        }
    }

    @ParameterizedTest
    @CsvSource({"35, false", "36, true", "37, false"})
    void concernedUserIdMustHaveExactly36Characters(int length, boolean valid) {
        var event = new ProcessInstanceEventEntity().setConcernedUserId("a".repeat(length));

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertEquals(valid, factory.getValidator().validateProperty(event, "concernedUserId").isEmpty());
        }
    }

    @ParameterizedTest
    @CsvSource({"255, true", "256, false"})
    void concernedIdentityValuesMustNotExceed255Characters(int length, boolean valid) {
        var event = new ProcessInstanceEventEntity()
                .setConcernedIdentityId("a".repeat(length))
                .setConcernedIdentityTitle("b".repeat(length));

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertEquals(valid, validator.validateProperty(event, "concernedIdentityId").isEmpty());
            assertEquals(valid, validator.validateProperty(event, "concernedIdentityTitle").isEmpty());
        }
    }
}
