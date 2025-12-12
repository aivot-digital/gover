import {ProcessInstanceStatus} from "../enums/process-instance-status";

export interface ProcessInstanceEntity {
    id: number;
    accessKey: string; // UUID as string
    processDefinitionId: number;
    processDefinitionVersion: number;
    status: ProcessInstanceStatus; // Should match ProcessInstanceStatus enum
    statusOverride: string | null;
    assignedFileNumbers: string[];
    deliveryChannels: DeliveryChannelConfig[];
    tags: string[];
    started: string; // ISO date string
    updated: string; // ISO date string
    finished: string | null; // ISO date string
    runtime: string | null; // ISO 8601 duration string
    initialPayload: Record<string, any>;
    initialNodeId: number;
}

export interface DeliveryChannelConfig {

}