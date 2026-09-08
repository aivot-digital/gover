package de.aivot.prosuna.backend.identity.dtos;

import de.aivot.prosuna.backend.identity.enums.IdentityProviderType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Public information about an identity provider configured for an identity slot.
 */
public record IdentityProviderOptionResponseDTO(
        @Nonnull
        UUID identityProviderKey,
        @Nonnull
        String identityProviderName,
        @Nullable
        UUID identityProviderAssetKey,
        @Nonnull
        IdentityProviderType identityProviderType,
        @Nonnull
        Boolean isAuthenticatedWithThis,
        @Nonnull
        List<String> additionalScopes
) {
}
