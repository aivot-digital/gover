import {FormStatus} from '../../forms/enums/form-status';
import {FormType} from '../../forms/enums/form-type';

export interface SubmissionWithMembershipResponseDTO {
    id: string;
    formId: number | null;
    created: string; // ISO date string
    assigneeId: string | null;
    archived: string | null; // ISO date string
    fileNumber: string | null;
    destinationId: number | null;
    customerInput: any; // ElementData type
    destinationSuccess: boolean | null;
    isTestSubmission: boolean | null;
    copySent: boolean | null;
    copyTries: number | null;
    reviewScore: number | null;
    destinationResult: string | null;
    destinationTimestamp: string | null; // ISO date string
    status: string; // SubmissionStatus enum
    updated: string; // ISO date string
    paymentTransactionKey: string | null;

    formVersion: number | null;
    formSlug: string | null;

    formInternalTitle: string | null;
    formPublicTitle: string | null;

    formDevelopingDepartmentId: number | null;
    formManagingDepartmentId: number | null;
    formResponsibleDepartmentId: number | null;

    formCreated: string | null; // ISO date string
    formUpdated: string | null; // ISO date string

    formPublishedVersion: number | null;
    formDraftedVersion: number | null;

    formStatus: FormStatus; // FormStatus enum
    formType: FormType; // FormType enum

    formLegalSupportDepartmentId: number | null;
    formTechnicalSupportDepartmentId: number | null;
    formImprintDepartmentId: number | null;
    formPrivacyDepartmentId: number | null;
    formAccessibilityDepartmentId: number | null;

    formCustomerAccessHours: number | null;
    formSubmissionRetentionWeeks: number | null;

    formThemeId: number | null;

    formPdfTemplateKey: string | null; // UUID as string

    formPaymentProviderKey: string | null; // UUID as string
    formPaymentPurpose: string | null;
    formPaymentDescription: string | null;
    formPaymentProducts: any[]; // PaymentProduct[]

    formIdentityProviders: any[]; // IdentityProviderLink[]
    formIdentityVerificationRequired: boolean | null;

    formDestinationId: number | null;

    formRootElement: any; // RootElement

    formVersionPublished: string | null; // ISO date string
    formVersionRevoked: string | null; // ISO date string

    userId: string | null;
    userEmail: string | null;
    userFirstName: string | null;
    userLastName: string | null;
    userFullName: string | null;
    userEnabled: boolean | null;
    userVerified: boolean | null;
    userGlobalAdmin: boolean | null;
    userDeletedInIdp: boolean | null;
    userIsDeveloper: boolean | null;
    userIsManager: boolean | null;
    userIsResponsible: boolean | null;
}
