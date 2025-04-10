import {XBezahldienstePaymentRequest} from '../../../models/xbezahldienste/x-bezahldienste-payment-request';
import {XBezahldienstePaymentInformation} from '../../../models/xbezahldienste/x-bezahldienste-paymentInformation';

export interface PaymentTransactionResponseDTO {
    key: string;
    paymentProviderKey: string;
    paymentRequest?: XBezahldienstePaymentRequest | null;
    paymentInformation: XBezahldienstePaymentInformation | null;
    paymentError?: string | null;
}
