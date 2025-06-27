import {Function} from '../functions/function';
import {JavascriptCode} from '../functions/javascript-code';

export interface PaymentProduct {
    id: string;
    reference: string;
    description: string;
    type: PaymentType;
    upfrontFixedQuantity?: number;
    upfrontQuantityFunction?: Omit<Function, 'code' | 'conditionSet'> & { code: string };
    upfrontQuantityJavascript?: JavascriptCode;
    taxRate: number;
    netPrice: number;
    bookingData: BookingDataItem[];
    taxInformation: string;
}

export enum PaymentType {
    UPFRONT_FIXED = 'upfrontFixed',
    UPFRONT_CALCULATED = 'upfrontCalculated',
    DOWNSTREAM = 'downstream',
}

export interface BookingDataItem {
    key: string | null | undefined;
    value: string | null | undefined;
}