package de.aivot.GoverBackend.form.dtos;

import de.aivot.GoverBackend.elements.models.elements.RootElement;
import de.aivot.GoverBackend.form.entities.FormVersionWithDetailsEntity;
import de.aivot.GoverBackend.form.entities.FormVersionWithMembershipEntity;
import de.aivot.GoverBackend.form.enums.FormType;
import de.aivot.GoverBackend.identity.models.IdentityProviderLink;
import de.aivot.GoverBackend.models.payment.PaymentProduct;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public record FormDetailsResponseDTO(
        Integer id,
        String slug,
        Integer version,
        String title,
        FormType type,
        RootElement root,
        Integer destinationId,
        Integer legalSupportDepartmentId,
        Integer technicalSupportDepartmentId,
        Integer imprintDepartmentId,
        Integer privacyDepartmentId,
        Integer accessibilityDepartmentId,
        Integer developingDepartmentId,
        Integer managingDepartmentId,
        Integer responsibleDepartmentId,
        Integer themeId,
        LocalDateTime created,
        LocalDateTime updated,
        Integer customerAccessHours,
        Integer submissionDeletionWeeks,
        UUID pdfBodyTemplateKey,
        Collection<PaymentProduct> products,
        String paymentPurpose,
        String paymentDescription,
        UUID paymentProvider,
        Boolean identityRequired,
        List<IdentityProviderLink> identityProviders
) {
    public static FormDetailsResponseDTO fromEntity(FormVersionWithDetailsEntity form) {
        return new FormDetailsResponseDTO(
                form.getId(),
                form.getSlug(),
                form.getVersion(),
                form.getInternalTitle(),
                form.getType(),
                form.getRootElement(),
                form.getDestinationId(),
                form.getLegalSupportDepartmentId(),
                form.getTechnicalSupportDepartmentId(),
                form.getImprintDepartmentId(),
                form.getPrivacyDepartmentId(),
                form.getAccessibilityDepartmentId(),
                form.getDevelopingDepartmentId(),
                form.getManagingDepartmentId(),
                form.getResponsibleDepartmentId(),
                form.getThemeId(),
                form.getCreated(),
                form.getUpdated(),
                form.getCustomerAccessHours(),
                form.getSubmissionRetentionWeeks(),
                form.getPdfTemplateKey(),
                form.getPaymentProducts(),
                form.getPaymentPurpose(),
                form.getPaymentDescription(),
                form.getPaymentProviderKey(),
                form.getIdentityVerificationRequired(),
                form.getIdentityProviders()
        );
    }

    public static FormDetailsResponseDTO fromEntity(FormVersionWithMembershipEntity form) {
        return new FormDetailsResponseDTO(
                form.getId(),
                form.getSlug(),
                form.getVersion(),
                form.getInternalTitle(),
                form.getType(),
                form.getRootElement(),
                form.getDestinationId(),
                form.getLegalSupportDepartmentId(),
                form.getTechnicalSupportDepartmentId(),
                form.getImprintDepartmentId(),
                form.getPrivacyDepartmentId(),
                form.getAccessibilityDepartmentId(),
                form.getDevelopingDepartmentId(),
                form.getManagingDepartmentId(),
                form.getResponsibleDepartmentId(),
                form.getThemeId(),
                form.getCreated(),
                form.getUpdated(),
                form.getCustomerAccessHours(),
                form.getSubmissionRetentionWeeks(),
                form.getPdfTemplateKey(),
                form.getPaymentProducts(),
                form.getPaymentPurpose(),
                form.getPaymentDescription(),
                form.getPaymentProviderKey(),
                form.getIdentityVerificationRequired(),
                form.getIdentityProviders()
        );
    }
}
