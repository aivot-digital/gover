import {CrudService} from './crud.service';
import {SystemConfig} from '../models/system-config';

export const SystemConfigsService = new CrudService<SystemConfig, 'systemConfigs', string>('system-configs');
