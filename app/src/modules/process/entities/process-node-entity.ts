import {type ProcessNodePort} from '../services/process-node-provider-api-service';
import {AuthoredElementValues} from '../../../models/element-data';

export interface ProcessNodeEntity {
    readonly ports: ProcessNodePort[];
    readonly portResolutionProblems: string[];
    id: number;
    processId: number;
    processVersion: number;
    processNodeDefinitionKey: string;
    processNodeDefinitionVersion: number;
    name: string | null;
    description: string | null;
    dataKey: string;
    configuration: AuthoredElementValues;
    outputMappings: Record<string, any>;
    timeLimitDays: number | null;
    timeLimitStrict: boolean;
    requirements: string | null;
    notes: string | null;
    savedWithErrors: boolean;
    created: string;
    updated: string;
}

export type ProcessNodeWrite = Omit<ProcessNodeEntity, 'ports' | 'portResolutionProblems'>;
