package de.aivot.GoverBackend.process.exceptions;

import jakarta.annotation.Nonnull;


public class ProcessNodeExecutionExceptionMissingPermissions extends ProcessNodeExecutionException {
    public ProcessNodeExecutionExceptionMissingPermissions(@Nonnull String message) {
        super(message);
    }

    public ProcessNodeExecutionExceptionMissingPermissions(@Nonnull Throwable cause) {
        super(cause);
    }

    public ProcessNodeExecutionExceptionMissingPermissions(@Nonnull String format, @Nonnull Object... args) {
        super(format, args);
    }
}
