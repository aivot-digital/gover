import {DataGrid, GridCallbackDetails, gridClasses, gridPageCountSelector, gridPageSelector, GridPaginationModel, GridSortModel} from '@mui/x-data-grid';
import {Box, CircularProgress, Menu, MenuItem, styled, SxProps, Tab, Tabs} from '@mui/material';
import React, {useCallback, useEffect, useMemo, useReducer, useRef, useState} from 'react';
import {TextFieldComponent} from '../text-field/text-field-component';
import SearchOutlinedIcon from '@mui/icons-material/SearchOutlined';
import {IconButton} from '../icon-button/icon-button';
import ZoomOutMapOutlinedIcon from '@mui/icons-material/ZoomOutMapOutlined';
import ZoomInMapOutlinedIcon from '@mui/icons-material/ZoomInMapOutlined';
import MoreVertOutlinedIcon from '@mui/icons-material/MoreVertOutlined';
import {useApi} from '../../hooks/use-api';
import {Page} from '../../models/dtos/page';
import {isStringNotNullOrEmpty, isStringNullOrEmpty} from '../../utils/string-utils';
import {GenericListRowModel} from './generic-list-row-models';
import {Actions} from '../actions/actions';
import {withAsyncWrapper} from '../../utils/with-async-wrapper';
import {GenericListProps} from './generic-list-props';
import CloseIcon from '@mui/icons-material/Close';
import {useSearchParams} from 'react-router-dom';
import {GridSortItem} from '@mui/x-data-grid/models/gridSortModel';

const UrlParamKeys = {
    search: 'search',
    page: 'page',
    size: 'size',
    sort: 'sort',
    order: 'order',
    filter: 'filter',
};

