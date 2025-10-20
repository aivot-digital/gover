import {IdentityProviderLink} from '../../identity/models/identity-provider-link';
import {PaymentProduct} from '../../../models/payment/payment-product';
import {FormType} from '../enums/form-type';
import {RootElement} from '../../../models/elements/root-element';
import {FormRequestDTO} from './form-request-dto';
import {FormStatus} from '../enums/form-status';
import {FormCitizenDetailsResponseDTO} from './form-citizen-details-response-dto';

export interface FormDetailsResponseDTO {
    id: number;
    slug: string;
    internalTitle: string;
    publicTitle: string;
    developingDepartmentId: number;
    managingDepartmentId: number | null;
    responsibleDepartmentId: number | null;
    publishedVersion: number | null;
    draftedVersion: number | null;
    versionCount: number;
    formId: number;
    version: number;
    status: FormStatus;
    type: FormType;
    legalSupportDepartmentId: number | null;
    technicalSupportDepartmentId: number | null;
    imprintDepartmentId: number | null;
    privacyDepartmentId: number | null;
    accessibilityDepartmentId: number | null;
    destinationId: number | null;
    themeId: number | null;
    pdfTemplateKey: string | null; // UUID as string
    paymentProviderKey: string | null; // UUID as string
    paymentPurpose: string | null;
    paymentDescription: string | null;
    paymentProducts: PaymentProduct[];
    identityProviders: IdentityProviderLink[];
    identityVerificationRequired: boolean;
    customerAccessHours: number | null;
    submissionRetentionWeeks: number | null;
    rootElement: RootElement;
    created: string | null; // ISO date string
    updated: string | null; // ISO date string
    published: string | null; // ISO date string
    revoked: string | null; // ISO date string
}

export function asFormRequestDTO(form: FormDetailsResponseDTO): FormRequestDTO {
    return {
        slug: form.slug,
        internalTitle: form.internalTitle,
        publicTitle: form.publicTitle,
        developingDepartmentId: form.developingDepartmentId,
        managingDepartmentId: form.managingDepartmentId,
        responsibleDepartmentId: form.responsibleDepartmentId,
        type: form.type,
        legalSupportDepartmentId: form.legalSupportDepartmentId,
        technicalSupportDepartmentId: form.technicalSupportDepartmentId,
        imprintDepartmentId: form.imprintDepartmentId,
        privacyDepartmentId: form.privacyDepartmentId,
        accessibilityDepartmentId: form.accessibilityDepartmentId,
        destinationId: form.destinationId,
        themeId: form.themeId,
        pdfTemplateKey: form.pdfTemplateKey,
        paymentProviderKey: form.paymentProviderKey,
        paymentPurpose: form.paymentPurpose,
        paymentDescription: form.paymentDescription,
        paymentProducts: form.paymentProducts,
        identityVerificationRequired: form.identityVerificationRequired,
        identityProviders: form.identityProviders,
        customerAccessHours: form.customerAccessHours,
        submissionRetentionWeeks: form.submissionRetentionWeeks,
        rootElement: form.rootElement,
    };
}

export function formCitizenDetailsResponseDTO(form: FormCitizenDetailsResponseDTO): FormDetailsResponseDTO {
    return {
        id: form.id,
        slug: form.slug,
        internalTitle: form.title,
        publicTitle: form.title,
        developingDepartmentId: form.developingDepartmentId,
        managingDepartmentId: form.managingDepartmentId ?? null,
        responsibleDepartmentId: form.responsibleDepartmentId ?? null,
        publishedVersion: form.version,
        draftedVersion: null,
        versionCount: 0,
        formId: form.id,
        version: form.version,
        status: FormStatus.Published,
        type: FormType.Public,
        legalSupportDepartmentId: form.legalSupportDepartmentId ?? null,
        technicalSupportDepartmentId: form.technicalSupportDepartmentId ?? null,
        imprintDepartmentId: form.imprintDepartmentId ?? null,
        privacyDepartmentId: form.privacyDepartmentId ?? null,
        accessibilityDepartmentId: form.accessibilityDepartmentId ?? null,
        destinationId: null,
        themeId: form.themeId,
        pdfTemplateKey: null,
        paymentProviderKey: null,
        paymentPurpose: null,
        paymentDescription: null,
        paymentProducts: [],
        identityProviders: form.identityProviders,
        identityVerificationRequired: form.identityRequired,
        customerAccessHours: null,
        submissionRetentionWeeks: null,
        rootElement: form.root,
        created: null,
        updated: null,
        published: null,
        revoked: null,
    };
}