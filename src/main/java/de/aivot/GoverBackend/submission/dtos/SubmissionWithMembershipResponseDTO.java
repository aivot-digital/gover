package de.aivot.GoverBackend.submission.dtos;

import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.elements.RootElement;
import de.aivot.GoverBackend.enums.SubmissionStatus;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.enums.FormType;
import de.aivot.GoverBackend.identity.models.IdentityProviderLink;
import de.aivot.GoverBackend.models.payment.PaymentProduct;
import de.aivot.GoverBackend.submission.entities.SubmissionWithMembership;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public record SubmissionWithMembershipResponseDTO(
        String id,
        Integer formId,
        LocalDateTime created,
        String assigneeId,
        LocalDateTime archived,
        String fileNumber,
        Integer destinationId,
        ElementData customerInput,
        Boolean destinationSuccess,
        Boolean isTestSubmission,
        Boolean copySent,
        Integer copyTries,
        Integer reviewScore,
        String destinationResult,
        LocalDateTime destinationTimestamp,
        SubmissionStatus status,
        LocalDateTime updated,
        String paymentTransactionKey,

        Integer formVersion,
        String formSlug,

        String formInternalTitle,
        String formPublicTitle,

        Integer formDevelopingDepartmentId,
        Integer formManagingDepartmentId,
        Integer formResponsibleDepartmentId,

        LocalDateTime formCreated,
        LocalDateTime formUpdated,

        Integer formPublishedVersion,
        Integer formDraftedVersion,

        FormStatus formStatus,
        FormType formType,

        Integer formLegalSupportDepartmentId,
        Integer formTechnicalSupportDepartmentId,
        Integer formImprintDepartmentId,
        Integer formPrivacyDepartmentId,
        Integer formAccessibilityDepartmentId,

        Integer formCustomerAccessHours,
        Integer formSubmissionRetentionWeeks,

        Integer formThemeId,

        UUID formPdfTemplateKey,

        UUID formPaymentProviderKey,
        String formPaymentPurpose,
        String formPaymentDescription,
        Collection<PaymentProduct> formPaymentProducts,

        List<IdentityProviderLink> formIdentityProviders,
        Boolean formIdentityVerificationRequired,

        Integer formDestinationId,

        RootElement formRootElement,

        LocalDateTime formVersionPublished,
        LocalDateTime formVersionRevoked,

        String userId,
        String userEmail,
        String userFirstName,
        String userLastName,
        String userFullName,
        Boolean userEnabled,
        Boolean userVerified,
        Boolean userGlobalAdmin,
        Boolean userDeletedInIdp,
        Boolean userIsDeveloper,
        Boolean userIsManager,
        Boolean userIsResponsible
) {

    public static SubmissionWithMembershipResponseDTO from(SubmissionWithMembership submission) {

        return new SubmissionWithMembershipResponseDTO(
                submission.getId(),
                submission.getFormId(),
                submission.getCreated(),
                submission.getAssigneeId(),
                submission.getArchived(),
                submission.getFileNumber(),
                submission.getDestinationId(),
                submission.getCustomerInput(),
                submission.getDestinationSuccess(),
                submission.getTestSubmission(),
                submission.getCopySent(),
                submission.getCopyTries(),
                submission.getReviewScore(),
                submission.getDestinationResult(),
                submission.getDestinationTimestamp(),
                submission.getStatus(),
                submission.getUpdated(),
                submission.getPaymentTransactionKey(),

                submission.getFormVersion(),
                submission.getFormSlug(),

                submission.getFormInternalTitle(),
                submission.getFormPublicTitle(),

                submission.getFormDevelopingDepartmentId(),
                submission.getFormManagingDepartmentId(),
                submission.getFormResponsibleDepartmentId(),

                submission.getFormCreated(),
                submission.getFormUpdated(),

                submission.getFormPublishedVersion(),
                submission.getFormDraftedVersion(),

                submission.getFormStatus(),
                submission.getFormType(),

                submission.getFormLegalSupportDepartmentId(),
                submission.getFormTechnicalSupportDepartmentId(),
                submission.getFormImprintDepartmentId(),
                submission.getFormPrivacyDepartmentId(),
                submission.getFormAccessibilityDepartmentId(),

                submission.getFormCustomerAccessHours(),
                submission.getFormSubmissionRetentionWeeks(),

                submission.getFormThemeId(),

                submission.getFormPdfTemplateKey(),

                submission.getFormPaymentProviderKey(),
                submission.getFormPaymentPurpose(),
                submission.getFormPaymentDescription(),
                submission.getFormPaymentProducts(),

                submission.getFormIdentityProviders(),
                submission.getFormIdentityVerificationRequired(),

                submission.getFormDestinationId(),

                submission.getFormRootElement(),

                submission.getFormVersionPublished(),
                submission.getFormVersionRevoked(),

                submission.getUserId(),
                submission.getUserEmail(),
                submission.getUserFirstName(),
                submission.getUserLastName(),
                submission.getUserFullName(),
                submission.getUserEnabled(),
                submission.getUserVerified(),
                submission.getUserGlobalAdmin(),
                submission.getUserDeletedInIdp(),
                submission.getUserIsDeveloper(),
                submission.getUserIsManager(),
                submission.getUserIsResponsible()
        );
    }

}
