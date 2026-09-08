package de.aivot.prosuna.backend.identity.dtos;

import de.aivot.prosuna.backend.communication.services.IdentityCommunicationService;
import de.aivot.prosuna.backend.identity.enums.IdentityType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Public state of a configured identity slot.
 */
public record IdentitySlotResponseDTO(
        @Nonnull
        String id,
        @Nullable
        String title,
        @Nullable
        String description,
        @Nonnull
        Boolean isOptional,
        @Nonnull
        Boolean allowsEmail,
        @Nullable
        IdentityType identityType,
        @Nullable
        String emailAddress,
        boolean isReady,
        @Nonnull
        List<IdentityProviderOptionResponseDTO> availableIdentityProviders,
        @Nullable
        IdentityCommunicationService.SelectionState communication
) {
    public Boolean getIsRequired() {
        return !isOptional;
    }
}
