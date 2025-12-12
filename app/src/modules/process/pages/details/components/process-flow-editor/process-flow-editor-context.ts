import {createContext} from 'react';
import {ProcessDefinitionNodeEntity} from "../../../../entities/process-definition-node-entity";

export interface ProcessFlowEditorContext {
    editable: boolean;
    showTargetHandles: boolean;

    selectedNode: ProcessDefinitionNodeEntity | null;
    onSelectedNode: (node: ProcessDefinitionNodeEntity | null) => void;

    onAddEdge: (fromNodeId: number, toNodeId: number, viaPortKey: string) => void;
    onDeleteEdge: (forEdgeId: number) => void;

    onAddFollowUpNode: (fromNodeId: number, viaPortKey: string) => void;
    onAddInbetweenNode: (forEdgeId: number) => void;
}

export const ProcessFlowEditorContext = createContext<ProcessFlowEditorContext>({
    editable: false,
    showTargetHandles: false,

    selectedNode: null,
    onSelectedNode: () => {},

    onAddEdge: () => {},
    onDeleteEdge: () => {},

    onAddFollowUpNode: () => {},
    onAddInbetweenNode: () => {},
});
