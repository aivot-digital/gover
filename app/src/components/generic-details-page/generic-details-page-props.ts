import {type BadgeProps} from '@mui/material';
import {type Api} from '../../hooks/use-api';
import {type GenericPageHeaderProps} from '../generic-page-header/generic-page-header-props';
import {type ReactNode, type RefObject} from 'react';
import {type ServerEntityType} from '../../shells/staff/data/server-entity-type';
import {
    type PermissionLike,
    type PermissionRequirement,
    type PermissionScope,
} from '../../modules/permissions/utils/permission-utils';

export type TabConfig<ItemType> = {
    path: string;
    label: ReactNode;
    badge?: BadgeProps;
    onlyExisting?: boolean;
    requiredPermission?: GenericDetailsPageTabPermission<ItemType>;
    isDisabled?: (item: ItemType | undefined) => boolean;
    disabledTooltip?: ReactNode | ((item: ItemType | undefined) => ReactNode);
};

export type GenericDetailsPagePermissionScope<ItemType> = PermissionScope<ItemType>;

export type GenericDetailsPagePermissionConfig<ItemType> = {
    scope: GenericDetailsPagePermissionScope<ItemType>;
    create?: PermissionLike;
    read?: PermissionLike;
    update?: PermissionLike;
};

export type GenericDetailsPageTabPermission<ItemType> = PermissionRequirement<ItemType>;

export type GenericDetailsPageHeaderConfig<ItemType> =
    Omit<GenericPageHeaderProps, 'isBusy'>
    | ((item: ItemType | undefined, isNewItem: boolean, notFound: boolean) => Omit<GenericPageHeaderProps, 'isBusy'>);

export type GenericDetailsPageControlRef = {
    refresh: () => void;
};

export interface GenericDetailsPageProps<ItemType, ID, AdditionalData> {
    getTabTitle: (item: ItemType) => string;
    header: GenericDetailsPageHeaderConfig<ItemType>;
    initializeItem: (api: Api) => ItemType;
    fetchData: (api: Api, id: ID) => Promise<ItemType>;
    fetchAdditionalData?: AdditionalDataFetchObject<AdditionalData, ID | string>;
    tabs: TabConfig<ItemType>[] | ((item: ItemType | undefined, isNewItem: boolean) => TabConfig<ItemType>[]);
    idParam?: string;
    // parentLink is used for links to the list pages on 404 errors
    parentLink?: {
        label: string,
        to: string,
    },
    getHeaderTitle?: (item?: ItemType, isNewItem?: boolean, notFound?: boolean) => string;
    itemRef?: RefObject<ItemType | null>;
    onItemChange?: (item: ItemType | null) => void;
    additionalDataRef?: RefObject<AdditionalData | null>;
    onAdditionalDataChange?: (item: AdditionalData | null) => void;
    controlRef?: RefObject<GenericDetailsPageControlRef | null>;
    entityType?: ServerEntityType;
    permissionCheck?: GenericDetailsPagePermissionConfig<ItemType>;
    isEditable?: (item: ItemType | undefined) => boolean;
    hasAccess?: (item: ItemType | undefined) => void;
}

type AdditionalDataFetchObject<AdditionalData, ID> = {
    [key in keyof AdditionalData]: (api: Api, id: ID) => Promise<AdditionalData[key]>;
}
