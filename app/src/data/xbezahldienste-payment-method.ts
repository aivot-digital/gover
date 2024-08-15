export enum XBezahldienstPaymentMethod {
    GIROPAY = 'GIROPAY',
    PAYDIRECT = 'PAYDIRECT',
    CREDITCARD = 'CREDITCARD',
    PAYPAL = 'PAYPAL',
    OTHER = 'OTHER'
}

export const XBezahldienstPaymentMethodLabels: Record<XBezahldienstPaymentMethod, string> = {
    [XBezahldienstPaymentMethod.GIROPAY]: 'Giropay',
    [XBezahldienstPaymentMethod.PAYDIRECT]: 'PayDirect',
    [XBezahldienstPaymentMethod.CREDITCARD]: 'Kreditkarte',
    [XBezahldienstPaymentMethod.PAYPAL]: 'PayPal',
    [XBezahldienstPaymentMethod.OTHER]: 'Sonstige',
}