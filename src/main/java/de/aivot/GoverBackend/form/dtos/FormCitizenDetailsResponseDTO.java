package de.aivot.GoverBackend.form.dtos;

import de.aivot.GoverBackend.elements.utils.ElementStreamUtils;
import de.aivot.GoverBackend.enums.BayernIdAccessLevel;
import de.aivot.GoverBackend.enums.BundIdAccessLevel;
import de.aivot.GoverBackend.enums.MukAccessLevel;
import de.aivot.GoverBackend.enums.SchleswigHolsteinIdAccessLevel;
import de.aivot.GoverBackend.form.entities.Form;
import de.aivot.GoverBackend.elements.models.RootElement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public record FormCitizenDetailsResponseDTO(
        @Nonnull
        Integer id,
        @Nonnull
        String slug,
        @Nonnull
        String version,
        @Nonnull
        String title,
        @Nonnull
        RootElement root,
        @Nullable
        Integer legalSupportDepartmentId,
        @Nullable
        Integer technicalSupportDepartmentId,
        @Nullable
        Integer imprintDepartmentId,
        @Nullable
        Integer privacyDepartmentId,
        @Nullable
        Integer accessibilityDepartmentId,
        @Nonnull
        Integer developingDepartmentId,
        @Nullable
        Integer managingDepartmentId,
        @Nullable
        Integer responsibleDepartmentId,
        @Nonnull
        Integer themeId,
        @Nonnull
        Boolean bundIdEnabled,
        @Nullable
        BundIdAccessLevel bundIdLevel,
        @Nonnull
        Boolean bayernIdEnabled,
        @Nullable
        BayernIdAccessLevel bayernIdLevel,
        @Nonnull
        Boolean mukEnabled,
        @Nullable
        MukAccessLevel mukLevel,
        @Nonnull
        Boolean shIdEnabled,
        @Nullable
        SchleswigHolsteinIdAccessLevel shIdLevel

) {
    public static FormCitizenDetailsResponseDTO fromEntity(Form form) {
        ElementStreamUtils
                .applyAction(form.getRoot(), element -> {
                        element.setTestProtocolSet(null);
                });

        return new FormCitizenDetailsResponseDTO(
                form.getId(),
                form.getSlug(),
                form.getVersion(),
                form.getTitle(),
                form.getRoot(),
                form.getLegalSupportDepartmentId(),
                form.getTechnicalSupportDepartmentId(),
                form.getImprintDepartmentId(),
                form.getPrivacyDepartmentId(),
                form.getAccessibilityDepartmentId(),
                form.getDevelopingDepartmentId(),
                form.getManagingDepartmentId(),
                form.getResponsibleDepartmentId(),
                form.getThemeId(),
                form.getBundIdEnabled(),
                form.getBundIdLevel(),
                form.getBayernIdEnabled(),
                form.getBayernIdLevel(),
                form.getMukEnabled(),
                form.getMukLevel(),
                form.getShIdEnabled(),
                form.getShIdLevel()
        );
    }
}
