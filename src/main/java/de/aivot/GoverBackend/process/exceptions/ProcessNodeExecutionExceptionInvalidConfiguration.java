package de.aivot.GoverBackend.process.exceptions;

import jakarta.annotation.Nonnull;


public class ProcessNodeExecutionExceptionInvalidConfiguration extends ProcessNodeExecutionException {
    public ProcessNodeExecutionExceptionInvalidConfiguration(@Nonnull String message) {
        super(message);
    }

    public ProcessNodeExecutionExceptionInvalidConfiguration(@Nonnull Throwable cause, @Nonnull String message) {
        super(cause, message);
    }

    public ProcessNodeExecutionExceptionInvalidConfiguration(@Nonnull Throwable cause) {
        super(cause);
    }

    public ProcessNodeExecutionExceptionInvalidConfiguration(@Nonnull String format, @Nonnull Object... args) {
        super(format, args);
    }

    public ProcessNodeExecutionExceptionInvalidConfiguration(@Nonnull Throwable cause, @Nonnull String format, @Nonnull Object... args) {
        super(cause, format, args);
    }

}
