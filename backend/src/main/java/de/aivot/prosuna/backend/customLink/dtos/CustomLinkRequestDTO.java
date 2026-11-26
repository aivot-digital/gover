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
        @NotNull(message = "Geben Sie eine Bezeichnung für den Custom-Link ein.") @NotBlank(message = "Geben Sie eine Bezeichnung für den Custom-Link ein.") @Size(max = 128, message = "Die Bezeichnung des Custom-Links darf höchstens 128 Zeichen lang sein.") String label,
        @Nullable @Size(max = 255, message = "Die Beschreibung des Custom-Links darf höchstens 255 Zeichen lang sein.") String description,
        @NotNull(message = "Geben Sie eine Zieladresse für den Custom-Link ein.") @NotBlank(message = "Geben Sie eine Zieladresse für den Custom-Link ein.") @Size(max = 500, message = "Die Zieladresse des Custom-Links darf höchstens 500 Zeichen lang sein.") @URL(message = "Geben Sie eine gültige Zieladresse mit http:// oder https:// ein.") @Pattern(regexp = "(?i)^https?://.+$", message = "Die Zieladresse des Custom-Links muss mit http:// oder https:// beginnen.") String url,
        @Nullable @Size(max = 64, message = "Der Symbolname darf höchstens 64 Zeichen lang sein.") @Pattern(regexp = "^[a-zA-Z0-9_-]*$", message = "Der Symbolname darf nur Buchstaben von A bis Z, Ziffern, Unterstriche und Bindestriche enthalten.") String icon,
        @NotNull(message = "Wählen Sie einen Typ für den Custom-Link aus.") CustomLinkType type,
        @NotNull(message = "Geben Sie an, ob der Custom-Link aktiviert ist.") Boolean enabled,
        @Nullable Long lockVersion
) implements RequestDTO<CustomLink> {
    @Override
    public CustomLink toEntity() {
        return new CustomLink()
                .setLabel(label.trim())
                .setDescription(description == null || description.isBlank() ? null : description.trim())
                .setUrl(url.trim())
                .setIcon(icon == null || icon.isBlank() ? null : icon)
                .setType(type)
                .setEnabled(enabled)
                .setLockVersion(lockVersion);
    }
}
