package de.aivot.prosuna.backend.process.exceptions;

import jakarta.annotation.Nonnull;


public class ProcessNodeExecutionExceptionMissingValue extends ProcessNodeExecutionException {
    public ProcessNodeExecutionExceptionMissingValue(@Nonnull String message) {
        super(message);
    }

    public ProcessNodeExecutionExceptionMissingValue(@Nonnull Throwable cause, @Nonnull String message) {
        super(message, cause);
    }

    public ProcessNodeExecutionExceptionMissingValue(@Nonnull Throwable cause) {
        super(cause);
    }

    public ProcessNodeExecutionExceptionMissingValue(@Nonnull String format, @Nonnull Object... args) {
        super(format, args);
    }

    public ProcessNodeExecutionExceptionMissingValue(@Nonnull Throwable cause, @Nonnull String format, @Nonnull Object... args) {
        super(cause, format, args);
    }
}
