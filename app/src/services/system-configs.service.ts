import {CrudService} from './crud.service';
import {SystemConfig} from '../models/entities/system-config';
import {ListApplication} from "../models/entities/list-application";
import axios, {AxiosResponse} from "axios";
import {ApiConfig} from "../api-config";

class _SystemConfigsService extends CrudService<SystemConfig, 'systemConfigs', string> {
    constructor() {
        super('system-configs');
    }

    async listPublicSystemConfigs(): Promise<SystemConfig[]> {
        const response: AxiosResponse = await axios.get(ApiConfig.address + '/public/system-configs', CrudService.getConfig());
        return response.data;
    }
}

export const SystemConfigsService = new _SystemConfigsService();
