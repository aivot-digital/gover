import {BaseApiService} from "../../../services/base-api-service";

export enum ProcessNodeType {
    Trigger = 'Trigger',
    Action = 'Action',
    FlowControl = 'FlowControl',
    Termination = 'Termination',
}

export interface ProcessNodeProvider {
    key: string;
    version: number;
    type: ProcessNodeType;
    name: string;
    description: string;
    ports: ProcessNodePort[];
}

export interface ProcessNodePort {
    key: string;
    label: string;
    description: string;
}

export class ProcessNodeProviderApiService extends BaseApiService {
    public getNodeProviders(): Promise<ProcessNodeProvider[]> {
        return this.get('/api/process-node-definitions/');
    }
}