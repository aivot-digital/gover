export interface PaymentProviderRequestDTO {
    name: string;
    description: string;
    providerKey: string;
    isTestProvider: boolean;
    config: Record<string, any>;
}
