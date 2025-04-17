package de.aivot.GoverBackend.form.dtos;

import de.aivot.GoverBackend.elements.models.RootElement;
import de.aivot.GoverBackend.form.entities.Form;
import de.aivot.GoverBackend.form.entities.FormWithMembership;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.enums.FormType;
import de.aivot.GoverBackend.identity.models.IdentityProviderLink;
import de.aivot.GoverBackend.models.payment.PaymentProduct;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public record FormDetailsResponseDTO(
        Integer id,
        String slug,
        String version,
        String title,
        FormStatus status,
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
        String pdfBodyTemplateKey,
        Collection<PaymentProduct> products,
        String paymentPurpose,
        String paymentDescription,
        String paymentProvider,
        Boolean identityRequired,
        List<IdentityProviderLink> identityProviders
) {
    public static FormDetailsResponseDTO fromEntity(Form form) {
        return new FormDetailsResponseDTO(
                form.getId(),
                form.getSlug(),
                form.getVersion(),
                form.getTitle(),
                form.getStatus(),
                form.getType(),
                form.getRoot(),
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
                form.getSubmissionDeletionWeeks(),
                form.getPdfBodyTemplateKey(),
                form.getProducts(),
                form.getPaymentPurpose(),
                form.getPaymentDescription(),
                form.getPaymentProvider(),
                form.getIdentityRequired(),
                form.getIdentityProviders()
        );
    }

    public static FormDetailsResponseDTO fromEntity(FormWithMembership form) {
        return new FormDetailsResponseDTO(
                form.getId(),
                form.getSlug(),
                form.getVersion(),
                form.getTitle(),
                form.getStatus(),
                form.getType(),
                form.getRoot(),
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
                form.getSubmissionDeletionWeeks(),
                form.getPdfBodyTemplateKey(),
                form.getProducts(),
                form.getPaymentPurpose(),
                form.getPaymentDescription(),
                form.getPaymentProvider(),
                form.getIdentityRequired(),
                form.getIdentityProviders()
        );
    }
}
