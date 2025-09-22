package de.aivot.GoverBackend.form.dtos;

import de.aivot.GoverBackend.elements.models.elements.RootElement;
import de.aivot.GoverBackend.form.entities.FormVersionWithDetailsEntity;
import de.aivot.GoverBackend.form.entities.FormVersionWithMembershipEntity;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.enums.FormType;
import de.aivot.GoverBackend.identity.models.IdentityProviderLink;
import de.aivot.GoverBackend.models.payment.PaymentProduct;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record FormDetailsResponseDTO(
        Integer id,
        String slug,
        String internalTitle,
        String publicTitle,
        Integer developingDepartmentId,
        Integer managingDepartmentId,
        Integer responsibleDepartmentId,
        Integer publishedVersion,
        Integer draftedVersion,
        Integer versionCount,
        Integer formId,
        Integer version,
        FormStatus status,
        FormType type,
        Integer legalSupportDepartmentId,
        Integer technicalSupportDepartmentId,
        Integer imprintDepartmentId,
        Integer privacyDepartmentId,
        Integer accessibilityDepartmentId,
        Integer destinationId,
        Integer themeId,
        UUID pdfTemplateKey,
        UUID paymentProviderKey,
        String paymentPurpose,
        String paymentDescription,
        List<PaymentProduct> paymentProducts,
        List<IdentityProviderLink> identityProviders,
        Boolean identityVerificationRequired,
        Integer customerAccessHours,
        Integer submissionRetentionWeeks,
        RootElement rootElement,
        LocalDateTime created,
        LocalDateTime updated,
        LocalDateTime published,
        LocalDateTime revoked
) {
    public static FormDetailsResponseDTO fromEntity(FormVersionWithDetailsEntity form) {
        return new FormDetailsResponseDTO(
                form.getId(),
                form.getSlug(),
                form.getInternalTitle(),
                form.getPublicTitle(),
                form.getDevelopingDepartmentId(),
                form.getManagingDepartmentId(),
                form.getResponsibleDepartmentId(),
                form.getPublishedVersion(),
                form.getDraftedVersion(),
                form.getVersionCount(),
                form.getFormId(),
                form.getVersion(),
                form.getStatus(),
                form.getType(),
                form.getLegalSupportDepartmentId(),
                form.getTechnicalSupportDepartmentId(),
                form.getImprintDepartmentId(),
                form.getPrivacyDepartmentId(),
                form.getAccessibilityDepartmentId(),
                form.getDestinationId(),
                form.getThemeId(),
                form.getPdfTemplateKey(),
                form.getPaymentProviderKey(),
                form.getPaymentPurpose(),
                form.getPaymentDescription(),
                form.getPaymentProducts(),
                form.getIdentityProviders(),
                form.getIdentityVerificationRequired(),
                form.getCustomerAccessHours(),
                form.getSubmissionRetentionWeeks(),
                form.getRootElement(),
                form.getCreated(),
                form.getUpdated(),
                form.getPublished(),
                form.getRevoked()
        );
    }

    public static FormDetailsResponseDTO fromEntity(FormVersionWithMembershipEntity form) {
        return new FormDetailsResponseDTO(
                form.getId(),
                form.getSlug(),
                form.getInternalTitle(),
                form.getPublicTitle(),
                form.getDevelopingDepartmentId(),
                form.getManagingDepartmentId(),
                form.getResponsibleDepartmentId(),
                form.getPublishedVersion(),
                form.getDraftedVersion(),
                form.getVersionCount(),
                form.getFormId(),
                form.getVersion(),
                form.getStatus(),
                form.getType(),
                form.getLegalSupportDepartmentId(),
                form.getTechnicalSupportDepartmentId(),
                form.getImprintDepartmentId(),
                form.getPrivacyDepartmentId(),
                form.getAccessibilityDepartmentId(),
                form.getDestinationId(),
                form.getThemeId(),
                form.getPdfTemplateKey(),
                form.getPaymentProviderKey(),
                form.getPaymentPurpose(),
                form.getPaymentDescription(),
                form.getPaymentProducts(),
                form.getIdentityProviders(),
                form.getIdentityVerificationRequired(),
                form.getCustomerAccessHours(),
                form.getSubmissionRetentionWeeks(),
                form.getRootElement(),
                form.getCreated(),
                form.getUpdated(),
                form.getPublished(),
                form.getRevoked()
        );
    }
}
