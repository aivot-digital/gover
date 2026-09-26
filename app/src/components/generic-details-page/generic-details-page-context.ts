import {createContext, useContext} from 'react';

export interface GenericDetailsPageContextType<ItemType, AdditionalData> {
    item?: ItemType;
    setItem: (item: ItemType | ((item: ItemType) => ItemType)) => void;
    isNewItem?: boolean;
    isExistingItem?: boolean;
    additionalData?: AdditionalData;
    setAdditionalData: (additionalData: AdditionalData) => void;
    isBusy: boolean;
    setIsBusy: (isBusy: boolean) => void;
    refresh: () => void;
    isEditable: boolean;
}

export const GenericDetailsPageContext = createContext<GenericDetailsPageContextType<any, any>>({
    setItem: () => {},
    setAdditionalData: () => {},
    isBusy: false,
    setIsBusy: () => {},
    refresh: () => {},
    isEditable: false,
});

export const GenericDetailsPageProvider = GenericDetailsPageContext.Provider;

export function useGenericDetailsPageContext<T, A>(): GenericDetailsPageContextType<T, A> {
    const context = useContext(GenericDetailsPageContext);
    if (context == null) {
        throw new Error('useGenericDetailsPageContext must be used within a GenericDetailsPageProvider');
    }
    return context;
}
