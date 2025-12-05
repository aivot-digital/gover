import {ReactNode, RefObject} from 'react';
import {BadgeProps, SxProps} from '@mui/material';
import {GridColDef, GridRowModel} from '@mui/x-data-grid';
import {Api} from '../../hooks/use-api';
import {GenericListRowModel} from './generic-list-row-models';
import {Action} from '../actions/actions-props';
import {Page} from '../../models/dtos/page';

export type GenericListColDef<T extends GridRowModel> = GridColDef<T> & {
    onlyFullScreen?: boolean;
};

export interface GenericListFilter {
    label: string;
    value: string;
    badge?: BadgeProps;
}

export interface GenericListProps<ItemType extends GenericListRowModel> {
    disableFullWidthToggle?: boolean;
    sx?: SxProps;

    preSearchElements?: ReactNode[];
    menuItems?: Array<{
        label: string;
        icon: ReactNode;
        onClick: () => void;
    }>;
    searchLabel?: string;
    searchPlaceholder?: string;
    columnIcon?: ReactNode | ((item: ItemType) => ReactNode);
    columnDefinitions: Array<GenericListColDef<ItemType> & { onlyFullScreen?: boolean; }>;
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
    filters?: GenericListFilter[];
    defaultFilter?: string;
    fetch: (options: GenericListPropsFetchOptions<ItemType>) => Promise<Page<ItemType>>;

    onFullWidthChange?: (isFullWidth: boolean) => void;
    onBusyChange?: (isBusy: boolean) => void;

    dynamicRowHeight?: boolean;

    controlRef?: RefObject<ListControlRef | null>;
}

export type ListControlRef = {
    refresh: () => void;
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
