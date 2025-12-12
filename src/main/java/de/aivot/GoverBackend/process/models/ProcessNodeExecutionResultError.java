package de.aivot.GoverBackend.process.models;

import jakarta.annotation.Nonnull;

public class ProcessNodeExecutionResultError extends ProcessNodeExecutionResult {
    @Nonnull
    private String message;

    // region constructor

    public ProcessNodeExecutionResultError() {
        this.message = "Es ist ein unbekannter Fehler aufgetreten.";
    }

    public ProcessNodeExecutionResultError(@Nonnull String message) {
        this.message = message;
    }

    // endregion

    // region factory methods

    public static ProcessNodeExecutionResultError of(@Nonnull String message) {
        return new ProcessNodeExecutionResultError(message);
    }

    public static ProcessNodeExecutionResultError of(@Nonnull Throwable throwable) {
        return new ProcessNodeExecutionResultError(throwable.getLocalizedMessage());
    }

    // endregion

    @Nonnull
    public String getMessage() {
        return message;
    }

    public ProcessNodeExecutionResultError setMessage(@Nonnull String message) {
        this.message = message;
        return this;
    }
}
