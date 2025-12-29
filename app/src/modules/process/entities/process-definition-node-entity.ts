export interface ProcessDefinitionNodeEntity {
    id: number;
    processId: number;
    processVersion: number;
    processNodeDefinitionKey: string;
    processNodeDefinitionVersion: number;
    name: string | null;
    description: string | null;
    dataKey: string;
    configuration: Record<string, any>;
}