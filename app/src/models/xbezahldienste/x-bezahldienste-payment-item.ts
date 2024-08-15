export interface XBezahldienstePaymentItem {
    id?: string;
    reference?: string;
    description?: string;
    taxRate?: number;
    quantity?: number;
    totalNetAmount?: number;
    totalTaxAmount?: number;
    singleNetAmount?: number;
    singleTaxAmount?: number;
    bookingData?: { [key: string]: string };
}