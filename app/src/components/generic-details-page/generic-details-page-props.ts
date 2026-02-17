import {BadgeProps} from '@mui/material';
import {Api} from '../../hooks/use-api';
import {GenericPageHeaderProps} from '../generic-page-header/generic-page-header-props';
import {RefObject} from 'react';
import {ServerEntityType} from '../../shells/staff/data/server-entity-type';

type TabConfig<ItemType> = {
    path: string;
    label: string;
    badge?: BadgeProps;
    isDisabled?: (item: ItemType | undefined) => boolean;
};

export interface GenericDetailsPageProps<ItemType, ID, AdditionalData> {
    getTabTitle: (item: ItemType) => string;
    header: Omit<GenericPageHeaderProps, 'isBusy'>;
    initializeItem: (api: Api) => ItemType;
    fetchData: (api: Api, id: ID) => Promise<ItemType>;
    fetchAdditionalData?: AdditionalDataFetchObject<AdditionalData, ID | string>;
    tabs: TabConfig<ItemType>[] | ((item: ItemType | undefined) => TabConfig<ItemType>[]);
    idParam?: string;
    // parentLink is used for links to the list pages on 404 errors
    parentLink?: {
        label: string,
        to: string,
    },
    getHeaderTitle?: (item?: ItemType, isNewItem?: boolean, notFound?: boolean) => string;
    itemRef?: RefObject<ItemType | null>;
    entityType?: ServerEntityType;
    isEditable?: (item: ItemType | undefined) => boolean;
}

type AdditionalDataFetchObject<AdditionalData, ID> = {
    [key in keyof AdditionalData]: (api: Api, id: ID) => Promise<AdditionalData[key]>;
}
