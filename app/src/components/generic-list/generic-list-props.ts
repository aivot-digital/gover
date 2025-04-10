import {ReactNode} from 'react';
import {BadgeProps, SxProps} from '@mui/material';
import {GridColDef} from '@mui/x-data-grid';
import {Api} from '../../hooks/use-api';
import {GenericListRowModel} from './generic-list-row-models';
import {Action} from '../actions/actions-props';
import {Page} from '../../models/dtos/page';

export interface GenericListProps<ItemType extends GenericListRowModel> {
    disableFullWidthToggle?: boolean;
    sx?: SxProps;

    preSearchElements?: ReactNode[];
    menuItems?: Array<{
        label: string;
        icon: ReactNode;
        onClick: () => void;
    }>;
    searchLabel: string;
    searchPlaceholder: string;
    columnDefinitions: Array<GridColDef<ItemType, ItemType, ItemType> & { onlyFullScreen?: boolean; }>;
    getRowIdentifier: (item: ItemType) => string;
    noDataPlaceholder?: ReactNode;
    noSearchResultsPlaceholder?: ReactNode;
    loadingPlaceholder?: ReactNode;
    rowMenuItems?: Array<{
        label: string;
        icon: ReactNode;
        onClick: (item: ItemType) => void;
    }>;
    rowActions?: (item: ItemType) => Action[];
    rowActionsCount?: number;
    defaultSortField?: keyof ItemType;
    filters?: {
        label: string;
        value: string;
        badge?: BadgeProps;
    }[];
    defaultFilter?: string;
    fetch: (options: GenericListPropsFetchOptions<ItemType>) => Promise<Page<ItemType>>;

    onFullWidthChange?: (isFullWidth: boolean) => void;
    onBusyChange?: (isBusy: boolean) => void;
}

export type SortOrder = 'ASC' | 'DESC';

export interface GenericListPropsFetchOptions<ItemType extends GenericListRowModel> {
    api: Api;
    search: string | undefined;
    page: number;
    size: number;
    sort?: keyof ItemType;
    order?: SortOrder;
    filter?: string;
}
