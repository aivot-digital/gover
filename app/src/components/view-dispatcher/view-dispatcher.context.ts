import {createContext, RefObject, useContext} from 'react';
import type {AnyElement} from '../../models/elements/any-element';
import type {AuthoredElementValues, DerivedRuntimeElementData} from '../../models/element-data';
import {type InputVariableSuggestion} from '../../models/input-mode';
import {findReplicatingContainerId} from '../../utils/flatten-elements';

export enum ViewDispatcherMode {
    Editor,
    Viewer,
}

export type TaskViewMode = 'staff' | 'customer';

/** The values of a replicating container row around the rendered elements. */
export interface ViewDispatcherRowScope {
    containerId: string;
    authoredElementValues: AuthoredElementValues;
    derivedData: DerivedRuntimeElementData;
}

export interface ViewDispatcherContextType {
    scrollContainerRef?: RefObject<HTMLDivElement | null>;
    mode: ViewDispatcherMode;
    showInvisibleElements?: boolean;
    taskViewMode?: TaskViewMode | null;
    readOnly?: boolean;
    showErrorSummary?: boolean;

    rootElement: AnyElement;
    allElements: AnyElement[];

    rootAuthoredElementValues: AuthoredElementValues;
    rootDerivedData: DerivedRuntimeElementData;
    /** The replicating container rows around the rendered elements, from the outermost to the innermost row. */
    rowScopes?: ViewDispatcherRowScope[];
    highlightedElementId?: string | null;
    inputModesEnabled?: boolean;
    inputModeVariables?: InputVariableSuggestion[];
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

/**
 * Resolves the values of the row, surrounding row or form level that contains the element. As in the backend
 * derivation, an element in the rows of another replicating container has no values for the rendered elements.
 */
export function resolveViewDispatcherValueScope(
    context: ViewDispatcherContextType,
    elementId: string,
): Pick<ViewDispatcherRowScope, 'authoredElementValues' | 'derivedData'> | undefined {
    const containerId = findReplicatingContainerId(context.rootElement, elementId);
    if (containerId === undefined) {
        return undefined;
    }

    if (containerId === null) {
        return {
            authoredElementValues: context.rootAuthoredElementValues,
            derivedData: context.rootDerivedData,
        };
    }

    return context.rowScopes?.find((scope) => scope.containerId === containerId);
}
