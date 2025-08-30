package de.aivot.GoverBackend.form.dtos;

import de.aivot.GoverBackend.elements.models.elements.RootElement;
import de.aivot.GoverBackend.form.entities.Form;
import de.aivot.GoverBackend.form.entities.FormVersionWithMembershipEntity;
import de.aivot.GoverBackend.form.enums.FormStatus;
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
        UUID paymentProvider,
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

    public static FormDetailsResponseDTO fromEntity(FormVersionWithMembershipEntity form) {
        return new FormDetailsResponseDTO(
                form.getId(),
                form.getSlug(),
                null, // TODO form.getVersion(),
                form.getTitle(),
                null, // TODO form.getStatus(),
                null, // TODO form.getType(),
                null, // TODO form.getRoot(),
                null, // TODO form.getDestinationId(),
                null, // TODO form.getLegalSupportDepartmentId(),
                null, // TODO form.getTechnicalSupportDepartmentId(),
                null, // TODO form.getImprintDepartmentId(),
                null, // TODO form.getPrivacyDepartmentId(),
                null, // TODO form.getAccessibilityDepartmentId(),
                form.getDevelopingDepartmentId(),
                form.getManagingDepartmentId(),
                form.getResponsibleDepartmentId(),
                null, // TODO form.getThemeId(),
                form.getCreated(),
                form.getUpdated(),
                null, // TODO form.getCustomerAccessHours(),
                null, // TODO form.getSubmissionDeletionWeeks(),
                null, // TODO form.getPdfBodyTemplateKey(),
                null, // TODO form.getProducts(),
                null, // TODO form.getPaymentPurpose(),
                null, // TODO form.getPaymentDescription(),
                null, // TODO form.getPaymentProvider(),
                null, // TODO form.getIdentityRequired(),
                null // TODO form.getIdentityProviders()
        );
    }
}
