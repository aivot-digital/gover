import {type RootElement} from '../elements/root-element';
import {ApplicationStatus} from '../../data/application-status';
import {PaymentProduct} from '../payment/payment-product';
import {FormType} from '../../modules/forms/enums/form-type';
import {IdentityProviderLink} from '../../modules/identity/models/identity-provider-link';
import {FormDetailsResponseDTO} from '../../modules/forms/dtos/form-details-response-dto';


export type Form = FormDetailsResponseDTO;

export type FormListProjection = Omit<Form, 'rootElement'>;
export type FormListProjectionPublic = Omit<FormListProjection, 'status' | 'destinationId' | 'created' | 'customerAccessHours' | 'submissionRetentionWeeks' | 'totalSubmissions' | 'openSubmissions' | 'inProgressSubmissions' | 'bundIdLevel' | 'bayernIdLevel' | 'shIdLevel' | 'mukLevel'>;
export type FormPublicProjection = Omit<Form, 'pdfTemplateKey' | 'paymentEndpointId' | 'paymentOriginatorId' | 'paymentPurpose' | 'paymentProducts' | 'paymentDescription'>;

export function isForm(obj: any): obj is Form {
    return obj != null && 'slug' in obj;
}
