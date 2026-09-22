import {ProcessInstanceStatus} from '../enums/process-instance-status';
import {ProcessTaskStatus} from '../enums/process-task-status';

interface ProcessListEntry {
    id: number;
    caseNumber: string;
    assignedFileNumbers: string[];
    processId: number;
    processVersion: number;
    processName: string;
    assignedUserId: string | null;
    assignedUserName: string | null;
    statusOverride: string | null;
    started: string;
    finished: string | null;
    test: boolean;
}
export interface ProcessInstanceListEntry extends ProcessListEntry {
    status: ProcessInstanceStatus;
}
export interface ProcessTaskListEntry extends ProcessListEntry {
    processInstanceId: number;
    taskName: string;
    taskType: string | null;
    description: string | null;
    status: ProcessTaskStatus;
    deadline: string | null;
}
export interface ProcessListOptions {
    processes: {value: string; label: string}[];
    assignees: {value: string; label: string}[];
}
export interface ProcessListFilter {
    search?: string;
    view?: string;
    processId?: number;
    processVersion?: number;
    instanceId?: number;
    assignee?: string;
}
