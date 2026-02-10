export enum ProcessNodeExecutionLogLevel {
    Debug = 'Debug',
    Info = 'Info',
    Warn = 'Warn',
    Error = 'Error',
}

export interface ProcessInstanceEventEntity {
    id: number;
    processInstanceId: number;
    processInstanceTaskId?: number | null;
    level: ProcessNodeExecutionLogLevel;
    isTechnical: boolean;
    isAudit: boolean;
    title: string;
    message: string;
    details: Record<string, any>;
    timestamp: string; // ISO date string
    triggeringUserId?: string | null;
}

