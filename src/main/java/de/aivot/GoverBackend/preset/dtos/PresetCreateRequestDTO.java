package de.aivot.GoverBackend.preset.dtos;

import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.lib.RequestDTO;
import de.aivot.GoverBackend.preset.entities.PresetEntity;
import de.aivot.GoverBackend.preset.entities.PresetVersionEntity;
import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record PresetCreateRequestDTO(
        @Nonnull
        @NotNull(message = "Der Titel darf nicht null sein.")
        @NotBlank(message = "Der Titel darf nicht leer sein.")
        @Size(min = 3, max = 255, message = "Der Titel muss mindestens 3 Zeichen lang sein, maximal aber 255 Zeichen lang sein.")
        String title,

        @Nonnull
        @NotNull(message = "Das Root-Element darf nicht null sein.")
        GroupLayoutElement rootElement
) implements RequestDTO<PresetEntity> {

    @Override
    public PresetEntity toEntity() {
        return new PresetEntity(
                UUID.randomUUID(),
                title,
                null,
                null,
                0,
                null,
                null
        );
    }

    public PresetVersionEntity toVersionEntity(PresetEntity parent) {
        return new PresetVersionEntity(
                parent.getKey(),
                1,
                rootElement,
                FormStatus.Drafted,
                null,
                null,
                null,
                null
        );
    }
}
