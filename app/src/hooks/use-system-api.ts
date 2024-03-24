import {Api} from './use-api';
import {HealthData} from "../models/dtos/health-data";

interface SystemApi {
    testSmtp(email: string): Promise<{ result?: string }>;

    getFileExtensions(): Promise<string[]>;

    getHealth(): Promise<HealthData>;
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

    const getHealth = async () => {
        return await api.get<HealthData>('public/actuator/health');
    };

    return {
        testSmtp,
        getFileExtensions,
        getHealth,
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