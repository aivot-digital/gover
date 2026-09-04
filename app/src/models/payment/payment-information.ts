import {PaymentStatus} from '../../data/payment-status';

export interface PaymentMethod {
    code: string;
    detail: string | null;
}

export interface PaymentInformation {
    providerTransactionId: string;
    providerReference: string | null;
    status: PaymentStatus;
    paymentUrl: string | null;
    paidAt: string | null;
    paymentMethod: PaymentMethod | null;
    statusMessage: string | null;
}
