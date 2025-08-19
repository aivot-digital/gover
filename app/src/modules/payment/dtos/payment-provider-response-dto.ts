import {ElementData} from '../../../models/element-data';

export interface PaymentProviderResponseDTO {
    key: string;
    name: string;
    description: string;
    providerKey: string;
    isTestProvider: boolean;
    isEnabled: boolean;
    config: ElementData;
}
