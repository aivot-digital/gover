export interface ProcessDefinitionNodeEntity {
    id: number;
    processDefinitionId: number;
    processDefinitionVersion: number;
    dataKey: string;
    codeKey: string;
    configuration: Record<string, any>;
}