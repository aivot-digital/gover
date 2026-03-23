package de.aivot.GoverBackend.form.dtos;

import de.aivot.GoverBackend.form.entities.VFormVersionWithDetailsEntity;

import jakarta.annotation.Nonnull;
import java.time.LocalDateTime;

public record FormCitizenListResponseDTO(
        @Nonnull
        String slug,
        @Nonnull
        Integer version,
        @Nonnull
        String title,
        @Nonnull
        LocalDateTime updated
) {
    public static FormCitizenListResponseDTO fromEntity(VFormVersionWithDetailsEntity form) {
        return new FormCitizenListResponseDTO(
                form.getSlug(),
                form.getPublishedVersion(),
                form.getPublicTitle(),
                form.getUpdated()
        );
    }
}
