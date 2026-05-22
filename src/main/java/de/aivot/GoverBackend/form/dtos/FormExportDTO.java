package de.aivot.GoverBackend.form.dtos;

import de.aivot.GoverBackend.form.entities.FormEntity;
import de.aivot.GoverBackend.form.entities.FormVersionEntity;
import jakarta.annotation.Nonnull;

import java.time.Instant;

public record FormExportDTO(
        @Nonnull
        FormEntity form,
        @Nonnull
        FormVersionEntity version,
        @Nonnull
        Build build,
        @Nonnull
        Instant timestamp
) {
    public record Build(
            @Nonnull
            String version,
            @Nonnull
            String number,
            @Nonnull
            String timestamp
    ) {
    }
}
