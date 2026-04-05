import {AuthoredElementValues} from '../../../models/element-data';

export interface ProcessNodeEntity {
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
    requirements: string | null;
    notes: string | null;
}
