import {createContext, useContext} from 'react';
import {ElementType} from '../../data/element-type/element-type';
import {ElementWithParents} from '../../utils/flatten-elements';
import {AnyElement} from '../../models/elements/any-element';
import {ElementDisplayContext} from '../../data/element-type/element-child-options';

export interface ElementTreeDragItem {
    id: string;
    type: ElementType;
    path: string[];
    parentPath: string[];
}

export interface ElementTreeExpandCommand {
    type: 'expand-all' | 'collapse-all' | 'expand-to-path' | null;
    version: number;
    targetPath?: string[];
}

export const ELEMENT_TREE_DND_ITEM_TYPE = 'element-tree-item';

export interface ElementTreeContextType {
    root: AnyElement;
    editable: boolean;
    parentModalZIndex?: number;
    scrollToElement: (elementPath: string) => void;
    canDropElement: (dragItem: ElementTreeDragItem, targetParentPath: string[], targetIndex: number) => boolean;
    moveElement: (dragItem: ElementTreeDragItem, targetParentPath: string[], targetIndex: number) => void;
    expandCommand: ElementTreeExpandCommand;
    activeSearchResultPath?: string[];
    allElements: ElementWithParents[];
    displayContext: ElementDisplayContext;
    allowElementIdEditing: boolean;
}

export const ElementTreeContext = createContext<ElementTreeContextType | null>(null);

export const ElementTreeContextProvider = ElementTreeContext.Provider;

export function useElementTreeContext(): ElementTreeContextType {
    const context = useContext(ElementTreeContext);
    if (context == null) {
        throw new Error('useElementTreeContext must be used within a ElementTreeContextProvider');
    }
    return context;
}
