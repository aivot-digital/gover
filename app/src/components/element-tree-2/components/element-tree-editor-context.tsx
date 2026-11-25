import {LegacyNoCodeMigrations, LegacyNoCodeSection} from '../../../modules/nocode/hooks/use-legacy-no-code-migration';
import {createContext, useContext} from 'react';
import {AnyElement} from '../../../models/elements/any-element';

export interface ElementTreeEditorContextType<T extends AnyElement> {
    noCodeValidationIndex?: number;
    noCodeValidationRequest?: number;
    legacyMigrations?: LegacyNoCodeMigrations;
    startLegacyMigration?: (section: LegacyNoCodeSection) => void;
    cancelLegacyMigration?: (section: LegacyNoCodeSection) => void;
    currentElement: T;
    onChangeCurrentElement: (element: T, reason?: 'normalize') => void;
    parents: AnyElement[];
    originalElement?: T;
    dataKeyUnlocked?: boolean;
    onUnlockDataKey?: () => void;
    onLockDataKey?: () => void;
    dataKeyValidationRequested?: boolean;
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
