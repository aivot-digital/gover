import {createContext, useContext} from 'react';
import {ProcessNodeEntity} from "../../../../entities/process-node-entity";
import {ProcessNodeProvider} from "../../../../services/process-node-provider-api-service";
import {GroupLayout} from "../../../../../../models/elements/form/layout/group-layout";

type ProcessNodeEditorContextType = {
    provider: ProcessNodeProvider;
    layout: GroupLayout;

    node: ProcessNodeEntity;
    setNode: (node: ProcessNodeEntity) => void;

    isEditable: boolean;
};

const ProcessNodeEditorContext = createContext<ProcessNodeEditorContextType | null>(null);

export const ProcessNodeEditorProvider = ProcessNodeEditorContext.Provider;

export function useProcessNodeEditorContext(){
    const context = useContext(ProcessNodeEditorContext);
    if (context == null) {
        throw new Error('useProcessNodeEditorContext must be used within a ProcessNodeEditorProvider');
    }
    return context;
}
