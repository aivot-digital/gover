import {AuthoredElementValues} from '../../../models/element-data';

export interface PaymentProviderResponseDTO {
    key: string;
    name: string;
    description: string;
    providerKey: string;
    providerVersion: number;
    isTestProvider: boolean;
    isEnabled: boolean;
    config: AuthoredElementValues;
}
