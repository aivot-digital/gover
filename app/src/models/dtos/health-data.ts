export type Status = 'UP' | 'DOWN' | 'UNKNOWN';

export interface AvComponent {
    status: Status;
}

export interface DbComponent {
    status: Status;
}

export interface DiskSpaceComponent {
    status: Status;
    details: {
        total: number;
        free: number;
        threshold: number;
        exists: boolean;
    };
}

export interface MailComponent {
    status: Status;
    details: {
        location: string;
    };
}

export interface S3Component {
    status: Status;
    details?: {
        hint?: string;
        error?: string;
    };
}

export interface PuppetComponent {
    status: Status;
    details?: {
        error?: string;
    };
}

export interface HealthData {
    status: Status;
    components?: HealthDataComponents;
}

export interface RedisComponent {
    status: Status;
}


export interface HealthDataComponents {
    av: AvComponent;
    db: DbComponent;
    diskSpace: DiskSpaceComponent;
    mail: MailComponent;
    s3: S3Component;
    puppet: PuppetComponent;
    redis: RedisComponent;
}
