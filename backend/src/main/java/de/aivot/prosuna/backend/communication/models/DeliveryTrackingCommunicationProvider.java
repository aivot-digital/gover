package de.aivot.prosuna.backend.communication.models;

import de.aivot.prosuna.backend.communication.exceptions.CommunicationException;
import jakarta.annotation.Nonnull;
import java.util.Map;

/** Optional extension: existing synchronous providers retain their send contract. */
public interface DeliveryTrackingCommunicationProvider<C, I> extends CommunicationProviderDefinition<C, I> {
    @Nonnull
    CommunicationSendResult checkDelivery(@Nonnull C configuration,
                                           @Nonnull Map<String, Object> receipt) throws CommunicationException;
}
