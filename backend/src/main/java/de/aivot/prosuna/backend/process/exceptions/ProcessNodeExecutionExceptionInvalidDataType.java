package de.aivot.prosuna.backend.process.exceptions;

import jakarta.annotation.Nonnull;


public class ProcessNodeExecutionExceptionInvalidDataType extends ProcessNodeExecutionException {
    public ProcessNodeExecutionExceptionInvalidDataType(@Nonnull String message) {
        super(message);
    }

    public ProcessNodeExecutionExceptionInvalidDataType(@Nonnull Throwable cause, @Nonnull String message) {
        super(message, cause);
    }

    public ProcessNodeExecutionExceptionInvalidDataType(@Nonnull Throwable cause) {
        super(cause);
    }

    public ProcessNodeExecutionExceptionInvalidDataType(@Nonnull String format, @Nonnull Object... args) {
        super(format, args);
    }

    public ProcessNodeExecutionExceptionInvalidDataType(@Nonnull Throwable cause, @Nonnull String format, @Nonnull Object... args) {
        super(cause, format, args);
    }
}
