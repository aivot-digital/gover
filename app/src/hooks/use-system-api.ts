import {Api} from './use-api';
import {HealthData} from "../models/dtos/health-data";
import {HttpExchanges} from '../models/dtos/http-exchange';

interface SystemApi {
    testSmtp(email: string): Promise<{ result?: string }>;

    getFileExtensions(): Promise<string[]>;

    getHealth(): Promise<HealthData>;

    getHttpExchanges(): Promise<HttpExchanges>;
}

export function useSystemApi(api: Api): SystemApi {
    const testSmtp = async (email: string) => {
        return await api.post<{ result?: string }>('mail/test/', {
            targetMail: email,
        });
    };

    const getFileExtensions = async () => {
        return await api.get<string[]>('system/file-extensions');
    };

    const getHealth = async () => {
        return await api.get<HealthData>('public/actuator/health');
    };

    const getHttpExchanges = async () => {
        return await api.get<HttpExchanges>('public/actuator/httpexchanges');
    };

    return {
        testSmtp,
        getFileExtensions,
        getHealth,
        getHttpExchanges,
    };
}
