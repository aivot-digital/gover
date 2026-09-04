import {PaymentRequest} from '../../../models/payment/payment-request';
import {PaymentInformation} from '../../../models/payment/payment-information';

export interface PaymentProviderTestDataResponseDTO {
    ok: boolean;
    request?: PaymentRequest | null;
    paymentInformation?: PaymentInformation | null;
    errorMessage?: string | null;
}
