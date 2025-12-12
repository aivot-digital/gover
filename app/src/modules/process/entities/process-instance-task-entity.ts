import {ProcessTaskStatus} from "../enums/process-task-status";

export interface ProcessInstanceTaskEntity {
    id: number;
    accessKey: string; // UUID as string
    processInstanceId: number;
    processDefinitionId: number;
    processDefinitionVersion: number;
    processDefinitionNodeId: number;
    status: ProcessTaskStatus;
    statusOverride: string | null;
    started: string; // ISO date string
    updated: string; // ISO date string
    finished: string | null; // ISO date string
    runtime: string | null; // ISO 8601 duration string
    bufferData: Record<string, any>; // JSON as string, or consider Record<string, any>
    metaData: Record<string, any>; // JSON as string, or consider Record<string, any>
    workingData: Record<string, any>; // JSON as string, or consider Record<string, any>
    assignedUserId: string | null;
}