package de.aivot.GoverBackend.form.dtos;

import de.aivot.GoverBackend.enums.*;
import de.aivot.GoverBackend.form.entities.Form;
import de.aivot.GoverBackend.form.entities.FormWithMembership;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.enums.FormType;
import de.aivot.GoverBackend.elements.models.RootElement;
import de.aivot.GoverBackend.models.payment.PaymentProduct;

import java.time.LocalDateTime;
import java.util.Collection;

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
        Boolean bundIdEnabled,
        BundIdAccessLevel bundIdLevel,
        Boolean bayernIdEnabled,
        BayernIdAccessLevel bayernIdLevel,
        Boolean shIdEnabled,
        SchleswigHolsteinIdAccessLevel shIdLevel,
        Boolean mukEnabled,
        MukAccessLevel mukLevel,
        String pdfBodyTemplateKey,
        Collection<PaymentProduct> products,
        String paymentPurpose,
        String paymentDescription,
        String paymentProvider
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
                form.getBundIdEnabled(),
                form.getBundIdLevel(),
                form.getBayernIdEnabled(),
                form.getBayernIdLevel(),
                form.getShIdEnabled(),
                form.getShIdLevel(),
                form.getMukEnabled(),
                form.getMukLevel(),
                form.getPdfBodyTemplateKey(),
                form.getProducts(),
                form.getPaymentPurpose(),
                form.getPaymentDescription(),
                form.getPaymentProvider()
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
                form.getBundIdEnabled(),
                form.getBundIdLevel(),
                form.getBayernIdEnabled(),
                form.getBayernIdLevel(),
                form.getShIdEnabled(),
                form.getShIdLevel(),
                form.getMukEnabled(),
                form.getMukLevel(),
                form.getPdfBodyTemplateKey(),
                form.getProducts(),
                form.getPaymentPurpose(),
                form.getPaymentDescription(),
                form.getPaymentProvider()
        );
    }
}
