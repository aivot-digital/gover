package de.aivot.prosuna.backend.communication.models;

import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import jakarta.annotation.Nonnull;
import java.util.UUID;

/** Extends the existing test layout without changing synchronous providers' responses. */
public class CommunicationTestResult extends GroupLayoutElement {
    private final UUID deliveryId;

    public CommunicationTestResult(@Nonnull UUID deliveryId) {
        this.deliveryId = deliveryId;
    }

    @Nonnull
    public UUID getDeliveryId() { return deliveryId; }
}
