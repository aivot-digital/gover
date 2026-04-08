import React, {createContext} from 'react';
import {type ProcessNodeEntity} from '../../entities/process-node-entity';

import {ProcessNodeProblems} from '../../entities/process-node-problems';

export interface ProcessDetailsPageContextType {
    editable: boolean;
    onSave: (node: ProcessNodeEntity) => Promise<void>;
    onDelete: (node: ProcessNodeEntity) => Promise<void>;
    onStartReplaceNode: (node: ProcessNodeEntity) => void;
    nodeRefreshSignal: {
        nodeId: number | null;
        version: number;
    };
    nodeValidationResults: ProcessNodeProblems[];
}

export const ProcessDetailsPageContext = createContext<ProcessDetailsPageContextType | null>(null);

export const ProcessDetailsPageProvider = ProcessDetailsPageContext.Provider;

export function useProcessDetailsPageContext(): ProcessDetailsPageContextType {
    const context = React.useContext(ProcessDetailsPageContext);
    if (context == null) {
        throw new Error('useProcessDetailsPageContext must be used within a ProcessDetailsPageProvider');
    }
    return context;
}
