package de.aivot.prosuna.backend.communication.models;

import jakarta.annotation.Nonnull;
import java.util.Map;

/** Transport receipt; acceptance is distinct from handing a message to the transport. */
public record CommunicationSendResult(
        @Nonnull CommunicationDeliveryStatus status,
        @Nonnull Map<String, Object> details
) {
}
