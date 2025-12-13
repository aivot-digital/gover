export interface ProcessDefinitionNodeEntity {
    id: number;
    processDefinitionId: number;
    processDefinitionVersion: number;
    name: string | null;
    description: string | null;
    dataKey: string;
    codeKey: string;
    configuration: Record<string, any>;
}