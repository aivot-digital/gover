import {createContext, useContext} from 'react';
import {type ProcessNodeEntity} from '../../../../entities/process-node-entity';
import {type ProcessNodeProvider} from '../../../../services/process-node-provider-api-service';
import {type GroupLayout} from '../../../../../../models/elements/form/layout/group-layout';
import {type ProcessTestClaimEntity} from '../../../../entities/process-test-claim-entity';
import {type ProcessNodeProblems} from '../../../../entities/process-node-problems';
import {type ProcessDataKeyHintResponse} from '../../../../entities/process-data-key-hint-response';

interface ProcessNodeEditorContextType {
    provider: ProcessNodeProvider;
    layout: GroupLayout;
    testClaim: ProcessTestClaimEntity | null;

    node: ProcessNodeEntity;
    setNode: (node: ProcessNodeEntity, updateOriginal: boolean) => void;

    isEditable: boolean;

    problems: ProcessNodeProblems | null;

    processDataKeyHints: ProcessDataKeyHintResponse[] | null;
}

const ProcessNodeEditorContext = createContext<ProcessNodeEditorContextType | null>(null);

export const ProcessNodeEditorProvider = ProcessNodeEditorContext.Provider;

export function useProcessNodeEditorContext(): ProcessNodeEditorContextType {
    const context = useContext(ProcessNodeEditorContext);
    if (context == null) {
        throw new Error('useProcessNodeEditorContext must be used within a ProcessNodeEditorProvider');
    }
    return context;
}

export function useOptionalProcessNodeEditorContext(): ProcessNodeEditorContextType | null {
    const context = useContext(ProcessNodeEditorContext);
    if (context == null) {
        return null;
    }
    return context;
}
