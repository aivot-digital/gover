import {createContext} from 'react';

export type GenericDetailsPageContextType<ItemType, AdditionalData> = {
    item?: ItemType;
    setItem: (item: ItemType) => void;
    isNewItem?: boolean;
    isExistingItem?: boolean;
    additionalData?: AdditionalData;
    setAdditionalData: (additionalData: AdditionalData) => void;
    isBusy: boolean;
    setIsBusy: (isBusy: boolean) => void;
};

export const GenericDetailsPageContext = createContext<GenericDetailsPageContextType<any, any>>({
    item: undefined,
    setItem: () => {
    },
    isNewItem: undefined,
    isExistingItem: undefined,
    additionalData: undefined,
    setAdditionalData: () => {
    },
    isBusy: false,
    setIsBusy: () => {
    },
});
