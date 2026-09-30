package de.aivot.prosuna.backend.process.models.executionResult;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class ProcessNodeExecutionResultTaskCompleted extends ProcessNodeExecutionResult {
    @Nonnull
    private String viaPort;

    @Nullable
    private ProcessNodeCompletionHistory completionHistory;

    @Nullable
    public ProcessNodeCompletionHistory getCompletionHistory() {
        return completionHistory;
    }

    public ProcessNodeExecutionResultTaskCompleted setCompletionHistory(@Nullable ProcessNodeCompletionHistory completionHistory) {
        this.completionHistory = completionHistory;
        return this;
    }

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
