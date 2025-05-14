// TODO: Implement this DTO
export interface PaymentProviderResponseDTO {
    key: string;
    name: string;
    description: string;
    providerKey: string;
    isTestProvider: boolean;
    isEnabled: boolean;
    config: Record<string, any>;
}
