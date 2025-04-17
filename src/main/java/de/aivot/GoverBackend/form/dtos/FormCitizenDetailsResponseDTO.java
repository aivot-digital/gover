package de.aivot.GoverBackend.form.dtos;

import de.aivot.GoverBackend.elements.models.RootElement;
import de.aivot.GoverBackend.elements.utils.ElementStreamUtils;
import de.aivot.GoverBackend.form.entities.Form;
import de.aivot.GoverBackend.identity.models.IdentityProviderLink;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

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
        Boolean identityRequired,
        @Nonnull
        List<IdentityProviderLink> identityProviders

) {
    public static FormCitizenDetailsResponseDTO fromEntity(Form form, boolean obfuscateSteps) {
        ElementStreamUtils
                .applyAction(form.getRoot(), element -> {
                    element.setTestProtocolSet(null);
                });

        if (obfuscateSteps) {
            for (var step : form.getRoot().getChildren()) {
                step.setChildren(List.of());
            }
        }

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
                form.getIdentityRequired(),
                form.getIdentityProviders()
        );
    }
}