export function GenericList<ItemType extends GenericListRowModel, FilterOption extends string | void = void>(props: GenericListProps<ItemType>) {
    const {
        columnDefinitions: originalColumnDefinitions,
        defaultSortField,
        noSearchResultsPlaceholder,
        noDataPlaceholder,
        dynamicRowHeight,
        rowActionsCount,
        rowActions,
        getRowIdentifier,
        defaultFilter,
        fetch: fetchFunc,
    } = props;

    const api = useApi();
    const [searchParams, setSearchParams] = useSearchParams();

    // Flag if the view is in full width mode
    const [isFullWidth, toggleIsFullWidth] = useReducer((isFullWidth) => !isFullWidth, false);

    // Slot for the menu item anchor element which is rendered when the menu button for a single row is clicked
    const [menuAnchorElement, setMenuAnchorElement] = useState<null | HTMLElement>(null);

    // Flag if the list is currently loading data
    const [isBusy, setIsBusy] = useState(false);

    // The currently loaded items
    const [items, setItems] = useState<Page<ItemType>>();

    // Establish the row count as a ref to avoid the data grid from resetting to page 0 if the total number of items is temporarily undefined
    const rowCountBuffer = useRef<number>(0);
    const rowCount = useMemo(() => {
        if (items?.totalElements != null) {
            rowCountBuffer.current = items.totalElements;
        }
        return rowCountBuffer.current;
    }, [items?.totalElements]);

    // Ref to the current abort controller to cancel ongoing fetch requests when a new one is started
    const abortControllerRef = useRef<AbortController | null>(null);

    // Sorting Model - Default is retrieved from URL params or prop
    const [sortModel, _setSortModel] = useState<GridSortModel>([]);
    // Pagination Model - Default is retrieved from URL params
    const [paginationModel, _setPaginationModel] = useState<GridPaginationModel>({
        page: 0,
        pageSize: 12,
    });
    const [search, _setSearch] = useState('');
    const [currentFilter, _setCurrentFilter] = useState<FilterOption | null>(null);

    // Set the internal state from the URL parameters
    useEffect(() => {
        const sortModel = sortModelFromSearchParams(searchParams, defaultSortField);
        // Check if the sort model is truly different to avoid resetting the datagrid to page 0 whenever new search params are set.
        // Just replacing the state caused a bug where the pagination was randomly resettet when reaching page 3.
        _setSortModel((prev) => {
            if (prev.length !== sortModel.length || prev[0]?.field !== sortModel[0]?.field || prev[0]?.sort !== sortModel[0]?.sort) {
                return sortModel;
            }
            return prev;
        });

        const paginationModel = paginationModelFromSearchParams(searchParams);
        _setPaginationModel(paginationModel);

        _setSearch(searchParams.get(UrlParamKeys.search) || '');
        _setCurrentFilter(searchParams.get(UrlParamKeys.filter) as FilterOption);
    }, [searchParams]);

    /**
     * Handles changes to the sorting model by updating the URL parameters.
     * @param newSortModel The new sorting model.
     */
    const handleSortModelChange = (newSortModel: GridSortModel) => {
        const sortItem = newSortModel[0] as GridSortItem | undefined;

        const field = sortItem?.field ?? defaultSortField?.toString();
        const order = sortItem?.sort ?? 'asc';

        if (field == null || isStringNullOrEmpty(field)) {
            searchParams.delete(UrlParamKeys.sort);
            searchParams.delete(UrlParamKeys.order);
        } else {
            searchParams.set(UrlParamKeys.sort, field);
            searchParams.set(UrlParamKeys.order, order);
        }

        setSearchParams(searchParams);
    };

    /**
     * Handles changes to the pagination model by updating the URL parameters.
     */
    const handlePaginationModelChange = (newPaginationModel: GridPaginationModel, details: GridCallbackDetails) => {
        const _pageCount = gridPageCountSelector({
            current: details.api,
        } as any);
        const _page = gridPageSelector({
            current: details.api,
        } as any);

        const page = newPaginationModel.page ?? 0;
        const size = newPaginationModel.pageSize ?? 12;

        searchParams.set(UrlParamKeys.page, (page + 1).toString());
        searchParams.set(UrlParamKeys.size, size.toString());

        setSearchParams(searchParams);
    };

    /**
     * Handles changes to the search term by updating the URL parameters.
     * Resets to the first page when the search term changes.
     * @param newSearch The new search term.
     */
    const handleSearchChange = (newSearch: string | undefined) => {
        if (newSearch == null || isStringNullOrEmpty(newSearch)) {
            searchParams.delete(UrlParamKeys.search);
        } else {
            searchParams.set(UrlParamKeys.search, newSearch);
        }

        // Reset to first page when search changes
        searchParams.set(UrlParamKeys.page, '1');

        setSearchParams(searchParams, {
            replace: true,
        });
    };

    /**
     * Handles changes to the current filter by updating the URL parameters.
     * Resets to the first page when the filter changes.
     * @param newFilter The new filter option.
     */
    const handleFilterChange = (newFilter: FilterOption | null) => {
        if (newFilter == null) {
            searchParams.delete(UrlParamKeys.filter);
        } else {
            searchParams.set(UrlParamKeys.filter, newFilter.toString());
        }

        // Reset to first page when filter changes
        searchParams.set(UrlParamKeys.page, '1');

        setSearchParams(searchParams);
    };

    const handleRefresh = useCallback(() => {
        setIsBusy(true);

        if (abortControllerRef.current) {
            abortControllerRef.current.abort();
        }

        const controller = new AbortController();
        abortControllerRef.current = controller;

        let sort: string | undefined = defaultSortField as string | undefined;
        let direction: 'ASC' | 'DESC' | undefined = 'ASC';

        if (sortModel != null && sortModel.length > 0) {
            sort = sortModel[0].field;
            direction = sortModel[0].sort === 'asc' ? 'ASC' : 'DESC';
        }

        withAsyncWrapper({
            desiredMinRuntime: 800,
            main: () => fetchFunc({
                api: api,
                search: isStringNotNullOrEmpty(search) ? search : undefined,
                page: paginationModel.page < 0 ? 0 : paginationModel.page,
                size: paginationModel.pageSize,
                sort: isStringNotNullOrEmpty(sort) ? sort : undefined,
                order: isStringNotNullOrEmpty(sort) ? direction : undefined,
                filter: currentFilter ?? defaultFilter,
            }),
            signal: controller.signal,
        })
            .then(page => {
                if (!controller.signal.aborted) {
                    setItems(page);
                }
            })
            .catch(error => {
                if (error.name !== 'AbortError') {
                    console.error(error);
                }
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setIsBusy(false);
                }
            });
    }, [api, currentFilter, sortModel, defaultFilter, search, defaultSortField, fetchFunc, paginationModel.page]);

    // Fetch data on dependency changes
    // This is a duplicate of handleRefresh to combat outdated data in the closure
    useEffect(() => {
        setIsBusy(true);

        if (abortControllerRef.current) {
            abortControllerRef.current.abort();
        }

        const controller = new AbortController();
        abortControllerRef.current = controller;

        let sort: string | undefined = defaultSortField as string | undefined;
        let direction: 'ASC' | 'DESC' | undefined = 'ASC';

        if (sortModel != null && sortModel.length > 0) {
            sort = sortModel[0].field;
            direction = sortModel[0].sort === 'asc' ? 'ASC' : 'DESC';
        }

        withAsyncWrapper({
            desiredMinRuntime: 800,
            main: () => props.fetch({
                api: api,
                search: isStringNotNullOrEmpty(search) ? search : undefined,
                page: paginationModel.page < 0 ? 0 : paginationModel.page,
                size: paginationModel.pageSize,
                sort: isStringNotNullOrEmpty(sort) ? sort : undefined,
                order: isStringNotNullOrEmpty(sort) ? direction : undefined,
                filter: currentFilter ?? props.defaultFilter,
            }),
            signal: controller.signal,
        })
            .then(page => {
                if (!controller.signal.aborted) {
                    setItems(page);
                }
            })
            .catch(error => {
                if (error.name !== 'AbortError') {
                    console.error(error);
                }
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setIsBusy(false);
                }
            });
    }, [api, currentFilter, sortModel, paginationModel, search]);

    useEffect(() => {
        if (props.controlRef == null) {
            return;
        }

        props.controlRef.current = {
            refresh: () => {
                handleRefresh();
            },
        };
    }, [handleRefresh]);

    useEffect(() => {
        return () => {
            if (abortControllerRef.current) {
                abortControllerRef.current.abort();
            }
        };
    }, []);

    useEffect(() => {
        props.onFullWidthChange?.(isFullWidth);
    }, [isFullWidth]);

    useEffect(() => {
        props.onBusyChange?.(isBusy);
    }, [isBusy]);

    const columnDefinitions = useMemo(() => {
        const columns = originalColumnDefinitions
            .filter(column => !column.onlyFullScreen || isFullWidth);

        if (rowActions != null) {
            columns.push({
                field: 'actions',
                headerName: '',
                sortable: false,
                resizable: false,
                // dynamic width calculation would result in a layout shift of the table, so we use a prop
                width: rowActionsCount ? (rowActionsCount * 42) + 26 : (4 * 42) + 26,
                renderCell: (params) => {
                    if (rowActions == null) {
                        return null;
                    }

                    return (
                        <Actions
                            actions={rowActions(params.row)}
                            sx={{
                                justifyContent: 'end',
                            }}
                            dense
                        />
                    );
                },
            });
        }

        return columns;
    }, [originalColumnDefinitions, isFullWidth, rowActions, rowActionsCount]);

    const showTopControls =
        ((props.filters != null && props.filters.length > 0) || props.disableFullWidthToggle !== true);

    const NoRowsOverlay = useMemo(() => () => (
        <StyledGridOverlay>
            <Box
                sx={{
                    position: 'relative',
                    display: 'inline-flex',
                }}
            >
                {
                    isStringNotNullOrEmpty(search) ?
                        (noSearchResultsPlaceholder ?? 'Keine Suchergebnisse gefunden.') :
                        (noDataPlaceholder ?? 'Keine Daten vorhanden.')
                }
            </Box>
        </StyledGridOverlay>
    ), [search, noSearchResultsPlaceholder, noDataPlaceholder]);

    const lastColIndex = columnDefinitions.length - 1;

    const style: SxProps = useMemo(() => ({
        width: '100%',
        borderRadius: 0,
        borderLeft: 'none',
        borderRight: 'none',
        backgroundColor: 'background.paper',
        '& .MuiDataGrid-columnHeader:first-of-type, & .MuiDataGrid-cell[data-colindex="0"]': {
            paddingLeft: '16px',
        },
        [`& .MuiDataGrid-columnHeader:last-of-type, & .MuiDataGrid-cell[data-colindex="${lastColIndex}"]`]: {
            paddingRight: '16px',
        },
        '& .MuiDataGrid-columnHeader': {
            backgroundColor: 'rgba(20, 38, 56, 0.06)',
        },
        '& .MuiDataGrid-columnHeader .MuiDataGrid-columnSeparator': {
            color: 'rgba(20, 38, 56, 0.2)',
        },
        // Remove cell focus outline
        [`& .${gridClasses.cell}:focus, & .${gridClasses.cell}:focus-within`]: {
            outline: 'none',
        },
        [`& .${gridClasses.columnHeader}:focus, & .${gridClasses.columnHeader}:focus-within`]: {
            outline: 'none',
        },
        // Remove drag handle for columns that are not resizeable
        [`& .${gridClasses.columnSeparator}`]: {
            [`&:not(.${gridClasses['columnSeparator--resizable']})`]: {
                display: 'none',
            },
        },
    }), [lastColIndex]);

    return (
        <Box
            sx={props.sx}
        >
            {
                showTopControls &&
                <Box
                    sx={{
                        display: 'flex',
                        alignItems: 'flex-end',
                        borderBottom: 1,
                        borderBottomColor: 'divider',
                    }}
                >
                    {
                        props.filters != null &&
                        <Tabs
                            sx={{
                                flex: 1,
                            }}
                            value={currentFilter ?? props.defaultFilter}
                            onChange={(_, newValue) => handleFilterChange(newValue)}
                        >
                            {
                                props.filters.map((filter) => (
                                    <Tab
                                        key={'' + filter.value}
                                        value={filter.value}
                                        label={filter.label}
                                    />
                                ))
                            }
                        </Tabs>
                    }

                    {
                        props.disableFullWidthToggle !== true &&
                        <IconButton
                            buttonProps={{
                                onClick: toggleIsFullWidth,
                                sx: {
                                    marginLeft: 'auto',
                                    marginRight: 1,
                                    marginY: 1,
                                },
                            }}
                            tooltipProps={{
                                title: isFullWidth ? 'Vollbildmodus beenden' : 'Vollbildmodus aktivieren',
                            }}
                        >
                            {
                                isFullWidth ?
                                    <ZoomInMapOutlinedIcon /> :
                                    <ZoomOutMapOutlinedIcon />
                            }
                        </IconButton>
                    }
                </Box>
            }

            <Box
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                    paddingX: 2,
                    gap: 2.5,
                }}
            >
                {
                    props.preSearchElements?.map((element, index) => (
                        <Box
                            key={index}
                            sx={{
                                flex: 1,
                            }}
                        >
                            {element}
                        </Box>
                    ))
                }
                {
                    props.searchLabel != null &&
                    <Box
                        sx={{
                            flex: 1,
                            padding: '4px 0 12px 0',
                        }}
                    >
                        <TextFieldComponent
                            label={props.searchLabel}
                            value={search}
                            onChange={handleSearchChange}
                            placeholder={props.searchPlaceholder}
                            startIcon={<SearchOutlinedIcon />}
                            endAction={
                                search
                                    ? {
                                        onClick: () => handleSearchChange(undefined),
                                        tooltip: 'Suche zurücksetzen',
                                        icon: <CloseIcon sx={{fontSize: 20}} />,
                                    }
                                    : undefined
                            }
                            debounce={1000}
                            size={'small'}
                        />
                    </Box>
                }

                {
                    props.menuItems != null &&
                    <Box>
                        <IconButton
                            buttonProps={{
                                onClick: (event) => setMenuAnchorElement(event.currentTarget),
                            }}
                            tooltipProps={{
                                title: 'Mehr',
                            }}
                        >
                            <MoreVertOutlinedIcon />
                        </IconButton>
                    </Box>
                }
            </Box>

            <Box
                sx={{
                    display: 'flex',
                    flexDirection: 'column',
                }}
            >
                <DataGrid
                    columns={columnDefinitions}
                    getRowId={getRowIdentifier}
                    rows={items?.content ?? []}
                    pagination={true}
                    paginationMode="server"
                    paginationModel={paginationModel}
                    onPaginationModelChange={handlePaginationModelChange}
                    pageSizeOptions={[12, 24, 48, 96]}
                    loading={isBusy}
                    rowCount={rowCount}
                    isRowSelectable={() => false}
                    sortingMode="server"
                    sortModel={sortModel}
                    onSortModelChange={handleSortModelChange}
                    disableColumnMenu={true}
                    sx={style}
                    slots={{
                        noRowsOverlay: NoRowsOverlay,
                    }}
                    getRowHeight={dynamicRowHeight ? () => 'auto' : undefined}
                />
            </Box>

            {
                props.menuItems &&
                <Menu
                    anchorEl={menuAnchorElement}
                    open={menuAnchorElement !== null}
                    onClose={() => setMenuAnchorElement(null)}
                >
                    {
                        props.menuItems.map(item => (
                            <MenuItem
                                onClick={() => {
                                    item.onClick();
                                    setMenuAnchorElement(null);
                                }}
                            >
                                {item.icon}
                                {item.label}
                            </MenuItem>
                        ))
                    }
                </Menu>
            }
        </Box>
    );
}

