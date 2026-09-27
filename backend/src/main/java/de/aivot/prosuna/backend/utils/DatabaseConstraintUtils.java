package de.aivot.prosuna.backend.utils;

import jakarta.annotation.Nonnull;
import org.hibernate.exception.ConstraintViolationException;

public final class DatabaseConstraintUtils {
    private DatabaseConstraintUtils() {
    }

    public static boolean isUniqueViolation(@Nonnull Throwable exception, @Nonnull String constraintName) {
        // Inspect the database failure itself: a separate existence query can misclassify concurrent
        // changes and cannot safely run inside a transaction whose insert has already failed.
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException constraint
                    && "23505".equals(constraint.getSQLState())
                    && constraintName.equals(constraint.getConstraintName())) {
                return true;
            }
        }
        return false;
    }
}
