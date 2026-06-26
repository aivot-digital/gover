import {BaseApiService} from "../../../services/base-api-service";

export enum ProcessNodeType {
    Trigger = 'Trigger',
    Action = 'Action',
    FlowControl = 'FlowControl',
    Termination = 'Termination',
}

export interface ProcessNodeProvider {
    key: string;
    componentKey: string;
    componentType: string;
    componentVersion: string;
    deprecationNotice: string | null;
    majorVersion: number;
    type: ProcessNodeType;
    name: string;
    description: string;
    parentPluginKey: string;
    ports: ProcessNodePort[];
    outputs: ProcessNodeOutput[];
}

export interface ProcessNodePort {
    key: string;
    label: string;
    description: string;
}

export interface ProcessNodeOutput {
    key: string;
    label: string;
    description: string;
}

export class ProcessNodeProviderApiService extends BaseApiService {
    public getNodeProviders(): Promise<ProcessNodeProvider[]> {
        return this.get('/api/process-node-definitions/');
    }

    public getNodeProvider(key: string, version: number): Promise<ProcessNodeProvider> {
        return this.get(`/api/process-node-definitions/${key}/versions/${version}/`);
    }
}