export type Status = 'UP' | 'DOWN' | 'UNKNOWN';

interface BaseComponent<T> {
    status: Status;
    details?: T;
}

export interface HealthData {
    status: Status;
    components?: HealthDataComponents;
}

export interface HealthDataComponents {
    av: BaseComponent<{
        error?: string;
    }>;
    db: BaseComponent<{
        database: string;
        validationQuery: string;
    }>;
    diskSpace: BaseComponent<{
        total: number;
        free: number;
        threshold: number;
        path: string;
        exists: boolean;
    }>;
    gotenberg: BaseComponent<{
        error?: string;
    }>;
    mail: BaseComponent<{
        location: string;
    }>;
    rabbit: BaseComponent<{
        version: string;
    }>;
    ssl: BaseComponent<{
        validChains: Array<any>;
        invalidChains: Array<any>;
    }>;
    redis: BaseComponent<{
        version: string;
    }>;
    storage: BaseComponent<{
        hints?: string[];
        errors?: string[];
    }>;
}
