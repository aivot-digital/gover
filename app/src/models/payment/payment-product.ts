import {Function} from '../functions/function';

export interface PaymentProduct {
    id: string;
    reference: string;
    description: string;
    type: PaymentType;
    upfrontFixedQuantity?: number;
    upfrontQuantityFunction?: Omit<Function, 'code' | 'conditionSet'> & { code: string };
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
    key: string;
    value: string;
}