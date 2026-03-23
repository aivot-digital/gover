import {ProcessHistoryEventType} from "../enums/process-history-event-type";

export interface ProcessInstanceHistoryEventEntity {
    id: number;
    type: ProcessHistoryEventType;
    title: string;
    message: string;
    details: Record<string, unknown>;
    timestamp: string; // ISO date string
    triggeringUserId: string | null;
    processInstanceId: number;
    processInstanceTaskId: number | null;
}

