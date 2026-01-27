package de.aivot.GoverBackend.process.exceptions;

import jakarta.annotation.Nonnull;


public class ProcessNodeExecutionExceptionIO extends ProcessNodeExecutionException {
    public ProcessNodeExecutionExceptionIO(@Nonnull String message) {
        super(message);
    }

    public ProcessNodeExecutionExceptionIO(@Nonnull Throwable cause) {
        super(cause);
    }

    public ProcessNodeExecutionExceptionIO(@Nonnull String format, @Nonnull Object... args) {
        super(format, args);
    }
}
