package de.aivot.prosuna.backend.communication.models;

import de.aivot.prosuna.backend.communication.entities.CommunicationDeliveryEntity;
import de.aivot.prosuna.backend.communication.services.CommunicationDeliveryMonitor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Safe staff projection: never exposes configuration, secrets, or the stored process continuation. */
public record CommunicationDeliveryView(
        @Nonnull UUID id,
        @Nonnull CommunicationDeliveryStatus status,
        @Nonnull String label,
        @Nullable String message,
        @Nonnull Map<String, Object> receipt,
        @Nonnull Instant updated,
        boolean overdue,
        boolean checking
) {
    @Nonnull
    public static CommunicationDeliveryView from(@Nonnull CommunicationDeliveryEntity delivery) {
        return new CommunicationDeliveryView(delivery.getId(), delivery.getStatus(),
                CommunicationDeliveryMonitor.label(delivery.getStatus()), delivery.getStatusMessage(),
                publicReceipt(delivery.getReceipt()), delivery.getUpdated(), delivery.getOverdueNotified(), delivery.getNextCheckAt() != null);
    }
    @Nonnull
    public static Map<String, Object> publicReceipt(@Nonnull Map<String, Object> receipt) {
        var result = new java.util.LinkedHashMap<String, Object>();
        for (var key : java.util.List.of("postfachId", "submissionId", "caseId", "destinationId", "senderDestinationId", "status", "eventId", "eventTime", "problems")) {
            if (receipt.containsKey(key)) result.put(key, receipt.get(key));
        }
        return result;
    }

}
