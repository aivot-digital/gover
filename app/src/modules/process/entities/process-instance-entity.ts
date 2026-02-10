import {type ProcessInstanceStatus} from '../enums/process-instance-status';

export interface ProcessInstanceEntity {
    id: number;
    accessKey: string; // UUID as string
    processId: number;
    initialProcessVersion: number;
    status: ProcessInstanceStatus; // Should match ProcessInstanceStatus enum
    statusOverride: string | null;
    assignedUserId: string | null;
    assignedFileNumbers: string[];
    identities: DeliveryChannelConfig[];
    started: string; // ISO date string
    updated: string; // ISO date string
    finished: string | null; // ISO date string
    runtime: string | null; // ISO 8601 duration string
    initialPayload: Record<string, any>;
    initialNodeId: number;
    keepUntil: string | null; // ISO date string
    createdForTestClaimId: number | null;
}

export interface DeliveryChannelConfig {

}
