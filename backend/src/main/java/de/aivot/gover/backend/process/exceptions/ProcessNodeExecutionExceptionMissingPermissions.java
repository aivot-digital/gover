package de.aivot.gover.backend.process.exceptions;

import jakarta.annotation.Nonnull;


public class ProcessNodeExecutionExceptionMissingPermissions extends ProcessNodeExecutionException {
    public ProcessNodeExecutionExceptionMissingPermissions(@Nonnull String message) {
        super(message);
    }

    public ProcessNodeExecutionExceptionMissingPermissions(@Nonnull Throwable cause, @Nonnull String message) {
        super(message, cause);
    }

    public ProcessNodeExecutionExceptionMissingPermissions(@Nonnull Throwable cause) {
        super(cause);
    }

    public ProcessNodeExecutionExceptionMissingPermissions(@Nonnull String format, @Nonnull Object... args) {
        super(format, args);
    }

    public ProcessNodeExecutionExceptionMissingPermissions(@Nonnull Throwable cause, @Nonnull String format, @Nonnull Object... args) {
        super(cause, format, args);
    }
}
