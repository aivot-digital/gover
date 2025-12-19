import {createContext} from 'react';
import {ProcessDefinitionNodeEntity} from "../../entities/process-definition-node-entity";

export interface ProcessDetailsPageContext {
    editable: boolean;
    onSave: (node: ProcessDefinitionNodeEntity) => void;
    onDelete: (node: ProcessDefinitionNodeEntity) => void;
}

export const ProcessDetailsPageContext = createContext<ProcessDetailsPageContext>({
    editable: false,
    onSave: () => {},
    onDelete: () => {}
});
