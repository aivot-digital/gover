package de.aivot.GoverBackend.form.dtos;

import de.aivot.GoverBackend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.steps.GenericStepElement;
import de.aivot.GoverBackend.elements.utils.ElementStreamUtils;
import de.aivot.GoverBackend.form.entities.VFormVersionWithDetailsEntity;
import de.aivot.GoverBackend.identity.models.IdentityProviderLink;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;

public record FormCitizenDetailsResponseDTO(
        @Nonnull
        Integer id,
        @Nonnull
        String slug,
        @Nonnull
        Integer version,
        @Nonnull
        String title,
        @Nonnull
        FormLayoutElement root,
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
    public static FormCitizenDetailsResponseDTO fromEntity(VFormVersionWithDetailsEntity form, boolean obfuscateSteps) {
        ElementStreamUtils
                .applyAction(form.getRootElement(), element -> {
                    element.setName("");
                    element.setTestProtocolSet(null);
                });

        if (obfuscateSteps) {
            for (var step : form.getRootElement().getChildren()) {
                if (step instanceof GenericStepElement s) {
                    s.setChildren(List.of());
                }
            }
        }

        return new FormCitizenDetailsResponseDTO(
                form.getId(),
                form.getSlug(),
                form.getVersion(),
                form.getPublicTitle(),
                form.getRootElement(),
                form.getLegalSupportDepartmentId(),
                form.getTechnicalSupportDepartmentId(),
                form.getImprintDepartmentId(),
                form.getPrivacyDepartmentId(),
                form.getAccessibilityDepartmentId(),
                form.getDevelopingDepartmentId(),
                form.getManagingDepartmentId(),
                form.getResponsibleDepartmentId(),
                form.getThemeId(),
                form.getIdentityVerificationRequired(),
                form.getIdentityProviders()
        );
    }
}
