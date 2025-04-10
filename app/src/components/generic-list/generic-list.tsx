import {DataGrid, GridSortModel} from '@mui/x-data-grid';
import {Box, CircularProgress, Menu, MenuItem, Tab, Tabs} from '@mui/material';
import React, {useEffect, useMemo, useReducer, useRef, useState} from 'react';
import {TextFieldComponent} from '../text-field/text-field-component';
import SearchOutlinedIcon from '@mui/icons-material/SearchOutlined';
import {IconButton} from '../icon-button/icon-button';
import ZoomOutMapOutlinedIcon from '@mui/icons-material/ZoomOutMapOutlined';
import ZoomInMapOutlinedIcon from '@mui/icons-material/ZoomInMapOutlined';
import MoreVertOutlinedIcon from '@mui/icons-material/MoreVertOutlined';
import {useApi} from '../../hooks/use-api';
import {Page} from '../../models/dtos/page';
import {isStringNotNullOrEmpty} from '../../utils/string-utils';
import {GenericListRowModel} from './generic-list-row-models';
import {Actions} from '../actions/actions';
import {withAsyncWrapper} from '../../utils/with-async-wrapper';
import {GenericListProps} from './generic-list-props';
import CloseIcon from "@mui/icons-material/Close";
import {useSearchParams} from "react-router-dom";

