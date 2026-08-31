import {type ProcessInstanceStatus} from '../enums/process-instance-status';
import {type IdentityDataMap} from '../../identity/models/identity-data';

export interface ProcessInstanceEntity {
    id: number;
    caseNumber: string;
    accessKey: string; // UUID as string
    processId: number;
    initialProcessVersion: number;
    status: ProcessInstanceStatus; // Should match ProcessInstanceStatus enum
    statusOverride: string | null;
    assignedUserId: string | null;
    assignedFileNumbers: string[];
    identities: IdentityDataMap;
    started: string; // ISO date string
    updated: string; // ISO date string
    finished: string | null; // ISO date string
    runtime: number | null; // Milliseconds
    initialPayload: Record<string, any>;
    initialNodeId: number;
    keepUntil: string | null; // ISO date string
    createdForTestClaimId: number | null;
}
