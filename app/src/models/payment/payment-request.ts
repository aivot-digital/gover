export type PaymentGender = 'M' | 'F' | 'D';

export interface PaymentAddress {
    street: string | null;
    houseNumber: string | null;
    addressLines: string[];
    postalCode: string | null;
    city: string | null;
    country: string | null;
}

export interface PaymentRequestor {
    name: string | null;
    firstName: string | null;
    gender: PaymentGender | null;
    organization: boolean;
    organizationName: string | null;
    address: PaymentAddress | null;
}

export interface PaymentRequestItem {
    id: string;
    reference: string;
    description: string;
    taxRate: number;
    quantity: number;
    totalNetAmount: number;
    totalTaxAmount: number;
    singleNetAmount: number;
    singleTaxAmount: number;
    bookingData: Record<string, string>;
}

export interface PaymentRequest {
    requestId: string;
    requestTimestamp: string;
    currency: string;
    grossAmount: number;
    purpose: string;
    description: string | null;
    redirectUrl: string;
    items: PaymentRequestItem[];
    requestor: PaymentRequestor | null;
}
