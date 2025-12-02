import {RootElement} from '../../../models/elements/root-element';
import {PaymentProduct} from '../../../models/payment/payment-product';
import {IdentityProviderLink} from '../../identity/models/identity-provider-link';
import {FormStatus} from '../enums/form-status';
import {FormType} from '../enums/form-type';

export interface FormVersionEntity {
    formId: number;
    publicTitle: string;
    version: number;
    status: FormStatus;
    type: FormType;
    managingDepartmentId: number | null;
    responsibleDepartmentId: number | null;
    legalSupportDepartmentId: number | null;
    technicalSupportDepartmentId: number | null;
    imprintDepartmentId: number | null;
    privacyDepartmentId: number | null;
    accessibilityDepartmentId: number | null;
    destinationId: number | null;
    customerAccessHours: number | null;
    submissionRetentionWeeks: number | null;
    themeId: number | null;
    pdfTemplateKey: string | null;
    paymentProviderKey: string | null;
    paymentPurpose: string;
    paymentDescription: string;
    paymentProducts: PaymentProduct[];
    identityProviders: IdentityProviderLink[];
    identityVerificationRequired: boolean;
    rootElement: RootElement;
    created: string;
    updated: string;
    published: string | null;
    revoked: string | null;
}

export function isFormVersionEntity(object: any): object is FormVersionEntity {
    return object != null && typeof object.formId === 'number' && typeof object.version === 'number' && typeof object.status === 'string' && typeof object.type === 'string' && object.rootElement != null;
}