export enum PaymentProvider {
    ePayBL = 'epaybl', // Must be in sync with data in application.properties
    pmPayment = 'pmpayment', // Must be in sync with data in application.properties
    GiroPay = 'giropay', // Must be in sync with data in application.properties
}

export const PaymentProviderLabels: Record<PaymentProvider, string> = {
    [PaymentProvider.ePayBL]: 'ePayBL', // Must be in sync with data in application.properties
    [PaymentProvider.pmPayment]: 'pmPayment', // Must be in sync with data in application.properties
    [PaymentProvider.GiroPay]: 'giropay', // Must be in sync with data in application.properties
};
