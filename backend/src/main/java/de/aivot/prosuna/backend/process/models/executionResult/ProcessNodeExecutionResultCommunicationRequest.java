package de.aivot.prosuna.backend.process.models.executionResult;

import de.aivot.prosuna.backend.communication.models.CommunicationMessage;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

/**
 * Describes one message that must be sent as part of applying a process-node execution result.
 *
 * @param recipientIdentityId logical process identity receiving the message
 * @param message message to dispatch through the communication provider selected for the identity
 * @param nodeDataOutputKey optional node-data key receiving the communication provider result
 */
public record ProcessNodeExecutionResultCommunicationRequest(
        @Nonnull String recipientIdentityId,
        @Nonnull CommunicationMessage message,
        @Nullable String nodeDataOutputKey
) {
    public ProcessNodeExecutionResultCommunicationRequest {
        recipientIdentityId = Objects.requireNonNull(recipientIdentityId, "recipientIdentityId").trim();
        message = Objects.requireNonNull(message, "message");
        if (recipientIdentityId.isEmpty()) {
            throw new IllegalArgumentException("Die Empfängeridentität darf nicht leer sein.");
        }
        if (nodeDataOutputKey != null) {
            nodeDataOutputKey = nodeDataOutputKey.trim();
            if (nodeDataOutputKey.isEmpty()) {
                throw new IllegalArgumentException("Der Node-Data-Ausgabeschlüssel darf nicht leer sein.");
            }
        }
    }
}
