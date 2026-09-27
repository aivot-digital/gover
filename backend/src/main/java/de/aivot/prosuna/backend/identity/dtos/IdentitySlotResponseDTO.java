package de.aivot.prosuna.backend.identity.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    /**
     * Whether this slot contains an identity that is still valid for its current configuration.
     * Communication provider selection and customer configuration are deliberately excluded.
     */
    @JsonIgnore
    public boolean hasValidIdentity() {
        if (identityType == IdentityType.Email) {
            return Boolean.TRUE.equals(allowsEmail) && emailAddress != null;
        }
        if (identityType == IdentityType.IdentityProvider) {
            return availableIdentityProviders
                    .stream()
                    .anyMatch(provider -> Boolean.TRUE.equals(provider.isAuthenticatedWithThis()));
        }
        return false;
    }
}
