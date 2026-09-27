import {
    DataGrid,
    gridClasses,
    GridPaginationModel,
    GridSortModel,
    GridPreferencePanelsValue,
    useGridApiRef,
} from '@mui/x-data-grid';
import {
    Box,
    Button,
    CircularProgress,
    Menu,
    MenuItem,
    styled,
    SxProps,
    Tab,
    Tabs,
    type Theme as MuiTheme,
} from '@mui/material';
import React, {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {SearchInput} from '../search-input/search-input';
import {IconButton} from '../icon-button/icon-button';
import ZoomOutMapOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/ZoomOutMap';
import ZoomInMapOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/ZoomInMap';
import MoreVertOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/MoreVert';
import {useApi} from '../../hooks/use-api';
import {Page} from '../../models/dtos/page';
import {isStringNotNullOrEmpty, isStringNullOrEmpty} from '../../utils/string-utils';
import {GenericListRowModel} from './generic-list-row-models';
import {Actions} from '../actions/actions';
import {withAsyncWrapper} from '../../utils/with-async-wrapper';
import {GenericListColumnsManagement} from './generic-list-columns-management';
import {
    loadListColumnSettings,
    saveListColumnSettings,
    loadListFullWidth,
    saveListFullWidth,
} from './generic-list-column-settings';
import {GenericListProps} from './generic-list-props';
import {useSearchParams} from 'react-router-dom';
import {CellContentWrapper} from '../cell-content-wrapper/cell-content-wrapper';
import WidthWide from '@aivot/mui-material-symbols-400-n25-outlined/WidthWide';
import ViewColumn from '@aivot/mui-material-symbols-400-n25-outlined/ViewColumn';
import FitPageWidth from '@aivot/mui-material-symbols-400-n25-outlined/FitPageWidth';
import {useListFilterCounts} from './use-list-filter-counts';
import {GenericListFilterLabel} from './generic-list-filter-label';
import {FormFieldTokens} from '../../theming/form-field-tokens';

const UrlParamKeys = {
    search: 'search',
    page: 'page',
    size: 'size',
    sort: 'sort',
    order: 'order',
    filter: 'filter',
};

export function GenericList<ItemType extends GenericListRowModel, FilterOption extends string | void = void>(
    props: GenericListProps<ItemType>,
) {
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
        columnIcon,
        refreshKey,
    } = props;

    const api = useApi();
    const gridApiRef = useGridApiRef();
    const [searchParams, setSearchParams] = useSearchParams();

    // Flag if the view is in full width mode
    const [isFullWidth, setIsFullWidth] = useState(
        () => props.disableFullWidthToggle !== true && loadListFullWidth(props.columnSettingsStorageKey),
    );
    const [columnResetCount, setColumnResetCount] = useState(0);
    const columnButtonRef = useRef<HTMLButtonElement>(null);
    const toggleIsFullWidth = () => {
        const next = !isFullWidth;
        setIsFullWidth(next);
        saveListFullWidth(props.columnSettingsStorageKey, next);
    };

    // Slot for the menu item anchor element which is rendered when the menu button for a single row is clicked
    const [menuAnchorElement, setMenuAnchorElement] = useState<null | HTMLElement>(null);

    // Flag if the list is currently loading data
    const [isBusy, setIsBusy] = useState(false);

    // The currently loaded items
    const [items, setItems] = useState<Page<ItemType>>();

    // Establish the row count as a ref to avoid the data grid from resetting to page 0 if the total number of items is temporarily undefined
    const rowCountBuffer = useRef<number>(0);
    const rowCount = useMemo(() => {
        if (items?.page.totalElements != null) {
            rowCountBuffer.current = items.page.totalElements;
        }
        return rowCountBuffer.current;
    }, [items?.page.totalElements]);

    // Ref to the current abort controller to cancel ongoing fetch requests when a new one is started
    const abortControllerRef = useRef<AbortController | null>(null);

    // Read the entire query in the same render. Mirrored state would briefly combine new
    // additional filters with the previous page and issue an inconsistent request.
    const sortField = searchParams.get(UrlParamKeys.sort) ?? defaultSortField?.toString();
    const sortOrder =
        searchParams.get(UrlParamKeys.order) === 'desc'
            ? 'desc'
            : searchParams.get(UrlParamKeys.order) === 'asc'
              ? 'asc'
              : (props.defaultSortOrder ?? 'asc');
    // Preserve model identity across page/filter changes: DataGrid treats a new sort model as a reset.
    const sortModel = useMemo<GridSortModel>(
        () =>
            sortField == null
                ? []
                : [
                      {
                          field: sortField,
                          sort: sortOrder,
                      },
                  ],
        [sortField, sortOrder],
    );
    const paginationModel = paginationModelFromSearchParams(searchParams);
    const search = searchParams.get(UrlParamKeys.search) ?? '';
    const currentFilter = searchParams.get(UrlParamKeys.filter) as FilterOption | null;
    const {
        counts,
        failed: countsFailed,
        busy: countsBusy,
        refresh: refreshCounts,
    } = useListFilterCounts(props.fetchFilterCounts, currentFilter ?? defaultFilter, refreshKey);

    /**
     * Handles changes to the sorting model by updating the URL parameters.
     * @param newSortModel The new sorting model.
     */
    const handleSortModelChange = (newSortModel: GridSortModel) => {
        const sortItem = newSortModel[0] as GridSortModel[number] | undefined;

        const field = sortItem?.field ?? defaultSortField?.toString();
        const order = sortItem?.sort ?? props.defaultSortOrder ?? 'asc';

        setSearchParams((current) => {
            const next = new URLSearchParams(current);
            if (field == null || isStringNullOrEmpty(field)) {
                next.delete(UrlParamKeys.sort);
                next.delete(UrlParamKeys.order);
            } else {
                next.set(UrlParamKeys.sort, field);
                next.set(UrlParamKeys.order, order);
            }
            return next;
        });
    };

    /**
     * Handles changes to the pagination model by updating the URL parameters.
     */
    const handlePaginationModelChange = (newPaginationModel: GridPaginationModel) => {
        const page = newPaginationModel.page ?? 0;
        const size = newPaginationModel.pageSize ?? 12;

        setSearchParams((current) => {
            const next = new URLSearchParams(current);
            next.set(UrlParamKeys.page, (page + 1).toString());
            next.set(UrlParamKeys.size, size.toString());
            return next;
        });
    };

    /**
     * Handles changes to the search term by updating the URL parameters.
     * Resets to the first page when the search term changes.
     * @param newSearch The new search term.
     */
    const handleSearchChange = (newSearch: string | undefined) => {
        setSearchParams(
            (current) => {
                const next = new URLSearchParams(current);
                if (newSearch == null || isStringNullOrEmpty(newSearch)) {
                    next.delete(UrlParamKeys.search);
                } else {
                    next.set(UrlParamKeys.search, newSearch);
                }
                next.set(UrlParamKeys.page, '1');
                return next;
            },
            {replace: true},
        );
    };

    /**
     * Handles changes to the current filter by updating the URL parameters.
     * Resets to the first page when the filter changes.
     * @param newFilter The new filter option.
     */
    const handleFilterChange = (newFilter: FilterOption | null) => {
        setSearchParams((current) => {
            const next = new URLSearchParams(current);
            if (newFilter == null) {
                next.delete(UrlParamKeys.filter);
            } else {
                next.set(UrlParamKeys.filter, newFilter.toString());
            }
            next.set(UrlParamKeys.page, '1');
            return next;
        });
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
            main: () =>
                fetchFunc({
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
            .then((page) => {
                if (!controller.signal.aborted) {
                    setItems(page);
                }
            })
            .catch((error) => {
                if (error.name !== 'AbortError') {
                    console.error(error);
                }
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setIsBusy(false);
                }
            });
    }, [
        api,
        currentFilter,
        sortModel,
        defaultFilter,
        search,
        defaultSortField,
        fetchFunc,
        paginationModel.page,
        paginationModel.pageSize,
        refreshKey,
    ]);

    useEffect(() => {
        handleRefresh();
    }, [handleRefresh]);

    useEffect(() => {
        if (props.controlRef == null) {
            return;
        }

        props.controlRef.current = {
            refresh: () => {
                handleRefresh();
                refreshCounts();
            },
        };
    }, [handleRefresh, refreshCounts, props.controlRef]);

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
        const columns = originalColumnDefinitions.filter((column) => !column.onlyFullScreen || isFullWidth);

        if (columnIcon != null) {
            columns.unshift({
                field: 'icon',
                hideable: false,
                headerName: 'Symbol',
                renderHeader: () => null,
                renderCell: (params) => {
                    const icon = typeof columnIcon === 'function' ? columnIcon(params.row) : columnIcon;
                    return <CellContentWrapper>{icon}</CellContentWrapper>;
                },
                disableColumnMenu: true,
                width: 24,
                sortable: false,
            });
        }

        if (rowActions != null) {
            columns.push({
                field: 'actions',
                hideable: false,
                headerName: '',
                sortable: false,
                resizable: false,
                // dynamic width calculation would result in a layout shift of the table, so we use a prop
                width: rowActionsCount ? rowActionsCount * 42 + 26 : 4 * 42 + 26,
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
    }, [columnIcon, originalColumnDefinitions, isFullWidth, rowActions, rowActionsCount]);

    const initialColumnSettings = useMemo(
        () =>
            loadListColumnSettings(
                columnResetCount > 0 ? undefined : props.columnSettingsStorageKey,
                columnDefinitions,
                props.initialColumnVisibilityModel,
            ),
        [props.columnSettingsStorageKey, columnDefinitions, props.initialColumnVisibilityModel, columnResetCount],
    );
    const persistColumnSettings = useCallback(() => {
        saveListColumnSettings(
            props.columnSettingsStorageKey,
            columnDefinitions,
            gridApiRef.current?.exportState().columns,
        );
    }, [props.columnSettingsStorageKey, columnDefinitions, gridApiRef]);

    const restoreDefaultColumns = useCallback(() => {
        saveListColumnSettings(props.columnSettingsStorageKey, columnDefinitions, {
            columnVisibilityModel: props.initialColumnVisibilityModel ?? {},
            dimensions: {},
        });
        // Remount only the grid to also restore native flex widths and discard resized-column flags.
        setColumnResetCount((count) => count + 1);
        columnButtonRef.current?.focus();
    }, [props.columnSettingsStorageKey, columnDefinitions, props.initialColumnVisibilityModel]);

    const showTopControls =
        (props.filters != null && props.filters.length > 0) ||
        props.disableFullWidthToggle !== true ||
        props.enableColumnSelection === true;
    const hasActiveFilters =
        isStringNotNullOrEmpty(search) ||
        props.hasActiveAdditionalFilters === true ||
        (currentFilter != null && currentFilter !== defaultFilter);

    const NoRowsOverlay = useMemo(
        () => () => (
            <StyledGridOverlay>
                <Box
                    sx={{
                        position: 'relative',
                        display: 'inline-flex',
                    }}
                >
                    {hasActiveFilters
                        ? (noSearchResultsPlaceholder ?? 'Keine Suchergebnisse gefunden.')
                        : (noDataPlaceholder ?? 'Keine Daten vorhanden.')}
                </Box>
            </StyledGridOverlay>
        ),
        [hasActiveFilters, noSearchResultsPlaceholder, noDataPlaceholder],
    );

    const lastColIndex = columnDefinitions.length - 1;
    const hasEmptyRows = (items?.content.length ?? 0) === 0;
    const hasFilterFields = props.preSearchElements?.length || props.searchLabel || props.menuItems?.length || props.filterActions;

    const style: SxProps = useMemo(
        () => ({
            width: '100%',
            borderRadius: 0,
            borderBottomLeftRadius: 1,
            borderBottomRightRadius: 1,
            overflow: 'hidden',
            borderLeft: 'none',
            borderRight: 'none',
            borderBottom: 'none',
            backgroundColor: 'background.paper',
            '& .MuiDataGrid-columnHeader:first-of-type, & .MuiDataGrid-cell[data-colindex="0"]': {
                paddingLeft: '16px',
            },
            [`& .MuiDataGrid-columnHeader:last-of-type, & .MuiDataGrid-cell[data-colindex="${lastColIndex}"]`]: {
                paddingRight: '16px',
            },
            [`& .${gridClasses.columnHeaders}, & .${gridClasses.columnHeader}, & .${gridClasses.columnHeaders} .${gridClasses.scrollbarFiller}, & .${gridClasses.columnHeaders} .${gridClasses.filler}`]:
                {
                    backgroundColor: (theme: MuiTheme) => `${theme.palette.action.hover} !important`,
                },
            '& .MuiDataGrid-columnHeader .MuiDataGrid-columnSeparator': {
                color: 'divider',
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
            ...(hasEmptyRows
                ? {
                      '& .MuiDataGrid-virtualScroller': {
                          minHeight: 316,
                      },
                  }
                : {}),
            ...(dynamicRowHeight
                ? {
                      // Let actual data rows grow with multiline/custom cell content instead of clipping.
                      // Exclude loading skeleton rows, otherwise their placeholder content gets top-aligned.
                      [`& .${gridClasses.row}:not(.${gridClasses.rowSkeleton}) .${gridClasses.cell}`]: {
                          alignItems: 'flex-start',
                          py: 0.75,
                      },
                      [`& .${gridClasses.row}:not(.${gridClasses.rowSkeleton}) .MuiDataGrid-cellContent`]: {
                          whiteSpace: 'normal',
                          overflow: 'visible',
                          textOverflow: 'unset',
                          lineHeight: 1.35,
                      },
                  }
                : {}),
        }),
        [lastColIndex, dynamicRowHeight, hasEmptyRows],
    );

    return (
        <Box sx={props.sx}>
            {showTopControls && (
                <Box
                    sx={{
                        display: 'flex',
                        alignItems: 'center',
                        borderBottom: 1,
                        borderBottomColor: 'divider',
                    }}
                >
                    {props.filters != null && (
                        <Tabs
                            sx={{
                                flex: 1,
                            }}
                            variant="scrollable"
                            value={currentFilter ?? props.defaultFilter}
                            onChange={(_, newValue) => handleFilterChange(newValue)}
                        >
                            {props.filters.map((filter) => (
                                <Tab
                                    key={'' + filter.value}
                                    value={filter.value}
                                    label={
                                        props.fetchFilterCounts == null || filter.showCount === false ? filter.label : (
                                            <GenericListFilterLabel
                                                filter={filter}
                                                count={counts?.[filter.value]}
                                                failed={countsFailed || (counts != null && counts[filter.value] == null)}
                                                busy={countsBusy}
                                            />
                                        )
                                    }
                                />
                            ))}
                        </Tabs>
                    )}

                    {props.enableColumnSelection && (
                        <Button
                            ref={columnButtonRef}
                            size="small"
                            startIcon={<ViewColumn />}
                            sx={{
                                mx: 1,
                                flexShrink: 0,
                            }}
                            onClick={() => gridApiRef.current?.showPreferences(GridPreferencePanelsValue.columns)}
                        >
                            Spalten
                        </Button>
                    )}
                    {props.disableFullWidthToggle !== true && (
                        <IconButton
                            buttonProps={{
                                onClick: toggleIsFullWidth,
                                sx: {
                                    marginLeft: 'auto',
                                    mr: 0.75,
                                },
                            }}
                            tooltipProps={{
                                title: isFullWidth ? 'Breite der Anzeige beschränken' : 'Volle Bildschirmbreite nutzen',
                            }}
                        >
                            {isFullWidth ? <WidthWide /> : <FitPageWidth />}
                        </IconButton>
                    )}
                </Box>
            )}

            <Box
                sx={{
                    display: 'flex',
                    flexWrap: 'wrap',
                    alignItems: 'start',
                    paddingX: 2,
                    pt: hasFilterFields ? 1 : 0,
                    pb: hasFilterFields ? 2 : 0,
                    gap: 2.5,
                }}
            >
                {React.Children.toArray(props.preSearchElements).map((element, index) => (
                    <Box
                        key={React.isValidElement(element) ? element.key : index}
                        sx={{
                            flex: '1 1 16rem',
                            minWidth: 0,
                            maxWidth: '100%',
                        }}
                    >
                        {element}
                    </Box>
                ))}
                {props.searchLabel != null && (
                    <Box
                        sx={{
                            flex: '1 1 16rem',
                            minWidth: 0,
                            maxWidth: '100%',
                        }}
                    >
                        <SearchInput
                            label={props.searchLabel}
                            value={search}
                            onChange={handleSearchChange}
                            placeholder={props.searchPlaceholder}
                            debounce={1000}
                        />
                    </Box>
                )}

                {props.filterActions != null && (
                    <Box
                        sx={{
                            flexShrink: 0,
                            display: 'flex',
                            alignItems: 'center',
                            height: FormFieldTokens.controlMinHeight,
                            mt: (theme) => `calc(${FormFieldTokens.labelRowMinHeight}px + ${theme.spacing(FormFieldTokens.labelToControlGap)})`,
                        }}
                    >
                        {props.filterActions}
                    </Box>
                )}

                {props.menuItems != null && (
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
                )}
            </Box>

            {props.listContextElements != null && props.listContextElements.length > 0 && (
                <Box
                    sx={{
                        display: 'flex',
                        flexDirection: 'column',
                        paddingX: 2,
                        paddingBottom: 2,
                        gap: 1.5,
                    }}
                >
                    {props.listContextElements.map((element, index) => (
                        <Box key={index}>{element}</Box>
                    ))}
                </Box>
            )}

            <Box
                sx={{
                    display: 'flex',
                    flexDirection: 'column',
                }}
            >
                <DataGrid
                    key={`${props.columnSettingsStorageKey ?? 'list'}-${columnResetCount}`}
                    apiRef={gridApiRef}
                    initialState={{columns: initialColumnSettings}}
                    onColumnVisibilityModelChange={
                        props.columnSettingsStorageKey == null ? undefined : persistColumnSettings
                    }
                    onColumnWidthChange={props.columnSettingsStorageKey == null ? undefined : persistColumnSettings}
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
                        columnsManagement: GenericListColumnsManagement,
                    }}
                    slotProps={{columnsManagement: {onRestoreDefaults: restoreDefaultColumns}}}
                    getRowHeight={dynamicRowHeight ? () => 'auto' : undefined}
                    getEstimatedRowHeight={dynamicRowHeight ? () => 80 : undefined}
                    rowHeight={props.rowHeight}
                />
            </Box>

            {props.menuItems && (
                <Menu
                    anchorEl={menuAnchorElement}
                    open={menuAnchorElement !== null}
                    onClose={() => setMenuAnchorElement(null)}
                >
                    {props.menuItems.map((item) => (
                        <MenuItem
                            onClick={() => {
                                item.onClick();
                                setMenuAnchorElement(null);
                            }}
                        >
                            {item.icon}
                            {item.label}
                        </MenuItem>
                    ))}
                </Menu>
            )}
        </Box>
    );
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
    const page = isNaN(pageFromUrl) || pageFromUrl < 1 ? 0 : pageFromUrl - 1;

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
    backgroundColor: theme.palette.background.paper,
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
