import {ServiceProviderDTO} from '../models/dtos/service-provider-dto';
import {BaseApiService} from './base-api-service';

export interface PluginDTO {
    name: string;
    description: string;
    buildDate: string;
    version: string;
    vendorName: string;
    components: {
        name: string;
        description: string;
    }[];
}

export class PluginsApiService extends BaseApiService {
    public async getPlugins(): Promise<PluginDTO[]> {
        return this.get<PluginDTO[]>('/api/plugins/');
    }
}