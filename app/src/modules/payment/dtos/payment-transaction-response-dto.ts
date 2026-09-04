import {PaymentRequest} from '../../../models/payment/payment-request';
import {PaymentInformation} from '../../../models/payment/payment-information';
import {PaymentStatus} from '../../../data/payment-status';

export interface PaymentTransactionResponseDTO {
    key: string;
    paymentProviderKey: string;
    paymentRequest: PaymentRequest | null;
    paymentInformation: PaymentInformation | null;
    paymentError?: string | null;
    hasError: boolean;
    status: PaymentStatus;
    created: string;
}
