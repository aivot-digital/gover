import {Api} from './use-api';
import {HealthData} from "../models/dtos/health-data";
import {HttpExchange, HttpExchanges} from '../models/dtos/http-exchange';

interface SystemApi {
    testSmtp(email: string): Promise<{ result?: string }>;

    getFileExtensions(): Promise<string[]>;

    getPaymentProviders(): Promise<{id: string, label: string}[]>;

    getHealth(): Promise<HealthData>;

    getHttpExchanges(): Promise<HttpExchanges>;
}

export function useSystemApi(api: Api): SystemApi {
    const testSmtp = async (email: string) => {
        return await api.post<{ result?: string }>('system/test-smtp', {
            targetMail: email,
        });
    };

    const getFileExtensions = async () => {
        return await api.get<string[]>('system/file-extensions');
    };

    const getPaymentProviders = async () => {
        return await api.get<{id: string, label: string}[]>('system/payment-providers');
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
        getPaymentProviders,
        getHealth,
        getHttpExchanges,
    };
}

export async function getSentryDsn(): Promise<string | undefined> {
    const response = await fetch('/api/public/system/sentry-dsn', {
        method: 'GET',
        headers: {
            'Accept': 'application/json',
        },
    });
    const dsn = await response.json();
    if (dsn.length < 1 || dsn[0].trim().length === 0) {
        return undefined;
    }
    return dsn[0].trim();
}