package de.aivot.GoverBackend.process.exceptions;

import jakarta.annotation.Nonnull;

/**
 * Exception indicating an unknown error occurred during the execution of a process node.
 * This exception captures the context of the process, node, instance, task, and the user who triggered the execution.
 */
public class ProcessNodeExecutionExceptionUnknown extends ProcessNodeExecutionException {
    public ProcessNodeExecutionExceptionUnknown(@Nonnull String message) {
        super(message);
    }

    public ProcessNodeExecutionExceptionUnknown(@Nonnull Throwable cause, @Nonnull String message) {
        super(cause, message);
    }

    public ProcessNodeExecutionExceptionUnknown(@Nonnull Throwable cause) {
        super(cause);
    }

    public ProcessNodeExecutionExceptionUnknown(@Nonnull String format, @Nonnull Object... args) {
        super(format, args);
    }

    public ProcessNodeExecutionExceptionUnknown(@Nonnull Throwable cause, @Nonnull String format, @Nonnull Object... args) {
        super(cause, format, args);
    }

}
