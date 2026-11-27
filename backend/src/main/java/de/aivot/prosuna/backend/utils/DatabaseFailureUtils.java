package de.aivot.prosuna.backend.utils;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.exception.JDBCConnectionException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.transaction.CannotCreateTransactionException;

import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public final class DatabaseFailureUtils {
    // PostgreSQL states of failures that can succeed when the same work runs again: connection exceptions,
    // insufficient resources, a server shutdown or restart, serialization failures, deadlocks and lock timeouts.
    private static final Set<String> TRANSIENT_SQL_STATE_CLASSES = Set.of("08", "53");
    private static final Set<String> TRANSIENT_SQL_STATES = Set.of("57P01", "57P02", "57P03", "40001", "40P01", "55P03");

    private DatabaseFailureUtils() {
    }

    /**
     * Whether a failure was caused by the database being temporarily unavailable or contended, so running the same
     * work again later can succeed. Spring and Hibernate wrap the cause differently depending on whether a statement,
     * the connection or the commit failed, e.g. in a {@code DataAccessResourceFailureException} or a
     * {@code TransactionSystemException}, so the whole cause chain is inspected. Failures that would repeat, such as
     * constraint violations, are not transient.
     */
    public static boolean isTransient(@Nonnull Throwable failure) {
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable cause = failure; cause != null && visited.add(cause); cause = cause.getCause()) {
            if (cause instanceof TransientDataAccessException
                    || cause instanceof RecoverableDataAccessException
                    || cause instanceof CannotCreateTransactionException
                    || cause instanceof CannotGetJdbcConnectionException
                    || cause instanceof JDBCConnectionException
                    || cause instanceof SQLTransientException
                    || cause instanceof SQLRecoverableException) {
                return true;
            }
            if (cause instanceof SQLException sqlException && isTransientSqlState(sqlException.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTransientSqlState(@Nullable String sqlState) {
        return sqlState != null && sqlState.length() == 5
                && (TRANSIENT_SQL_STATE_CLASSES.contains(sqlState.substring(0, 2)) || TRANSIENT_SQL_STATES.contains(sqlState));
    }
}
