package de.aivot.prosuna.backend.process.models.executionResult;

import de.aivot.prosuna.backend.communication.models.CommunicationMessage;
import de.aivot.prosuna.backend.communication.utils.EmailAddressUtils;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Describes one message that must be sent as part of applying a process-node execution result.
 *
 * @param recipientIdentityId    logical process identity receiving the message, if one already exists
 * @param recipientEmailAddress  direct email recipient when no process identity exists yet
 * @param message                message to dispatch
 * @param nodeDataOutputKey      optional node-data key receiving the communication provider result
 */
public record ProcessNodeExecutionResultCommunicationRequest(
        @Nullable String recipientIdentityId,
        @Nullable String recipientEmailAddress,
        @Nonnull CommunicationMessage message,
        @Nullable String nodeDataOutputKey
) {
    private static final String DEFAULT_NODE_DATA_OUTPUT_KEY = "communicationResult";

    public ProcessNodeExecutionResultCommunicationRequest(@Nonnull String recipientIdentityId,
                                                          @Nonnull CommunicationMessage message) {
        this(recipientIdentityId, null, message, null);
    }

    public ProcessNodeExecutionResultCommunicationRequest(@Nonnull String recipientIdentityId,
                                                          @Nonnull CommunicationMessage message,
                                                          @Nullable String nodeDataOutputKey) {
        this(recipientIdentityId, null, message, nodeDataOutputKey);
    }

    public static ProcessNodeExecutionResultCommunicationRequest toEmail(@Nonnull String recipientEmailAddress,
                                                                         @Nonnull CommunicationMessage message) {
        return new ProcessNodeExecutionResultCommunicationRequest(null, recipientEmailAddress, message, null);
    }

    public ProcessNodeExecutionResultCommunicationRequest {
        if ((StringUtils.isNullOrEmpty(recipientIdentityId) && recipientEmailAddress == null)
                || (recipientIdentityId != null && recipientEmailAddress != null)) {
            throw new IllegalArgumentException("Es muss genau eine Empfängeridentität oder E-Mail-Adresse angegeben werden.");
        }

        if (message == null) {
            throw new IllegalArgumentException("Die zu versendende Nachricht muss angegeben werden.");
        }

        nodeDataOutputKey = nodeDataOutputKey == null ? DEFAULT_NODE_DATA_OUTPUT_KEY : nodeDataOutputKey;
        if (StringUtils.isNullOrEmpty(nodeDataOutputKey)) {
            throw new IllegalArgumentException("Der Node-Data-Ausgabeschlüssel für das Kommunikationsergebnis darf nicht leer sein.");
        }

        if (recipientIdentityId != null) {
            recipientIdentityId = recipientIdentityId.trim();
        } else {
            recipientEmailAddress = EmailAddressUtils.normalizeSingleAddress(recipientEmailAddress);
        }
        nodeDataOutputKey = nodeDataOutputKey.trim();
    }
}
