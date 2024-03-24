type Status = 'UP' | 'DOWN';

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

export interface HealthData {
    status: Status;
    components?: {
        av: AvComponent;
        db: DbComponent;
        diskSpace: DiskSpaceComponent;
        mail: MailComponent;
    };
}
