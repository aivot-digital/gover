package de.aivot.gover.backend.search.dtos;

import de.aivot.gover.backend.search.entities.SearchItemEntity;
import jakarta.annotation.Nonnull;

public record SearchItemResponseDTO(
        @Nonnull
        String id,
        @Nonnull
        String label,
        @Nonnull
        String originTable
) {
    public static SearchItemResponseDTO fromEntity(@Nonnull SearchItemEntity entity) {
        return new SearchItemResponseDTO(
                entity.getId(),
                entity.getLabel(),
                entity.getOriginTable()
        );
    }
}
