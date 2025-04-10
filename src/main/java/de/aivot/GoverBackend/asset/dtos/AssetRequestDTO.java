package de.aivot.GoverBackend.asset.dtos;

import de.aivot.GoverBackend.asset.entities.AssetEntity;
import de.aivot.GoverBackend.lib.ReqeustDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import javax.annotation.Nonnull;

public record AssetRequestDTO(
        @Nonnull
        @NotBlank(message = "filename is required")
        @Length(min = 3, max = 255, message = "filename must be between 3 and 255 characters")
        String filename,

        @Nonnull
        @NotNull(message = "Is private is required")
        Boolean isPrivate
) implements ReqeustDTO<AssetEntity> {

    @Override
    public AssetEntity toEntity() {
        var asset = new AssetEntity();
        asset.setFilename(filename());
        asset.setPrivate(isPrivate());
        return asset;
    }
}
