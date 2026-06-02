package de.aivot.GoverBackend.process.exceptions;

import jakarta.annotation.Nonnull;


public class ProcessNodeExecutionExceptionTimeout extends ProcessNodeExecutionException {
    public ProcessNodeExecutionExceptionTimeout(@Nonnull String message) {
        super(message);
    }

    public ProcessNodeExecutionExceptionTimeout(@Nonnull Throwable cause, @Nonnull String message) {
        super(message, cause);
    }

    public ProcessNodeExecutionExceptionTimeout(@Nonnull Throwable cause) {
        super(cause);
    }

    public ProcessNodeExecutionExceptionTimeout(@Nonnull String format, @Nonnull Object... args) {
        super(format, args);
    }

    public ProcessNodeExecutionExceptionTimeout(@Nonnull Throwable cause, @Nonnull String format, @Nonnull Object... args) {
        super(cause, format, args);
    }
}
