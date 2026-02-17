import {RootElement} from '../../../models/elements/root-element';
import {PaymentProduct} from '../../../models/payment/payment-product';
import {IdentityProviderLink} from '../../identity/models/identity-provider-link';
import {FormType} from '../enums/form-type';
import {FormStatus} from '../enums/form-status';

export interface VFormVersionWithDetailsEntity {
    id: number;
    slug: string;
    internalTitle: string;
    developingDepartmentId: number;
    publishedVersion: number;
    draftedVersion: number;
    versionCount: number;
    formId: number;
    version: number;
    status: FormStatus;
    type: FormType;
    legalSupportDepartmentId: number;
    technicalSupportDepartmentId: number;
    imprintDepartmentId: number;
    privacyDepartmentId: number;
    accessibilityDepartmentId: number;
    destinationId: number;
    themeId: number;
    pdfTemplateKey: string;
    paymentProviderKey: string;
    paymentPurpose: string;
    paymentDescription: string;
    paymentProducts: PaymentProduct[];
    identityProviders: IdentityProviderLink[]
    identityVerificationRequired: boolean;
    customerAccessHours: number;
    submissionRetentionWeeks: number;
    rootElement: RootElement;
    created: string;
    updated: string;
    published: string;
    revoked: string;
    publicTitle: string;
    managingDepartmentId: number;
    responsibleDepartmentId: number;
}