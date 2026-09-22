package de.aivot.prosuna.backend.process.models;

import de.aivot.prosuna.backend.identity.models.IdentityData;
import de.aivot.prosuna.backend.process.models.executionResult.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Map;

/** Versioned snapshot of the result to apply after acceptance. Contains no attachment streams. */
public record CommunicationContinuation(
        int version,
        @Nonnull String kind,
        @Nullable String viaPort,
        @Nullable String identityId,
        @Nullable String transactionKey,
        @Nullable String paymentProviderName,
        @Nullable Map<String, Object> runtimeData,
        @Nullable Map<String, Object> nodeData,
        @Nullable Map<String, Object> processData,
        @Nullable String taskStatusOverride,
        @Nullable Boolean clearTaskStatusOverride,
        @Nullable String outputKey,
        @Nullable Long previousTaskId,
        @Nullable String userId,
        @Nonnull Map<String, IdentityData> additionalIdentities,
        @Nonnull Map<String, Object> messageDetails
) {
    @Nonnull
    public static CommunicationContinuation from(@Nonnull ProcessNodeExecutionResult result,
                                                  @Nullable Long previousTaskId,
                                                  @Nullable String userId,
                                                  @Nonnull Map<String, IdentityData> additionalIdentities,
                                                  @Nonnull Map<String, Object> messageDetails) {
        var kind = switch (result) {
            case ProcessNodeExecutionResultTaskCompleted ignored -> "completed";
            case ProcessNodeExecutionResultTaskAssignedCustomer ignored -> "customer";
            case ProcessNodeExecutionResultPaymentRequested ignored -> "payment";
            default -> throw new IllegalArgumentException("Dieses Prozessergebnis unterstützt keinen asynchronen Nachrichtenversand.");
        };
        return new CommunicationContinuation(1, kind,
                result instanceof ProcessNodeExecutionResultTaskCompleted r ? r.getViaPort() : null,
                result instanceof ProcessNodeExecutionResultTaskAssignedCustomer r ? r.getIdentityId() : null,
                result instanceof ProcessNodeExecutionResultPaymentRequested r ? r.getTransactionKey() : null,
                result instanceof ProcessNodeExecutionResultPaymentRequested r ? r.getPaymentProviderName() : null,
                result.getRuntimeData(), result.getNodeData(), result.getProcessData(),
                result.getTaskStatusOverride(), result.getClearTaskStatusOverride(),
                result.getCommunicationRequest().nodeDataOutputKey(), previousTaskId, userId,
                additionalIdentities, messageDetails);
    }

    @Nonnull
    public ProcessNodeExecutionResult restore(@Nonnull Map<String, Object> receipt) {
        if (version != 1) throw new IllegalStateException("Unbekannte Version der Versandfortsetzung.");
        ProcessNodeExecutionResult result = switch (kind) {
            case "completed" -> new ProcessNodeExecutionResultTaskCompleted(viaPort);
            case "customer" -> new ProcessNodeExecutionResultTaskAssignedCustomer(identityId);
            case "payment" -> new ProcessNodeExecutionResultPaymentRequested(transactionKey, paymentProviderName);
            default -> throw new IllegalStateException("Unbekannte Versandfortsetzung.");
        };
        var output = nodeData == null ? new java.util.LinkedHashMap<String, Object>() : new java.util.LinkedHashMap<>(nodeData);
        if (outputKey != null) output.put(outputKey, receipt);
        return result.setRuntimeData(runtimeData).setNodeData(output).setProcessData(processData)
                .setTaskStatusOverride(taskStatusOverride).setClearTaskStatusOverride(clearTaskStatusOverride);
    }
}