export function GenericList<ItemType extends GenericListRowModel, FilterOption extends string | void = void>(props: GenericListProps<ItemType>) {
    const api = useApi();
    const [searchParams, setSearchParams] = useSearchParams();

    const [isFullWidth, toggleIsFullWidth] = useReducer((isFullWidth) => !isFullWidth, false);
    const [menuAnchorElement, setMenuAnchorElement] = useState<null | HTMLElement>(null);
    const [isBusy, setIsBusy] = useState(false);

    const [items, setItems] = useState<Page<ItemType>>();
    const abortControllerRef = useRef<AbortController | null>(null);

    // Sorting Model - Default is retrieved from URL params or prop
    const [sortModel, setSortModel] = useState<GridSortModel>(() => {
        const sortField = searchParams.get("sort") ||
            (typeof props.defaultSortField === "string" ? props.defaultSortField : undefined);
        const sortOrder: "asc" | "desc" = searchParams.get("order") === "desc" ? "desc" : "asc";

        return sortField ? [{ field: sortField, sort: sortOrder }] : [];
    });

    const [currentPage, setCurrentPage] = useState(() => {
        const pageFromUrl = Number(searchParams.get("page"));
        return isNaN(pageFromUrl) || pageFromUrl < 1 ? 0 : pageFromUrl - 1;
    });

    const [pageSize, setPageSize] = useState(() => Number(searchParams.get("size")) || 12);
    const [search, setSearch] = useState(() => searchParams.get("search") || "");
    const [currentFilter, setCurrentFilter] = useState<FilterOption | undefined>(() =>
        (searchParams.get("filter") as FilterOption) || undefined
    );


    const [isFirstUpdate, setIsFirstUpdate] = useState(true); // Tracks first URL update

    // Synchronize internal state with URL parameters
    useEffect(() => {
        const sortField = searchParams.get("sort") ||
            (typeof props.defaultSortField === "string" ? props.defaultSortField : undefined);
        const sortOrder: "asc" | "desc" = searchParams.get("order") === "desc" ? "desc" : "asc";

        setSortModel(sortField ? [{ field: sortField, sort: sortOrder }] : []);
        setCurrentPage(() => {
            const pageFromUrl = Number(searchParams.get("page"));
            return isNaN(pageFromUrl) || pageFromUrl < 1 ? 0 : pageFromUrl - 1;
        });
        setPageSize(Number(searchParams.get("size")) || 12);
        setSearch(searchParams.get("search") || "");
        setCurrentFilter(searchParams.get("filter") as FilterOption);
    }, [searchParams]);

    // Updates URL parameters when state changes
    const updateUrlParams = () => {
        const params = new URLSearchParams();

        // Include search and filter only if set
        if (search) params.set("search", search);
        if (currentFilter) params.set("filter", currentFilter);

        // Ensure sorting is always included for consistency
        const sortField = sortModel.length > 0
            ? sortModel[0].field
            : (typeof props.defaultSortField === "string" ? props.defaultSortField : undefined);

        if (sortField) {
            params.set("sort", sortField);
        }

        if (sortModel.length > 0) {
            params.set("order", sortModel[0].sort ?? "asc");
        }

        if (currentPage !== 0 || search || currentFilter) {
            params.set("page", (currentPage + 1).toString());
        }

        params.set("size", pageSize.toString());

        // Compare the current URL with the new parameters to avoid unnecessary updates
        const newUrl = params.toString();
        const currentUrl = searchParams.toString();

        if (newUrl !== currentUrl) {

            if (search) {
                setSearchParams(params, {replace: true}); // Avoids creating an extra history entry for every debounced search update
            } else if (isFirstUpdate) {
                setSearchParams(params, { replace: true }); // Avoids creating an extra history entry on first load
                setIsFirstUpdate(false);
            } else {
                setSearchParams(params); // Normal push for subsequent changes
            }
        }
    };

    useEffect(() => {
        updateUrlParams();
    }, [sortModel, currentPage, pageSize, search, currentFilter]);

    // Fetch data on dependency changes
    useEffect(() => {
        setIsBusy(true);

        if (abortControllerRef.current) {
            abortControllerRef.current.abort();
        }

        const controller = new AbortController();
        abortControllerRef.current = controller;

        let sort: string | undefined = props.defaultSortField as string | undefined;
        let direction: 'ASC' | 'DESC' | undefined = 'ASC';

        if (sortModel != null && sortModel.length > 0) {
            sort = sortModel[0].field;
            direction = sortModel[0].sort === 'asc' ? 'ASC' : 'DESC';
        } else if (sort) {
            setSortModel([{ field: sort, sort: direction === 'ASC' ? 'asc' : 'desc' }]);
        }

        withAsyncWrapper({
            desiredMinRuntime: 800,
            main: () => props.fetch({
                api: api,
                search: isStringNotNullOrEmpty(search) ? search : undefined,
                page: currentPage < 0 ? 0 : currentPage,
                size: pageSize,
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
    }, [api, currentFilter, sortModel, currentPage, pageSize, search]);

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
        const columns = props
            .columnDefinitions
            .filter(column => !column.onlyFullScreen || isFullWidth);

        if (props.rowActions != null) {
            columns.push({
                field: 'actions',
                headerName: '',
                sortable: false,
                // dynamic width calculation would result in a layout shift of the table, so we use a prop
                width: props.rowActionsCount ? (props.rowActionsCount * 42) + 26: (4 * 42) + 26,
                renderCell: (params) => {
                    if (props.rowActions == null) {
                        return null;
                    }

                    return (
                        <Actions
                            actions={props.rowActions(params.row)}
                            sx={{
                                marginLeft: 'auto',
                            }}
                        />
                    );
                },
            });
        }

        if (props.rowMenuItems != null) {

        }

        return columns;
    }, [props.columnDefinitions, isFullWidth]);

    const showTopControls =
        ((props.filters != null && props.filters.length > 0) || props.disableFullWidthToggle !== true)

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
                            onChange={(_, newValue) => setCurrentFilter(newValue)}
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
                <Box
                    sx={{
                        flex: 1,
                        padding: '4px 0 12px 0',
                    }}
                >
                    <TextFieldComponent
                        label={props.searchLabel}
                        value={search}
                        onChange={val => setSearch(val ?? '')}
                        placeholder={props.searchPlaceholder}
                        startIcon={<SearchOutlinedIcon />}
                        endAction={
                            search
                                ? {
                                    onClick: () => setSearch(''),
                                    tooltip: 'Suche zurücksetzen',
                                    icon: <CloseIcon sx={{ fontSize: 20 }} />,
                                }
                                : undefined
                        }
                        debounce={1000}
                        size={'small'}
                    />
                </Box>

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

            <Box>
                <DataGrid
                    columns={columnDefinitions}
                    getRowId={props.getRowIdentifier}
                    rows={items?.content ?? []}
                    page={currentPage}
                    onPageChange={(newPage) => setCurrentPage(newPage)}
                    pageSize={pageSize}
                    onPageSizeChange={(newPageSize) => setPageSize(newPageSize)}
                    rowsPerPageOptions={[12, 24, 48, 96]}
                    loading={isBusy}
                    rowCount={items?.totalElements ?? 0}
                    pagination={true}
                    disableSelectionOnClick={true}
                    paginationMode="server"
                    filterMode="server"
                    sortingMode="server"
                    disableColumnMenu={true}
                    sortModel={sortModel}
                    onSortModelChange={(newSortModel) => {
                        if (newSortModel.length === 0 && sortModel.length > 0) {
                            // If sorting is removed, revert to default if available
                            if (typeof props.defaultSortField === "string") {
                                setSortModel([{ field: props.defaultSortField, sort: "asc" }]);
                            } else {
                                setSortModel([]);
                            }
                        } else {
                            setSortModel(newSortModel);
                        }
                    }}
                    autoHeight={true}
                    sx={{
                        width: '100%',
                        borderRadius: 0,
                        borderLeft: 'none',
                        borderRight: 'none',
                        "& .MuiDataGrid-columnHeader:first-of-type, & .MuiDataGrid-cell:first-of-type": {
                            paddingLeft: "16px",
                        },
                        "& .MuiDataGrid-columnHeader:last-of-type, & .MuiDataGrid-cell:last-of-type": {
                            paddingRight: "16px",
                        },
                        "& .MuiDataGrid-cell:focus, & .MuiDataGrid-columnHeader:focus": {
                            outline: "none",
                        },
                        "& .MuiDataGrid-cell:focus-within, & .MuiDataGrid-columnHeader:focus-within": {
                            outline: "none",
                        },
                        "& .MuiDataGrid-columnHeaders": {
                            backgroundColor: "rgba(20, 38, 56, 0.06)",
                        },
                        "& .MuiDataGrid-columnHeaders .MuiDataGrid-columnHeader:last-of-type .MuiDataGrid-columnSeparator": {
                            display: "none",
                        }
                    }}
                    components={{
                        LoadingOverlay: () => (
                            <Box
                                sx={{
                                    position: 'absolute',
                                    width: '100%',
                                    height: '100%',
                                    display: 'flex',
                                    justifyContent: 'center',
                                    alignItems: 'center',
                                    backgroundColor: 'rgba(255, 255, 255, 0.5)',
                                    backdropFilter: 'blur(0.5em)',
                                    zIndex: 1000,
                                }}
                            >
                                <CircularProgress />
                            </Box>
                        ),
                        NoRowsOverlay: () => (
                            <Box
                                sx={{
                                    width: '100%',
                                    height: '100%',
                                    display: 'flex',
                                    justifyContent: 'center',
                                    alignItems: 'center',
                                }}
                            >
                                {
                                    isStringNotNullOrEmpty(search) ?
                                        (props.noSearchResultsPlaceholder ?? 'Keine Suchergebnisse gefunden.') :
                                        (props.noDataPlaceholder ?? 'Keine Daten vorhanden.')
                                }
                            </Box>
                        ),
                    }}
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