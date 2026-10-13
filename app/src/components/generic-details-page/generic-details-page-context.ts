import {createContext, useContext} from 'react';
import {type GenericDetailsPageSubscribeEvent} from './generic-details-page-events';

export interface GenericDetailsPageContextType<ItemType, AdditionalData, Events extends object = {}> {
    item?: ItemType;
    setItem: (item: ItemType | ((item: ItemType) => ItemType)) => void;
    isNewItem?: boolean;
    isExistingItem?: boolean;
    additionalData?: AdditionalData;
    setAdditionalData: (additionalData: AdditionalData) => void;
    isBusy: boolean;
    setIsBusy: (isBusy: boolean) => void;
    refresh: () => void;
    /** Subscribes to future events and returns an unsubscribe function. */
    subscribeEvent: GenericDetailsPageSubscribeEvent<Events>;
    isEditable: boolean;
}

export const GenericDetailsPageContext = createContext<GenericDetailsPageContextType<any, any>>({
    setItem: () => {},
    setAdditionalData: () => {},
    isBusy: false,
    setIsBusy: () => {},
    refresh: () => {},
    subscribeEvent: () => () => {},
    isEditable: false,
});

export const GenericDetailsPageProvider = GenericDetailsPageContext.Provider;

export function useGenericDetailsPageContext<T, A, Events extends object = {}>(): GenericDetailsPageContextType<T, A, Events> {
    const context = useContext(GenericDetailsPageContext);
    if (context == null) {
        throw new Error('useGenericDetailsPageContext must be used within a GenericDetailsPageProvider');
    }
    return context as GenericDetailsPageContextType<T, A, Events>;
}
