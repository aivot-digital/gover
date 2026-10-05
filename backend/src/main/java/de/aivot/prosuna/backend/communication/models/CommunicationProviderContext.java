package de.aivot.prosuna.backend.communication.models;

import de.aivot.prosuna.backend.communication.entities.CommunicationProviderBindingEntity;
import de.aivot.prosuna.backend.communication.entities.CommunicationProviderEntity;
import de.aivot.prosuna.backend.identity.entities.IdentityProviderEntity;
import jakarta.annotation.Nonnull;

/** Fully resolved, typed runtime context for one communication-provider binding. */
public record CommunicationProviderContext<C, I>(
        @Nonnull CommunicationProviderEntity communicationProvider,
        @Nonnull IdentityProviderEntity identityProvider,
        @Nonnull CommunicationProviderBindingEntity binding,
        @Nonnull C communicationProviderConfiguration,
        @Nonnull I identityProviderBindingConfiguration
) {
}
