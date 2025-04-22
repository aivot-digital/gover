import {type RootElement} from '../elements/root-element';
import {ApplicationStatus} from '../../data/application-status';
import {PaymentProduct} from '../payment/payment-product';
import {FormType} from '../../modules/forms/enums/form-type';
import {IdentityProviderLink} from '../../modules/identity/models/identity-provider-link';


export interface Form {
    id: number;
    slug: string;
    version: string;
    title: string;
    status: ApplicationStatus;
    type: FormType;
    root: RootElement;

    destinationId: number | null;
    legalSupportDepartmentId: number | null;
    technicalSupportDepartmentId: number | null;
    imprintDepartmentId: number | null;
    privacyDepartmentId: number | null;
    accessibilityDepartmentId: number | null;
    developingDepartmentId: number;
    managingDepartmentId: number | null;
    responsibleDepartmentId: number | null;
    themeId: number | null;

    created: string;
    updated: string;

    customerAccessHours: number;
    submissionDeletionWeeks: number;

    pdfBodyTemplateKey?: string | null;

    products?: PaymentProduct[];
    paymentPurpose?: string;
    paymentDescription?: string;
    paymentProvider?: string;

    identityRequired: boolean;
    identityProviders: IdentityProviderLink[];
}

export type FormListProjection = Omit<Form, 'root'>;
export type FormListProjectionPublic = Omit<FormListProjection, 'status' | 'destinationId' | 'created' | 'customerAccessHours' | 'submissionDeletionWeeks' | 'totalSubmissions' | 'openSubmissions' | 'inProgressSubmissions' | 'bundIdLevel' | 'bayernIdLevel' | 'shIdLevel' | 'mukLevel'>;
export type FormPublicProjection = Omit<Form, 'pdfBodyTemplateKey' | 'paymentEndpointId' | 'paymentOriginatorId' | 'paymentPurpose' | 'products' | 'paymentDescription'>;

export function isForm(obj: any): obj is Form {
    return obj != null && 'slug' in obj;
}
