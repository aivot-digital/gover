import {Api} from '../../hooks/use-api';
import {PaymentProviderRequestDTO} from './dtos/payment-provider-request-dto';
import {PaymentProviderTestDataResponseDTO} from './dtos/payment-provider-test-data-response-dto';
import {PaymentProviderTestDataRequestDTO} from './dtos/payment-provider-test-data-request-dto';
import {PaymentProviderDefinitionResponseDTO} from './dtos/payment-provider-definition-response-dto';
import {PaymentProviderResponseDTO} from './dtos/payment-provider-response-dto';
import {CrudApiService} from '../../services/crud-api-service';

export interface PaymentProvidersFilter {
    name: string;
    isEnabled: boolean;
}

export class PaymentProvidersApiService extends CrudApiService<PaymentProviderRequestDTO, PaymentProviderResponseDTO, PaymentProviderResponseDTO, PaymentProviderResponseDTO, PaymentProviderResponseDTO, string, PaymentProvidersFilter> {
    constructor(api: Api) {
        super(api, 'payment-providers/');
    }

    public async listDefinitions(): Promise<PaymentProviderDefinitionResponseDTO[]> {
        return await this.api.get<PaymentProviderDefinitionResponseDTO[]>(`payment-provider-definitions/`, {});
    }

    public initialize(): PaymentProviderResponseDTO {
        return {
            key: '',
            providerKey: '',
            name: '',
            description: '',
            isTestProvider: false,
            isEnabled: false,
            config: {},
        };
    }

    public async test(key: string, purpose: string, description: string, amount: number): Promise<PaymentProviderTestDataResponseDTO> {
        const request: PaymentProviderTestDataRequestDTO = {
            purpose,
            description,
            amount,
        };
        return await this.api.post<PaymentProviderTestDataResponseDTO>(`payment-providers/${key}/test/`, request, {});
    }
}
