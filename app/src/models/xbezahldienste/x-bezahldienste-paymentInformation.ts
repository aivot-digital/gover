import {XBezahldienstePaymentStatus} from '../../data/xbezahldienste-payment-status';
import {XBezahldienstPaymentMethod} from '../../data/xbezahldienste-payment-method';

export interface XBezahldienstePaymentInformation {
    transactionUrl?: string;
    transactionRedirectUrl?: string;
    transactionId?: string;
    transactionReference?: string;
    transactionTimestamp?: string;
    paymentMethod?: XBezahldienstPaymentMethod;
    paymentMethodDetail?: string;
    status?: XBezahldienstePaymentStatus;
    statusDetail?: string;
}