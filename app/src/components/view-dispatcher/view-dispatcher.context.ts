import {createContext, RefObject, useContext} from 'react';
import type {AnyElement} from '../../models/elements/any-element';
import type {AuthoredElementValues, DerivedRuntimeElementData} from '../../models/element-data';

export enum ViewDispatcherMode {
    Editor,
    Viewer,
}

export interface ViewDispatcherContextType {
    scrollContainerRef?: RefObject<HTMLDivElement | null>;
    mode: ViewDispatcherMode;
    showInvisibleElements?: boolean;

    rootElement: AnyElement;
    allElements: AnyElement[];

    rootAuthoredElementValues: AuthoredElementValues;
    rootDerivedData: DerivedRuntimeElementData;
}

export const ViewDispatcherContext = createContext<ViewDispatcherContextType | null>(null);

export const ViewDispatcherContextProvider = ViewDispatcherContext.Provider;

export function useViewDispatcherContext(): ViewDispatcherContextType {
    const context = useContext(ViewDispatcherContext);
    if (context == null) {
        throw new Error('useViewDispatcherContext must be an ViewDispatcherContextProvider');
    }
    return context;
}
