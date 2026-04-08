import {createContext, useContext} from 'react';
import {type ProcessNodeEntity} from '../../../../entities/process-node-entity';
import type {ProcessInstanceEntity} from '../../../../entities/process-instance-entity';
import type {ProcessInstanceTaskEntity} from '../../../../entities/process-instance-task-entity';
import type {ProcessInstanceEventEntity} from '../../../../entities/process-instance-event-entity';

import {ProcessNodeProblems} from '../../../../entities/process-node-problems';

export interface ProcessFlowEditorContextType {
    editable: boolean;
    showTargetHandles: boolean;

    selectedNode: ProcessNodeEntity | null;

    onAddEdge: (fromNodeId: number, toNodeId: number, viaPortKey: string) => void;
    onDeleteEdge: (forEdgeId: number) => void;
    onDeleteNode: (node: ProcessNodeEntity) => void | Promise<void>;
    onConnectNodeToExisting: (node: ProcessNodeEntity, preferredPortKey?: string) => void;
    onStartReplaceNode: (node: ProcessNodeEntity) => void;

    onReloadRuntimeData: () => void;

    onAddFollowUpNode: (fromNodeId: number, viaPortKey: string) => void;
    onAddInbetweenNode: (forEdgeId: number) => void;

    runtimeData: {
        instance: ProcessInstanceEntity;
        tasks: ProcessInstanceTaskEntity[];
        events: ProcessInstanceEventEntity[];
    } | null;

    nodeValidationResults: ProcessNodeProblems[];
}

export const ProcessFlowEditorContext = createContext<ProcessFlowEditorContextType | null>(null);

export const ProcessFlowEditorProvider = ProcessFlowEditorContext.Provider;

export function useProcessFlowEditorContext(): ProcessFlowEditorContextType {
    const context = useContext(ProcessFlowEditorContext);
    if (context == null) {
        throw new Error('useProcessFlowEditorContext must be used within a ProcessFlowEditorProvider');
    }
    return context;
}
