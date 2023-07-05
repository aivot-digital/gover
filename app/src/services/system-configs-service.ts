import {SystemConfig} from '../models/entities/system-config';
import {ApiService} from "./api-service";

class _SystemConfigsService extends ApiService<SystemConfig, SystemConfig, string> {
    constructor() {
        super('system-configs');
    }

    public async listPublicSystemConfigs(): Promise<SystemConfig[]> {
        return await ApiService.get('public/system-configs');
    }
}

export const SystemConfigsService = new _SystemConfigsService();
