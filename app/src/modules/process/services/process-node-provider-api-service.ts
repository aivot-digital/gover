import {BaseApiService} from "../../../services/base-api-service";
import {SvgIconComponent} from "../../../types/svg-icon-component";
import Automation from "@aivot/mui-material-symbols-400-n25-outlined/Automation";
import ApprovalDelegation from "@aivot/mui-material-symbols-400-n25-outlined/ApprovalDelegation";
import ApprovalDelegationOff from "@aivot/mui-material-symbols-400-n25-outlined/ApprovalDelegationOff";
import AllMatch from "@aivot/mui-material-symbols-400-n25-outlined/AllMatch";

export enum ProcessNodeType {
    Trigger = 'Trigger',
    Action = 'Action',
    FlowControl = 'FlowControl',
    Termination = 'Termination',
}

export enum ProcessNodeExecutionType {
    Automatic = 'Automatic',
    Manual = 'Manual',
    SemiAutomatic = 'SemiAutomatic',
}

export const ProcessNodeExecutionTypeLabels: Record<ProcessNodeExecutionType, string> = {
    [ProcessNodeExecutionType.Automatic]: 'Automatisiert',
    [ProcessNodeExecutionType.Manual]: 'Manuell',
    [ProcessNodeExecutionType.SemiAutomatic]: 'Teilautomatisiert',
};

export const ProcessNodeExecutionTypeColors: Record<ProcessNodeExecutionType, string> = {
    [ProcessNodeExecutionType.Automatic]: 'secondary',
    [ProcessNodeExecutionType.Manual]: 'secondary',
    [ProcessNodeExecutionType.SemiAutomatic]: 'secondary',
};

export const ProcessNodeExecutionTypeIcons: Record<ProcessNodeExecutionType, SvgIconComponent> = {
    [ProcessNodeExecutionType.Automatic]: AllMatch,
    [ProcessNodeExecutionType.Manual]: ApprovalDelegation,
    [ProcessNodeExecutionType.SemiAutomatic]: ApprovalDelegationOff,
};

export interface ProcessNodeProvider {
    key: string;
    componentKey: string;
    componentType: string;
    componentVersion: string;
    deprecationNotice: string | null;
    majorVersion: number;
    type: ProcessNodeType;
    executionTypes: ProcessNodeExecutionType[];
    name: string;
    abstractDescription: string;
    description: string;
    documentationUrl: string | null;
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
    typeDefinition: string;
}

export class ProcessNodeProviderApiService extends BaseApiService {
    public getNodeProviders(): Promise<ProcessNodeProvider[]> {
        return this.get('/api/process-node-definitions/');
    }

    public getNodeProvider(key: string, version: number): Promise<ProcessNodeProvider> {
        return this.get(`/api/process-node-definitions/${key}/versions/${version}/`);
    }
}
