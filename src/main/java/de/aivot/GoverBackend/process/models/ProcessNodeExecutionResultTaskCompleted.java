package de.aivot.GoverBackend.process.models;

import jakarta.annotation.Nonnull;

public class ProcessNodeExecutionResultTaskCompleted extends ProcessNodeExecutionResult {
    @Nonnull
    private String viaPort;

    // region constructor

    public ProcessNodeExecutionResultTaskCompleted() {
        this.viaPort = "default";
    }

    public ProcessNodeExecutionResultTaskCompleted(@Nonnull String viaPort) {
        this.viaPort = viaPort;
    }

    // endregion

    // region factory methods

    public static ProcessNodeExecutionResultTaskCompleted of(@Nonnull String viaPort) {
        return new ProcessNodeExecutionResultTaskCompleted(viaPort);
    }

    // endregion

    @Nonnull
    public String getViaPort() {
        return viaPort;
    }

    public ProcessNodeExecutionResultTaskCompleted setViaPort(@Nonnull String viaPort) {
        this.viaPort = viaPort;
        return this;
    }
}
