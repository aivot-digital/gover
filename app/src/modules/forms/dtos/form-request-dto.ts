import {IdentityProviderLink} from '../../identity/models/identity-provider-link';
import {PaymentProduct} from '../../../models/payment/payment-product';
import {RootElement} from '../../../models/elements/root-element';
import {FormType} from '../enums/form-type';

export interface FormRequestDTO {
    slug: string;
    internalTitle: string;
    publicTitle: string;
    developingDepartmentId: number;
    managingDepartmentId?: number | null;
    responsibleDepartmentId?: number | null;
    type: FormType;
    legalSupportDepartmentId?: number | null;
    technicalSupportDepartmentId?: number | null;
    imprintDepartmentId?: number | null;
    privacyDepartmentId?: number | null;
    accessibilityDepartmentId?: number | null;
    destinationId?: number | null;
    themeId?: number | null;
    pdfTemplateKey?: string | null; // UUID as string
    paymentProviderKey?: string | null; // UUID as string
    paymentPurpose?: string | null;
    paymentDescription?: string | null;
    paymentProducts?: PaymentProduct[] | null;
    identityVerificationRequired: boolean;
    identityProviders: IdentityProviderLink[];
    customerAccessHours?: number | null;
    submissionRetentionWeeks?: number | null;
    rootElement: RootElement;
}