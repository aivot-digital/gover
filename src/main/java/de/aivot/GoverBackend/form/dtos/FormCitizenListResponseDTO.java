package de.aivot.GoverBackend.form.dtos;

import de.aivot.GoverBackend.form.entities.FormVersionWithDetailsEntity;

import javax.annotation.Nonnull;
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
    public static FormCitizenListResponseDTO fromEntity(FormVersionWithDetailsEntity form) {
        return new FormCitizenListResponseDTO(
                form.getSlug(),
                form.getVersion(),
                form.getPublicTitle(),
                form.getUpdated()
        );
    }
}
