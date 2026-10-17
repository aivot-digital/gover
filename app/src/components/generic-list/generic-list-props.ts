import {ReactNode, RefObject} from 'react';
import {SxProps} from '@mui/material';
import {type ChipProps} from '../chip/chip';
import {GridColDef, GridRowModel} from '@mui/x-data-grid';
import {Api} from '../../hooks/use-api';
import {GenericListRowModel} from './generic-list-row-models';
import {Action} from '../actions/actions-props';
import {type StorageKey} from '../../data/storage-key';
import {Page} from '../../models/dtos/page';

export type GenericListColDef<T extends GridRowModel> = GridColDef<T> & {
    onlyFullScreen?: boolean;
};

export interface GenericListFilter {
    label: string;
    value: string;
    showCount?: boolean;
    countColor?: ChipProps['color'];
}

export type ListFilterCounts = Record<string, number>;

export interface ListFilterCountsOptions {
    signal: AbortSignal;
}

export type FetchListFilterCounts = (options: ListFilterCountsOptions) => Promise<ListFilterCounts>;

export interface GenericListProps<ItemType extends GenericListRowModel> {
    disableFullWidthToggle?: boolean;
    sx?: SxProps;

    preSearchElements?: ReactNode[];
    filterActions?: ReactNode;
    hasActiveAdditionalFilters?: boolean;
    listContextElements?: ReactNode[];
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
    defaultSortOrder?: 'asc' | 'desc';
    enableColumnSelection?: boolean;
    initialColumnVisibilityModel?: Record<string, boolean>;
    columnSettingsStorageKey?: StorageKey;
    filters?: GenericListFilter[];
    defaultFilter?: string;
    fetch: (options: GenericListPropsFetchOptions<ItemType>) => Promise<Page<ItemType>>;
    /** Counts the readable list scope independently of search and secondary filters; keep this callback stable. */
    fetchFilterCounts?: FetchListFilterCounts;
    /** Refresh rows and counts when the surrounding access context changes. */
    refreshKey?: unknown;

    onFullWidthChange?: (isFullWidth: boolean) => void;
    onBusyChange?: (isBusy: boolean) => void;

    dynamicRowHeight?: boolean;
    rowHeight?: number;

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
