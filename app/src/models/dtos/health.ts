type Status = 'UP' | 'DOWN';

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

export interface Health {
    status: Status;
    components: {
        db: DbComponent;
        diskSpace: DiskSpaceComponent;
        mail: MailComponent;
    };
}
