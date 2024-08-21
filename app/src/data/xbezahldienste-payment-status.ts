export enum XBezahldienstePaymentStatus {
    Initial = 'INITIAL',
    Payed = 'PAYED',
    Failed = 'FAILED',
    Canceled = 'CANCELED',
}

export const XBezahldienstePaymentStatusLabels: Record<XBezahldienstePaymentStatus, string> = {
    [XBezahldienstePaymentStatus.Initial]: 'Zahlung ausstehend',
    [XBezahldienstePaymentStatus.Payed]: 'Bezahlt',
    [XBezahldienstePaymentStatus.Failed]: 'Bezahlung fehlgeschlagen',
    [XBezahldienstePaymentStatus.Canceled]: 'Zahlung abgebrochen',
}