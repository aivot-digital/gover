import {Api} from './use-api';
import {SystemConfig} from '../models/entities/system-config';

interface SystemConfigsApi {
    list(): Promise<SystemConfig[]>;

    listPublic(): Promise<SystemConfig[]>;

    save(key: string, value: string): Promise<SystemConfig>;

    saveBoolean(key: string, value: boolean): Promise<SystemConfig>;
}

export function useSystemConfigsApi(api: Api): SystemConfigsApi {

    const list = async () => {
        return await api.get<SystemConfig[]>('system-configs');
    };

    const listPublic = async () => {
        return await api.getPublic<SystemConfig[]>('system-configs');
    };


    const save = async (key: string, value: string) => {
        return await api.put<SystemConfig>(`system-configs/${key}`, {
            key: key,
            value: value,
            publicConfig: true,
            created: new Date().toISOString(),
            updated: new Date().toISOString(),
        });
    };

    const saveBoolean = async (key: string, value: boolean) => {
        return await api.put<SystemConfig>(`system-configs/${key}`, {
            key: key,
            value: value ? 'true' : 'false',
            publicConfig: true,
            created: new Date().toISOString(),
            updated: new Date().toISOString(),
        });
    };

    return {
        list,
        listPublic,
        save,
        saveBoolean,
    };
}
