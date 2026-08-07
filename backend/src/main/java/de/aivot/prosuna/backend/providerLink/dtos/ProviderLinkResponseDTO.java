package de.aivot.prosuna.backend.providerLink.dtos;

import de.aivot.prosuna.backend.providerLink.entities.ProviderLink;

import jakarta.annotation.Nonnull;
import java.time.Instant;

public record ProviderLinkResponseDTO(
        @Nonnull
        Integer id,
        @Nonnull
        String text,
        @Nonnull
        String link,
        @Nonnull
        Instant created,
        @Nonnull
        Instant updated
) {
    public static ProviderLinkResponseDTO fromEntity(ProviderLink entity) {
        return new ProviderLinkResponseDTO(
                entity.getId(),
                entity.getText(),
                entity.getLink(),
                entity.getCreated(),
                entity.getUpdated()
        );
    }
}
