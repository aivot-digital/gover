package de.aivot.GoverBackend.providerLink.dtos;

import de.aivot.GoverBackend.providerLink.entities.ProviderLink;

import javax.annotation.Nonnull;
import java.time.LocalDateTime;

public record ProviderLinkResponseDTO(
        @Nonnull
        Integer id,
        @Nonnull
        String text,
        @Nonnull
        String link,
        @Nonnull
        LocalDateTime created,
        @Nonnull
        LocalDateTime updated
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
