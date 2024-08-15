import {XBezahldienstePaymentItem} from './x-bezahldienste-payment-item';
import {XBezahldiensteRequestor} from './x-bezahldienste-requestor';

export interface XBezahldienstePaymentRequest {
    requestId?: string;
    requestTimestamp?: string;
    currency?: string;
    grosAmount?: number;
    purpose?: string;
    description?: string;
    redirectUrl?: string;
    items?: XBezahldienstePaymentItem[];
    requestor?: XBezahldiensteRequestor;
}