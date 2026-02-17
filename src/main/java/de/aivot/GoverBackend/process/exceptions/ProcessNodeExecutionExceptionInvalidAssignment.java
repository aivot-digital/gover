package de.aivot.GoverBackend.process.exceptions;

import jakarta.annotation.Nonnull;


public class ProcessNodeExecutionExceptionInvalidAssignment extends ProcessNodeExecutionException {
    public ProcessNodeExecutionExceptionInvalidAssignment(@Nonnull String message) {
        super(message);
    }

    public ProcessNodeExecutionExceptionInvalidAssignment(@Nonnull Throwable cause, @Nonnull String message) {
        super(cause, message);
    }

    public ProcessNodeExecutionExceptionInvalidAssignment(@Nonnull Throwable cause) {
        super(cause);
    }

    public ProcessNodeExecutionExceptionInvalidAssignment(@Nonnull Throwable cause, @Nonnull String format, @Nonnull Object... args) {
        super(cause, format, args);
    }

    public ProcessNodeExecutionExceptionInvalidAssignment(@Nonnull String format, @Nonnull Object... args) {
        super(format, args);
    }

}
