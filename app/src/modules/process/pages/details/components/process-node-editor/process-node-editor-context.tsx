import React, {createContext, useContext} from 'react';
import {type ProcessNodeEntity} from '../../../../entities/process-node-entity';
import {type ProcessNodeProvider} from '../../../../services/process-node-provider-api-service';
import {type GroupLayout} from '../../../../../../models/elements/form/layout/group-layout';

interface ProcessNodeEditorContextType {
    provider: ProcessNodeProvider;
    layout: GroupLayout;

    node: ProcessNodeEntity;
    setNode: (node: ProcessNodeEntity) => void;

    isEditable: boolean;
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
