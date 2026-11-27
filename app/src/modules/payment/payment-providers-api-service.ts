import {PaymentProviderRequestDTO} from './dtos/payment-provider-request-dto';
import {PaymentProviderTestDataResponseDTO} from './dtos/payment-provider-test-data-response-dto';
import {PaymentProviderTestDataRequestDTO} from './dtos/payment-provider-test-data-request-dto';
import {PaymentProviderDefinitionResponseDTO} from './dtos/payment-provider-definition-response-dto';
import {PaymentProviderResponseDTO} from './dtos/payment-provider-response-dto';
import {BaseCrudApiService} from '../../services/base-crud-api-service';
import {type AuthoredElementValues, type DerivedRuntimeElementData} from '../../models/element-data';
import {type ElementDerivationOptions} from '../elements/elements-api-service';

export interface PaymentProvidersFilter {
    name: string;
    isEnabled: boolean;
}

export class PaymentProvidersApiService extends BaseCrudApiService<
    PaymentProviderRequestDTO,
    PaymentProviderResponseDTO,
    PaymentProviderResponseDTO,
    PaymentProviderRequestDTO,
    string,
    PaymentProvidersFilter
> {
    constructor() {
        super('/api/payment-providers/');
    }

    public async listDefinitions(): Promise<PaymentProviderDefinitionResponseDTO[]> {
        return await this.get<PaymentProviderDefinitionResponseDTO[]>(`/api/payment-provider-definitions/`, {});
    }

    public async deriveDefinitionConfiguration(key: string,
                                               version: number,
                                               authoredElementValues: AuthoredElementValues,
                                               derivationOptions: ElementDerivationOptions,
                                               abort?: AbortSignal): Promise<DerivedRuntimeElementData> {
        return await this.post<unknown, DerivedRuntimeElementData>(`/api/payment-provider-definitions/${key}/${version}/derive/`, {
            authoredElementValues,
            derivationOptions,
        }, {
            abort,
        });
    }

    public initialize(): PaymentProviderResponseDTO {
        return {
            key: '',
            providerKey: '',
            providerVersion: 0,
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
        return await this.post<any, PaymentProviderTestDataResponseDTO>(`/api/payment-providers/${key}/test/`, request, {});
    }
}
