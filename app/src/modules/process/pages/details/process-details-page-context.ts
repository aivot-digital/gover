import {createContext} from 'react';
import {ProcessNodeEntity} from "../../entities/process-node-entity";

export interface ProcessDetailsPageContext {
    editable: boolean;
    onSave: (node: ProcessNodeEntity) => void;
    onDelete: (node: ProcessNodeEntity) => void;
}

export const ProcessDetailsPageContext = createContext<ProcessDetailsPageContext>({
    editable: false,
    onSave: () => {},
    onDelete: () => {}
});
