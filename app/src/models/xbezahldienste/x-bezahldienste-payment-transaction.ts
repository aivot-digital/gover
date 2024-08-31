import {XBezahldienstePaymentRequest} from './x-bezahldienste-payment-request';
import {XBezahldienstePaymentInformation} from './x-bezahldienste-paymentInformation';

export interface XBezahldienstePaymentTransaction {
    paymentInformation?: XBezahldienstePaymentInformation;
    paymentRequest?: XBezahldienstePaymentRequest;
}