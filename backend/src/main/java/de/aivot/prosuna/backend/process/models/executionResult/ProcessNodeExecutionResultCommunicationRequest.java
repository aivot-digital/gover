package de.aivot.prosuna.backend.process.models.executionResult;

import de.aivot.prosuna.backend.communication.models.CommunicationMessage;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Describes one message that must be sent as part of applying a process-node execution result.
 *
 * @param recipientIdentityId logical process identity receiving the message
 * @param message             message to dispatch through the communication provider selected for the identity
 * @param nodeDataOutputKey   optional node-data key receiving the communication provider result
 */
public record ProcessNodeExecutionResultCommunicationRequest(
        @Nonnull String recipientIdentityId,
        @Nonnull CommunicationMessage message,
        @Nullable String nodeDataOutputKey
) {
    private static final String DEFAULT_NODE_DATA_OUTPUT_KEY = "communicationResult";

    public ProcessNodeExecutionResultCommunicationRequest(@Nonnull String recipientIdentityId,
                                                          @Nonnull CommunicationMessage message) {
        this(recipientIdentityId, message, null);
    }

    public ProcessNodeExecutionResultCommunicationRequest {
        if (StringUtils.isNullOrEmpty(recipientIdentityId)) {
            throw new IllegalArgumentException("Die ID der Empfängeridentität muss angegeben werden und darf nicht leer sein.");
        }

        if (message == null) {
            throw new IllegalArgumentException("Die zu versendende Nachricht muss angegeben werden.");
        }

        nodeDataOutputKey = nodeDataOutputKey == null ? DEFAULT_NODE_DATA_OUTPUT_KEY : nodeDataOutputKey;
        if (StringUtils.isNullOrEmpty(nodeDataOutputKey)) {
            throw new IllegalArgumentException("Der Node-Data-Ausgabeschlüssel für das Kommunikationsergebnis darf nicht leer sein.");
        }

        recipientIdentityId = recipientIdentityId.trim();
        nodeDataOutputKey = nodeDataOutputKey.trim();
    }
}
