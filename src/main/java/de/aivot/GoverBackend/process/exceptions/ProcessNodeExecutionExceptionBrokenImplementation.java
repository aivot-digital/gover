package de.aivot.GoverBackend.process.exceptions;

import jakarta.annotation.Nonnull;

/**
 * Exception indicating an unknown error occurred during the execution of a process node.
 * This exception captures the context of the process, node, instance, task, and the user who triggered the execution.
 */
public class ProcessNodeExecutionExceptionBrokenImplementation extends ProcessNodeExecutionException {
    public ProcessNodeExecutionExceptionBrokenImplementation(@Nonnull String message) {
        super(message);
    }

    public ProcessNodeExecutionExceptionBrokenImplementation(@Nonnull Throwable cause) {
        super(cause);
    }

    public ProcessNodeExecutionExceptionBrokenImplementation(@Nonnull String format, @Nonnull Object... args) {
        super(format, args);
    }
}
