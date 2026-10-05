export interface ProcessDefinitionEdgeEntity {
    id: number;
    processId: number;
    processVersion: number;
    fromNodeId: number;
    toNodeId: number;
    viaPort: string;
}