/**
 * Creates a sorting model from URL search parameters.
 *
 * When no sort field is specified, an empty model is returned to avoid sorting.
 *
 * @param searchParams The URLSearchParams object containing the search parameters.
 * @param defaultSortField An optional default sort field to use if none is specified in the URL.
 */
function sortModelFromSearchParams(searchParams: URLSearchParams, defaultSortField?: any): GridSortModel {
    let sortField = searchParams.get(UrlParamKeys.sort);
    if (sortField == null && defaultSortField != null) {
        sortField = defaultSortField.toString();
    }

    // When no sort field is specified, we return an empty model to avoid sorting
    if (sortField == null) {
        return [];
    }

    const sortOrder: 'asc' | 'desc' = searchParams.get(UrlParamKeys.order) === 'desc' ? 'desc' : 'asc';

    return [{
        field: sortField,
        sort: sortOrder,
    }];
}

/**
 * Creates a pagination model from URL search parameters.
 *
 * Defaults to page 0 and size 12 if parameters are missing or invalid.
 *
 * @param searchParams The URLSearchParams object containing the search parameters.
 */
function paginationModelFromSearchParams(searchParams: URLSearchParams): GridPaginationModel {
    const pageFromUrl = Number(searchParams.get(UrlParamKeys.page));
    const page = (isNaN(pageFromUrl) || pageFromUrl < 1) ? 0 : pageFromUrl - 1;

    const sizeFormUrl = Number(searchParams.get(UrlParamKeys.size));
    const size = isNaN(sizeFormUrl) || sizeFormUrl < 1 ? 12 : sizeFormUrl;

    return {
        page: page,
        pageSize: size,
    };
}

const StyledGridOverlay = styled('div')(({theme}) => ({
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    height: '100%',
    backgroundColor: 'rgba(18, 18, 18, 0.9)',
    ...theme.applyStyles('light', {
        backgroundColor: 'rgba(255, 255, 255, 0.9)',
    }),
}));

const LoadingOverlay = () => (
    <StyledGridOverlay>
        <Box
            sx={{
                position: 'relative',
                display: 'inline-flex',
            }}
        >
            <CircularProgress />
        </Box>
    </StyledGridOverlay>
);