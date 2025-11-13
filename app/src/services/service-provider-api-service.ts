import {ServiceProviderDTO} from '../models/dtos/service-provider-dto';
import {BaseApiService} from './base-api-service';

export class ServiceProviderApiService extends BaseApiService {
    public async getServiceProviders(): Promise<ServiceProviderDTO[]> {
        return this.get('/api/service-providers');
    }
}