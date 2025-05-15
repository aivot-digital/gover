export interface PaymentProviderRequestDTO {
    name: string;
    description: string;
    providerKey: string;
    isTestProvider: boolean;
    isEnabled: boolean;
    config: Record<string, any>;
}
