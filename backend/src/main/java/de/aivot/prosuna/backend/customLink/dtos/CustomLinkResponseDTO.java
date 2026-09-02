package de.aivot.prosuna.backend.customLink.dtos;

import de.aivot.prosuna.backend.customLink.entities.CustomLink;
import de.aivot.prosuna.backend.customLink.enums.CustomLinkType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.Instant;

public record CustomLinkResponseDTO(
        @Nonnull Integer id,
        @Nonnull String label,
        @Nullable String description,
        @Nonnull String url,
        @Nullable String icon,
        @Nonnull CustomLinkType type,
        int position,
        boolean enabled,
        @Nonnull Instant created,
        @Nonnull Instant updated
) {
    public static CustomLinkResponseDTO fromEntity(CustomLink entity) {
        return new CustomLinkResponseDTO(
                entity.getId(),
                entity.getLabel(),
                entity.getDescription(),
                entity.getUrl(),
                entity.getIcon(),
                entity.getType(),
                entity.getPosition(),
                entity.getEnabled(),
                entity.getCreated(),
                entity.getUpdated()
        );
    }
}
