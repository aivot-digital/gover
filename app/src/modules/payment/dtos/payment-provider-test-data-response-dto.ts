import {XBezahldienstePaymentRequest} from '../../../models/xbezahldienste/x-bezahldienste-payment-request';
import {XBezahldienstePaymentTransaction} from '../../../models/xbezahldienste/x-bezahldienste-payment-transaction';

export interface PaymentProviderTestDataResponseDTO {
    ok: boolean;
    request?: XBezahldienstePaymentRequest | null;
    transaction?: XBezahldienstePaymentTransaction | null;
    errorMessage?: string | null;
}
