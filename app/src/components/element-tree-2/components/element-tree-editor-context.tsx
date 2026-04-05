import {createContext, useContext} from 'react';
import {AnyElement} from '../../../models/elements/any-element';
import {ElementWithParents} from '../../../utils/flatten-elements';

export interface ElementTreeEditorContextType<T extends AnyElement> {
    currentElement: T;
    onChangeCurrentElement: (element: T) => void;
    parents: AnyElement[];
}

export const ElementTreeEditorContext = createContext<ElementTreeEditorContextType<any> | null>(null);

export const ElementTreeEditorContextProvider = ElementTreeEditorContext.Provider;

export function useElementTreeEditorContext<T extends AnyElement>(): ElementTreeEditorContextType<T> {
    const context = useContext(ElementTreeEditorContext);
    if (context == null) {
        throw new Error('useElementTreeEditorContext must be used within a ElementTreeEditorContextProvider');
    }
    return context;
}
