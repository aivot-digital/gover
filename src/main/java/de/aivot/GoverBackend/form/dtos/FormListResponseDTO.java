package de.aivot.GoverBackend.form.dtos;

import de.aivot.GoverBackend.form.entities.Form;
import de.aivot.GoverBackend.form.entities.FormVersionWithMembershipEntity;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.enums.FormType;
import de.aivot.GoverBackend.identity.models.IdentityProviderLink;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record FormListResponseDTO(
        Integer id,
        String slug,
        Integer version,
        String title,
        FormStatus status,
        FormType type,
        Integer developingDepartmentId,
        Integer managingDepartmentId,
        Integer responsibleDepartmentId,
        Integer themeId,
        LocalDateTime created,
        LocalDateTime updated,
        UUID paymentProvider,
        Boolean identityRequired,
        List<IdentityProviderLink> identityProviders
) {
    public static FormListResponseDTO fromEntity(Form form) {
        return new FormListResponseDTO(
                form.getId(),
                form.getSlug(),
                1, // TODO
                form.getTitle(),
                form.getStatus(),
                form.getType(),
                form.getDevelopingDepartmentId(),
                form.getManagingDepartmentId(),
                form.getResponsibleDepartmentId(),
                form.getThemeId(),
                form.getCreated(),
                form.getUpdated(),
                form.getPaymentProvider(),
                form.getIdentityRequired(),
                form.getIdentityProviders()
        );
    }

    public static FormListResponseDTO fromEntity(FormVersionWithMembershipEntity form) {
        return new FormListResponseDTO(
                form.getId(),
                form.getSlug(),
                form.getPublishedVersion(),
                form.getTitle(),
                FormStatus.Published, // TODO
                FormType.Public, // TODO
                form.getDevelopingDepartmentId(),
                form.getManagingDepartmentId(),
                form.getResponsibleDepartmentId(),
                1,
                form.getCreated(),
                form.getUpdated(),
                UUID.randomUUID(),
                false,
                null
        );
    }
}
