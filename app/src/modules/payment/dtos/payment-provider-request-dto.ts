export interface PaymentProviderRequestDTO {
    name: string;
    description: string;
    providerKey: string;
    providerVersion: number;
    isTestProvider: boolean;
    isEnabled: boolean;
    config: Record<string, any>;
}
