package de.aivot.prosuna.backend.utils;

import org.hibernate.exception.JDBCConnectionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionSystemException;

import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseFailureUtilsTest {
    @ParameterizedTest
    @ValueSource(strings = {"08006", "08003", "57P01", "57P03", "40001", "40P01", "55P03", "53300"})
    void recognizesTransientSqlStatesThroughExceptionWrappers(String sqlState) {
        var failure = new TransactionSystemException("Could not commit JPA transaction",
                new RuntimeException("Unable to commit against JDBC Connection", new SQLException("failure", sqlState)));

        assertTrue(DatabaseFailureUtils.isTransient(failure));
    }

    @Test
    void recognizesLostAndUnavailableConnections() {
        assertTrue(DatabaseFailureUtils.isTransient(new DataAccessResourceFailureException("could not execute statement",
                new JDBCConnectionException("could not execute statement", new SQLException("I/O error")))));
        assertTrue(DatabaseFailureUtils.isTransient(new CannotCreateTransactionException("Could not open JPA EntityManager",
                new SQLTransientConnectionException("Connection is not available, request timed out"))));
        assertTrue(DatabaseFailureUtils.isTransient(new CannotGetJdbcConnectionException("Failed to obtain JDBC Connection")));
        assertTrue(DatabaseFailureUtils.isTransient(new CannotAcquireLockException("lock timeout")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"23505", "22001", "42P01", "54001", "57014"})
    void doesNotTreatFailuresThatWouldRepeatAsTransient(String sqlState) {
        var failure = new JpaSystemException(new RuntimeException(new SQLException("failure", sqlState)));

        assertFalse(DatabaseFailureUtils.isTransient(failure));
    }

    @Test
    void doesNotTreatOtherFailuresAsTransient() {
        assertFalse(DatabaseFailureUtils.isTransient(new DataIntegrityViolationException("duplicate key")));
        assertFalse(DatabaseFailureUtils.isTransient(new IllegalStateException("node failed")));
        assertFalse(DatabaseFailureUtils.isTransient(new DataAccessResourceFailureException("statement too complex",
                new SQLException("statement too complex", "54001"))));
    }
}
