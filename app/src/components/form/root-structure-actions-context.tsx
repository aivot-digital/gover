import {createContext, useContext} from 'react';

export interface RootStructureActionsContextType {
    canAddAtRoot: boolean;
    openAddAtRootDialog: () => void;
}

export const RootStructureActionsContext = createContext<RootStructureActionsContextType | null>(null);

export const RootStructureActionsContextProvider = RootStructureActionsContext.Provider;

export function useRootStructureActionsContext(): RootStructureActionsContextType | null {
    return useContext(RootStructureActionsContext);
}
