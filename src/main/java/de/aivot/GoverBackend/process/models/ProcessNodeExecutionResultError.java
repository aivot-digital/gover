package de.aivot.GoverBackend.process.models;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class ProcessNodeExecutionResultError extends ProcessNodeExecutionResult {
    @Nullable
    private String message;

    @Nullable
    private Throwable throwable;

    // region constructor

    public ProcessNodeExecutionResultError() {
        this.message = "Es ist ein unbekannter Fehler aufgetreten.";
    }

    public ProcessNodeExecutionResultError(@Nullable String message,
                                           @Nullable Throwable throwable) {
        this.message = message;
        this.throwable = throwable;
    }

    // endregion

    // region factory methods

    public static ProcessNodeExecutionResultError of(@Nonnull String message) {
        return new ProcessNodeExecutionResultError(message, null);
    }

    public static ProcessNodeExecutionResultError of(@Nonnull Throwable throwable) {
        return new ProcessNodeExecutionResultError(null, throwable);
    }

    // endregion


    @Nullable
    public String getMessage() {
        return message;
    }

    public ProcessNodeExecutionResultError setMessage(@Nullable String message) {
        this.message = message;
        return this;
    }

    @Nullable
    public Throwable getThrowable() {
        return throwable;
    }

    public ProcessNodeExecutionResultError setThrowable(@Nullable Throwable throwable) {
        this.throwable = throwable;
        return this;
    }
}
