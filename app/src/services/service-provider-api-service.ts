import {Api} from '../hooks/use-api';
import {NoCodeOperatorDetailsDTO} from '../models/dtos/no-code-operator-details-dto';
import {NoCodeDataType} from '../data/no-code-data-type';
import {ServiceProviderDTO} from '../models/dtos/service-provider-dto';

export class ServiceProviderApiService {
    private readonly api: Api;

    constructor(api: Api) {
        this.api = api;
    }

    public async getServiceProviders(): Promise<ServiceProviderDTO[]> {
        return this.api.get('service-providers');
    }
}