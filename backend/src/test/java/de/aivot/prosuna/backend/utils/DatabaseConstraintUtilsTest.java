package de.aivot.prosuna.backend.utils;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseConstraintUtilsTest {
    @Test
    void recognizesOnlyTheNamedUniqueConstraintThroughExceptionWrappers() {
        var failure = new DataIntegrityViolationException("save failed", new IllegalStateException(
                new ConstraintViolationException("duplicate", new SQLException("duplicate", "23505"), "access_key_unique")));

        assertTrue(DatabaseConstraintUtils.isUniqueViolation(failure, "access_key_unique"));
        assertFalse(DatabaseConstraintUtils.isUniqueViolation(failure, "case_number_unique"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"23502", "23503", "23514", "40001"})
    void doesNotTreatOtherSqlFailuresAsKeyCollisions(String sqlState) {
        var failure = new ConstraintViolationException("failure", new SQLException("failure", sqlState), "access_key_unique");

        assertFalse(DatabaseConstraintUtils.isUniqueViolation(failure, "access_key_unique"));
    }

    @Test
    void doesNotInferConstraintsFromErrorMessages() {
        assertFalse(DatabaseConstraintUtils.isUniqueViolation(
                new DataIntegrityViolationException("duplicate access_key_unique"), "access_key_unique"));
        assertFalse(DatabaseConstraintUtils.isUniqueViolation(
                new ConstraintViolationException("duplicate", new SQLException("duplicate", "23505"), null), "access_key_unique"));
    }
}
