import {createContext, useContext} from 'react';
import {type ProcessNodeEntity} from '../../../../entities/process-node-entity';
import type {ProcessInstanceEntity} from '../../../../entities/process-instance-entity';
import type {ProcessInstanceTaskEntity} from '../../../../entities/process-instance-task-entity';
import type {ProcessInstanceEventEntity} from '../../../../entities/process-instance-event-entity';
import type {ProcessInstanceAttachmentEntity} from '../../../../entities/process-instance-attachment-entity';
import type {ProcessInstanceAttachmentSetEntity} from '../../../../entities/process-instance-attachment-set-entity';

import {ProcessNodeProblems} from '../../../../entities/process-node-problems';
import {type ProcessNodeProvider} from '../../../../services/process-node-provider-api-service';

export interface ProcessFlowEditorContextType {
    editable: boolean;
    showTargetHandles: boolean;

    selectedNode: ProcessNodeEntity | null;
    onSelectNode: (node: ProcessNodeEntity | null) => void;

    onAddEdge: (fromNodeId: number, toNodeId: number, viaPortKey: string) => void;
    onDeleteEdge: (forEdgeId: number) => void;
    onDeleteNode: (node: ProcessNodeEntity) => void | Promise<void>;
    onConnectNodeToExisting: (node: ProcessNodeEntity, preferredPortKey?: string) => void;
    onStartReplaceNode: (node: ProcessNodeEntity) => void;
    onStartCloneNode: (node: ProcessNodeEntity) => void;
    onShowNodeProviderDetails: (provider: ProcessNodeProvider) => void;
    onShowTaskEvents: (taskId: number) => void;

    onReloadRuntimeData: () => void;
    onDownloadAttachment?: (attachment: ProcessInstanceAttachmentEntity) => void | Promise<void>;

    onAddFollowUpNode: (fromNodeId: number, viaPortKey: string) => void;
    onAddInbetweenNode: (forEdgeId: number) => void;

    runtimeData: {
        instance: ProcessInstanceEntity;
        tasks: ProcessInstanceTaskEntity[];
        events: ProcessInstanceEventEntity[];
        attachments: ProcessInstanceAttachmentEntity[];
        attachmentSets: ProcessInstanceAttachmentSetEntity[];
    } | null;

    nodeProblems: ProcessNodeProblems[];
    showNodeProblemsForNodes: Record<number, boolean>;
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
