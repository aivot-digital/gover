export enum PaymentStatus {
    Pending = 'PENDING',
    Paid = 'PAID',
    Failed = 'FAILED',
    Canceled = 'CANCELED',
}

export const PaymentStatusLabels: Record<PaymentStatus, string> = {
    [PaymentStatus.Pending]: 'Zahlung ausstehend',
    [PaymentStatus.Paid]: 'Bezahlt',
    [PaymentStatus.Failed]: 'Bezahlung fehlgeschlagen',
    [PaymentStatus.Canceled]: 'Zahlung abgebrochen',
};
