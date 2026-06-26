import {ProcessTaskStatus} from "../enums/process-task-status";

export interface ProcessInstanceTaskEntity {
    id: number;
    accessKey: string; // UUID as string
    processInstanceId: number;
    processId: number;
    processVersion: number;
    processNodeId: number;
    previousProcessInstanceTaskId: number | null;
    previousProcessNodeId: number | null;
    previousProcessNodePortKey: string | null;
    status: ProcessTaskStatus;
    statusOverride: string | null;
    started: string; // ISO date string
    updated: string; // ISO date string
    finished: string | null; // ISO date string
    runtime: string | null; // ISO 8601 duration string
    runtimeData: Record<string, any>;
    nodeData: Record<string, any>;
    processData: Record<string, any>;
    processDataDiff: Record<string, any>;
    assignedUserId: string | null;
    deadline: string | null; // ISO date string
}
