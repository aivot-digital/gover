export interface ProcessDefinitionEdgeEntity {
    id: number;
    processDefinitionId: number;
    processDefinitionVersion: number;
    fromNodeId: number;
    toNodeId: number;
    viaPort: string;
}