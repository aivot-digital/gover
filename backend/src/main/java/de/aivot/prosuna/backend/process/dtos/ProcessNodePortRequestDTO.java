package de.aivot.prosuna.backend.process.dtos;

import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import jakarta.annotation.Nonnull;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** A prospective node; initial/import configuration is prepared without persisting it. */
public record ProcessNodePortRequestDTO(
        @Nonnull @NotNull @Valid ProcessNodeEntity node,
        @Nonnull Boolean forImport,
        @Nonnull Boolean useInitialConfiguration
) {
    public ProcessNodePortRequestDTO {
        forImport = Boolean.TRUE.equals(forImport);
        useInitialConfiguration = Boolean.TRUE.equals(useInitialConfiguration);
    }
}
