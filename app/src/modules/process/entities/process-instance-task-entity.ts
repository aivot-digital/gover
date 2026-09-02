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
    runtime: number | null; // Milliseconds
    runtimeData: Record<string, any>;
    nodeData: Record<string, any>;
    processData: Record<string, any>;
    // TODO: Review whether this custom reverse-diff format and its database generation are still needed. The
    // process-data UI now compares complete task snapshots because this representation is ambiguous for some changes.
    processDataDiff: Record<string, any>;
    assignedUserId: string | null;
    deadline: string | null; // ISO date string
}
