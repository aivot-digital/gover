import {createContext, useContext} from 'react';
import {AnyElement} from '../../../models/elements/any-element';

export interface ElementTreeInlineEditorContextType {
    navigateToElementEditor: (element: AnyElement, tab?: string | null) => void;
    cloneElement: (element: AnyElement) => void;
    deleteElement: (element: AnyElement) => void;
    editable: boolean;
}

export const ElementTreeInlineEditorContext = createContext<ElementTreeInlineEditorContextType | null>(null);

export const ElementTreeInlineEditorContextProvider = ElementTreeInlineEditorContext.Provider;

export function useElementTreeInlineEditorContext(): ElementTreeInlineEditorContextType | null {
    const context = useContext(ElementTreeInlineEditorContext);
    if (context == null) {
        return null;
    }
    return context;
}
