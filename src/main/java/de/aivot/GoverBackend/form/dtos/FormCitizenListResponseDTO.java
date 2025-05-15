package de.aivot.GoverBackend.form.dtos;

import de.aivot.GoverBackend.form.entities.Form;

import javax.annotation.Nonnull;
import java.time.LocalDateTime;

public record FormCitizenListResponseDTO(
        @Nonnull
        String slug,
        @Nonnull
        String version,
        @Nonnull
        String title,
        @Nonnull
        LocalDateTime updated
) {
    public static FormCitizenListResponseDTO fromEntity(Form form) {
        return new FormCitizenListResponseDTO(
                form.getSlug(),
                form.getVersion(),
                form.getFormTitle(),
                form.getUpdated()
        );
    }
}
