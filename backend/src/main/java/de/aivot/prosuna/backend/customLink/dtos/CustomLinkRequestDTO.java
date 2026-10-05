package de.aivot.prosuna.backend.customLink.dtos;

import de.aivot.prosuna.backend.customLink.entities.CustomLink;
import de.aivot.prosuna.backend.customLink.enums.CustomLinkType;
import de.aivot.prosuna.backend.lib.RequestDTO;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record CustomLinkRequestDTO(
        @NotNull @NotBlank @Size(max = 128) String label,
        @Nullable @Size(max = 255) String description,
        @NotNull @NotBlank @Size(max = 500) @URL @Pattern(regexp = "(?i)^https?://.+$") String url,
        @Nullable @Size(max = 64) @Pattern(regexp = "^[a-zA-Z0-9_-]*$") String icon,
        @NotNull CustomLinkType type,
        @NotNull Boolean enabled
) implements RequestDTO<CustomLink> {
    @Override
    public CustomLink toEntity() {
        return new CustomLink()
                .setLabel(label.trim())
                .setDescription(description == null || description.isBlank() ? null : description.trim())
                .setUrl(url.trim())
                .setIcon(icon == null || icon.isBlank() ? null : icon)
                .setType(type)
                .setEnabled(enabled);
    }
}
