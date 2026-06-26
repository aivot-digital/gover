package de.aivot.GoverBackend.process.exceptions;

import jakarta.annotation.Nonnull;

public abstract class ProcessNodeExecutionException extends Exception {
    private boolean alreadyLogged = false;

    public ProcessNodeExecutionException(@Nonnull String message) {
        super(message);
    }

    public ProcessNodeExecutionException(@Nonnull Throwable cause, @Nonnull String message) {
        super(message, cause);
    }

    public ProcessNodeExecutionException(@Nonnull Throwable cause) {
        super(cause);
    }

    public ProcessNodeExecutionException(@Nonnull String format, @Nonnull Object... args) {
        super(String.format(format, args));
    }

    public ProcessNodeExecutionException(@Nonnull Throwable cause, @Nonnull String format, @Nonnull Object... args) {
        super(String.format(format, args), cause);
    }

    public boolean isAlreadyLogged() {
        return alreadyLogged;
    }

    public ProcessNodeExecutionException setAlreadyLogged(boolean alreadyLogged) {
        this.alreadyLogged = alreadyLogged;
        return this;
    }
}
