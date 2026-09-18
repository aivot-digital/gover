import {type Page} from '../../../models/dtos/page';
import {ProcessNodeExecutionLogLevel} from '../entities/process-instance-event-entity';

export interface ProcessInstanceEventLogContext {
    id: number;
    caseNumber: string;
    started: string;
    finished: string | null;
    runtime: number | null;
}

export interface ProcessInstanceEventLogTaskContext {
    id: number;
    name: string;
    started: string;
    finished: string | null;
    runtime: number | null;
}

export interface ProcessInstanceEventLogEntry {
    id: number;
    processInstanceId: number;
    processInstanceTaskId: number | null;
    level: ProcessNodeExecutionLogLevel;
    technical: boolean;
    audit: boolean;
    title: string;
    message: string;
    details: Record<string, unknown>;
    timestamp: string;
    triggeringUserId: string | null;
    triggeringUserName: string | null;
    processNodeName: string | null;
}

export interface ProcessInstanceEventLog {
    instance: ProcessInstanceEventLogContext;
    task: ProcessInstanceEventLogTaskContext | null;
    events: Page<ProcessInstanceEventLogEntry>;
}

export type ProcessInstanceEventLogFilter = 'all' | 'notable';
export type ProcessInstanceEventLogSortOrder = 'ASC' | 'DESC';
