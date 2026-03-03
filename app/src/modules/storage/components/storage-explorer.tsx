import React, {type ReactNode, useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {
    Box,
    Breadcrumbs,
    Button,
    Chip,
    CircularProgress,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Divider,
    Grid,
    IconButton,
    InputAdornment,
    List,
    ListItemButton,
    ListItemIcon,
    ListItemText,
    Paper,
    Stack,
    type SxProps,
    TextField,
    type Theme,
    Tooltip,
    Typography,
} from '@mui/material';
import {alpha} from '@mui/material/styles';
import {DataGrid, type GridColDef, type GridRenderCellParams} from '@mui/x-data-grid';
import Fuse from 'fuse.js';
import SimpleBar from 'simplebar-react';
import FolderOutlinedIcon from '@mui/icons-material/FolderOutlined';
import DownloadOutlinedIcon from '@mui/icons-material/DownloadOutlined';
import HomeOutlinedIcon from '@mui/icons-material/HomeOutlined';
import ArrowUpwardOutlinedIcon from '@mui/icons-material/ArrowUpwardOutlined';
import SearchOutlinedIcon from '@mui/icons-material/SearchOutlined';
import ClearOutlinedIcon from '@mui/icons-material/ClearOutlined';
import ChevronRightOutlinedIcon from '@mui/icons-material/ChevronRightOutlined';
import ExpandMoreOutlinedIcon from '@mui/icons-material/ExpandMoreOutlined';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import CloseOutlinedIcon from '@mui/icons-material/CloseOutlined';
import ContentCopyOutlinedIcon from '@mui/icons-material/ContentCopyOutlined';
import {StorageProvidersApiService} from '../storage-providers-api-service';
import {type StorageIndexItem} from '../entities/storage-index-item-entity';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar, showErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {getFileTypeIcon} from '../../../utils/file-type-icon';
import {type StorageProviderEntity} from '../entities/storage-provider-entity';
import {humanizeFileSize} from '../../../utils/humanization-utils';

interface StorageExplorerProps {
    providerId: number;
    filterMimeTypes?: string[];
    allowFileDownload?: boolean;
    showContainerBorder?: boolean;
    showTopNavigationBar?: boolean;
    minGridHeight?: number;
    sx?: SxProps<Theme>;
}

const ROOT_PATH = '/';

function normalizeDirectoryPath(path: string): string {
    if (path.trim().length === 0 || path === ROOT_PATH) {
        return ROOT_PATH;
    }

    let normalized = path;
    if (!normalized.startsWith('/')) {
        normalized = `/${normalized}`;
    }

    if (!normalized.endsWith('/')) {
        normalized = `${normalized}/`;
    }

    return normalized;
}

function isDirectory(item: StorageIndexItem): boolean {
    if (item.directory != null) {
        return item.directory;
    }
    if (item.isDirectory != null) {
        return item.isDirectory;
    }
    return item.pathFromRoot.endsWith('/');
}

function formatDateTime(dateString: string): string {
    if (dateString.trim().length === 0) {
        return 'Unbekannt';
    }

    const normalized = dateString.replace(/(\.\d{3})\d+/, '$1');
    const date = new Date(normalized);

    if (Number.isNaN(date.getTime())) {
        return dateString;
    }

    const formatted = new Intl.DateTimeFormat('de-DE', {
        dateStyle: 'medium',
        timeStyle: 'short',
    }).format(date);

    return `${formatted} Uhr`;
}

function getFolderPath(pathFromRoot: string): string {
    const normalized = pathFromRoot.trim();
    if (normalized.length === 0 || normalized === ROOT_PATH) {
        return ROOT_PATH;
    }

    const slashIndex = normalized.lastIndexOf('/');
    if (slashIndex <= 0) {
        return ROOT_PATH;
    }

    const folder = normalized.slice(0, slashIndex + 1);
    return folder.length > 0 ? folder : ROOT_PATH;
}

function useSyncedColumnHeight(
    ref: React.MutableRefObject<HTMLDivElement | null>,
    fallbackHeight: number,
): number {
    // Keep both columns visually aligned while allowing only the tree panel to scroll internally.
    const [height, setHeight] = useState<number>(fallbackHeight);
    const heightRef = useRef<number>(fallbackHeight);
    const resizeFrameRef = useRef<number | null>(null);

    useEffect(() => {
        const target = ref.current;
        if (target == null) {
            return;
        }

        const initialHeight = Math.round(target.getBoundingClientRect().height);
        if (initialHeight > 0) {
            heightRef.current = initialHeight;
            setHeight(initialHeight);
        }

        const observer = new ResizeObserver((entries) => {
            const nextHeight = entries[0]?.contentRect.height;
            if (nextHeight == null || nextHeight <= 0) {
                return;
            }

            const roundedHeight = Math.round(nextHeight);
            if (roundedHeight === heightRef.current) {
                return;
            }

            if (resizeFrameRef.current != null) {
                cancelAnimationFrame(resizeFrameRef.current);
            }

            resizeFrameRef.current = requestAnimationFrame(() => {
                if (roundedHeight === heightRef.current) {
                    return;
                }
                heightRef.current = roundedHeight;
                setHeight(roundedHeight);
                resizeFrameRef.current = null;
            });
        });

        observer.observe(target);
        return () => {
            observer.disconnect();
            if (resizeFrameRef.current != null) {
                cancelAnimationFrame(resizeFrameRef.current);
                resizeFrameRef.current = null;
            }
        };
    }, [ref]);

    useEffect(() => {
        if (heightRef.current === fallbackHeight) {
            return;
        }
        heightRef.current = fallbackHeight;
        setHeight(fallbackHeight);
    }, [fallbackHeight]);

    return height;
}

export function StorageExplorer(props: StorageExplorerProps): ReactNode {
    const {
        providerId,
        filterMimeTypes,
        allowFileDownload = false,
        showContainerBorder = false,
        showTopNavigationBar = false,
        minGridHeight = 706,
        sx,
    } = props;

    const dispatch = useAppDispatch();
    const api = useMemo(() => new StorageProvidersApiService(), []);

    const [provider, setProvider] = useState<StorageProviderEntity>();
    const [currentPath, setCurrentPath] = useState<string>(ROOT_PATH);
    const [currentFolder, setCurrentFolder] = useState<StorageIndexItem[]>([]);

    const [search, setSearch] = useState('');
    const [isLoading, setIsLoading] = useState(true);
    const [isDownloading, setIsDownloading] = useState(false);

    // Keep content during close transition to avoid dialog flicker before unmount.
    const [dialogItem, setDialogItem] = useState<StorageIndexItem>();
    const [isDialogOpen, setIsDialogOpen] = useState(false);

    const [folderCache, setFolderCache] = useState<Record<string, StorageIndexItem[]>>({});
    const [expandedPaths, setExpandedPaths] = useState<string[]>([ROOT_PATH]);
    const [treeLoadingPaths, setTreeLoadingPaths] = useState<Record<string, boolean>>({});
    const [probedPaths, setProbedPaths] = useState<Record<string, boolean>>({});
    const treeItemGapPx = 5;
    const treeItemHeightPx = 40;
    const isRootPath = currentPath === ROOT_PATH;
    const rightColumnContentRef = useRef<HTMLDivElement | null>(null);
    // Keep the tree column constrained on first paint, before ResizeObserver reports.
    const structureHeightFallback = minGridHeight + 60;
    const structureColumnHeight = useSyncedColumnHeight(rightColumnContentRef, structureHeightFallback);

    // Tree is loaded on demand per expanded folder and cached to avoid repeat requests.
    const loadTreeChildren = useCallback((path: string): void => {
        const normalizedPath = normalizeDirectoryPath(path);
        if (treeLoadingPaths[normalizedPath] === true || folderCache[normalizedPath] != null) {
            return;
        }

        setTreeLoadingPaths((prevLoading) => ({
            ...prevLoading,
            [normalizedPath]: true,
        }));

        setProbedPaths((prev) => ({
            ...prev,
            [normalizedPath]: true,
        }));

        void api
            .getFolder(providerId, normalizedPath)
            .then((items) => {
                setFolderCache((prev) => ({
                    ...prev,
                    [normalizedPath]: items.filter((item) => isDirectory(item)),
                }));
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Beim Abrufen der Ordnerstruktur ist ein Fehler aufgetreten.'));
            })
            .finally(() => {
                setTreeLoadingPaths((prev) => ({
                    ...prev,
                    [normalizedPath]: false,
                }));
            });
    }, [api, dispatch, folderCache, providerId, treeLoadingPaths]);

    useEffect(() => {
        setCurrentPath(ROOT_PATH);
        setDialogItem(undefined);
        setFolderCache({});
        setExpandedPaths([ROOT_PATH]);
        setProbedPaths({});
        setSearch('');

        api
            .retrieve(providerId)
            .then((loadedProvider) => {
                setProvider(loadedProvider);
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Der Speicheranbieter konnte nicht geladen werden.'));
            });
    }, [api, dispatch, providerId]);

    useEffect(() => {
        setIsLoading(true);
        const normalizedPath = normalizeDirectoryPath(currentPath);

        api
            .getFolder(providerId, normalizedPath)
            .then((items) => {
                setCurrentFolder(items);
                setFolderCache((prev) => ({
                    ...prev,
                    [normalizedPath]: items.filter((item) => isDirectory(item)),
                }));
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Beim Abrufen des Ordners ist ein Fehler aufgetreten.'));
            })
            .finally(() => {
                setIsLoading(false);
            });
    }, [api, currentPath, dispatch, providerId]);

    const filteredByMime = useMemo(() => {
        const normalizedFilter = filterMimeTypes?.map((mimeType) => mimeType.toLowerCase()) ?? [];

        return currentFolder.filter((item) => {
            if (isDirectory(item)) {
                return true;
            }

            if (normalizedFilter.length === 0) {
                return true;
            }

            const mimeType = item.mimeType?.toLowerCase() ?? '';
            if (mimeType.length === 0) {
                return true;
            }

            return normalizedFilter.includes(mimeType);
        });
    }, [currentFolder, filterMimeTypes]);

    // Visible rows pipeline: MIME filtering -> fuzzy search -> directory-first alphabetic ordering.
    const rows = useMemo(() => {
        const trimmedSearch = search.trim();

        let searchableItems = filteredByMime;
        if (trimmedSearch.length > 0) {
            const fuse = new Fuse(filteredByMime, {
                includeScore: true,
                threshold: 0.38,
                ignoreLocation: true,
                minMatchCharLength: 2,
                keys: [
                    {name: 'filename', weight: 0.7},
                    {name: 'pathFromRoot', weight: 0.2},
                    {name: 'mimeType', weight: 0.1},
                ],
            });
            searchableItems = fuse.search(trimmedSearch).map((entry) => entry.item);
        }

        return [...searchableItems]
            .sort((a, b) => {
                const aIsDirectory = isDirectory(a);
                const bIsDirectory = isDirectory(b);

                if (aIsDirectory && !bIsDirectory) {
                    return -1;
                }
                if (!aIsDirectory && bIsDirectory) {
                    return 1;
                }

                return a.filename.localeCompare(b.filename, 'de');
            });
    }, [filteredByMime, search]);

    const folderCount = rows.filter((item) => isDirectory(item)).length;
    const fileCount = rows.length - folderCount;

    const breadcrumbParts = useMemo(() => currentPath
        .split('/')
        .filter((part) => part.length > 0), [currentPath]);

    const parentPath = useMemo(() => {
        if (currentPath === ROOT_PATH) {
            return ROOT_PATH;
        }

        const parts = currentPath.split('/').filter((part) => part.length > 0);
        if (parts.length <= 1) {
            return ROOT_PATH;
        }

        return normalizeDirectoryPath(`/${parts.slice(0, -1).join('/')}`);
    }, [currentPath]);

    const columns = useMemo<GridColDef<StorageIndexItem>[]>(() => [
        {
            field: 'filename',
            headerName: 'Name',
            flex: 1,
            minWidth: 280,
            sortable: true,
            renderCell: (params: GridRenderCellParams<StorageIndexItem, string>) => {
                const item = params.row;
                const itemIsDirectory = isDirectory(item);

                return (
                    <Stack
                        direction="row"
                        spacing={1}
                        alignItems="center"
                        sx={{
                            width: '100%',
                            minWidth: 0,
                            height: '100%',
                        }}
                    >
                        <Box sx={{display: 'inline-flex', alignItems: 'center', color: 'text.secondary'}}>
                            {itemIsDirectory
                                ? <FolderOutlinedIcon fontSize="small"/>
                                : getFileTypeIcon(item.mimeType, {fontSize: 'small'})}
                        </Box>

                        <Typography
                            variant="body2"
                            noWrap={true}
                            title={item.filename}
                            sx={{
                                minWidth: 0,
                                flex: 1,
                                whiteSpace: 'nowrap',
                                overflow: 'hidden',
                                textOverflow: 'ellipsis',
                            }}
                        >
                            {item.filename}
                        </Typography>

                        {item.missing && (
                            <Chip
                                label="Fehlend"
                                size="small"
                                color="warning"
                                variant="outlined"
                            />
                        )}
                    </Stack>
                );
            },
        },
        {
            field: 'mimeType',
            headerName: 'Typ',
            minWidth: 180,
            flex: 0.5,
            valueGetter: (_, row) => {
                if (isDirectory(row)) {
                    return 'Ordner';
                }
                return row.mimeType || 'Unbekannt';
            },
        },
        {
            field: 'sizeInBytes',
            headerName: 'Größe',
            minWidth: 120,
            flex: 0.22,
            align: 'right',
            headerAlign: 'right',
            valueGetter: (_, row) => {
                if (isDirectory(row)) {
                    return '-';
                }
                return humanizeFileSize(row.sizeInBytes);
            },
            sortable: false,
        },
        {
            field: 'updated',
            headerName: 'Aktualisiert',
            minWidth: 190,
            flex: 0.36,
            valueGetter: (_, row) => formatDateTime(row.updated),
        },
    ], []);

    const metadataEntries = useMemo(() => {
        if (dialogItem?.metadata == null) {
            return [];
        }

        return Object.entries(dialogItem.metadata)
            .filter(([, value]) => value != null && String(value).trim().length > 0);
    }, [dialogItem]);
    const dialogFolderPath = useMemo(() => {
        if (dialogItem == null) {
            return ROOT_PATH;
        }
        return getFolderPath(dialogItem.pathFromRoot);
    }, [dialogItem]);

    const isExpanded = (path: string): boolean => expandedPaths.includes(path);

    const toggleFolder = (path: string): void => {
        const normalizedPath = normalizeDirectoryPath(path);

        if (isExpanded(normalizedPath)) {
            setExpandedPaths((prev) => prev.filter((entry) => entry !== normalizedPath));
            return;
        }

        setExpandedPaths((prev) => [...prev, normalizedPath]);
        loadTreeChildren(normalizedPath);
    };

    const navigateToFolder = (path: string): void => {
        setCurrentPath(normalizeDirectoryPath(path));
    };

    const openFileDialog = (item: StorageIndexItem): void => {
        setDialogItem(item);
        setIsDialogOpen(true);
    };

    const closeFileDialog = (): void => {
        setIsDialogOpen(false);
    };

    const copyToClipboard = async (value: string, label: string): Promise<void> => {
        try {
            await navigator.clipboard.writeText(value);
            dispatch(showSuccessSnackbar(`${label} wurde kopiert.`));
        } catch (error) {
            console.error(error);
            dispatch(showErrorSnackbar(`${label} konnte nicht kopiert werden.`));
        }
    };
    const renderCopyValue = (label: string, value: string, copyLabel: string): ReactNode => (
        <>
            <Grid size={{xs: 12, sm: 4}}>
                <Typography
                    variant="body2"
                    color="text.secondary"
                >{label}</Typography>
            </Grid>
            <Grid size={{xs: 12, sm: 8}}>
                <Stack
                    direction="row"
                    spacing={0.5}
                    alignItems="center"
                >
                    <Typography
                        variant="body2"
                        title={value}
                        noWrap={true}
                        sx={{minWidth: 0, flex: 1, overflow: 'hidden', textOverflow: 'ellipsis'}}
                    >
                        {value}
                    </Typography>
                    <Tooltip
                        title={`${copyLabel} kopieren`}
                        arrow={true}
                    >
                        <IconButton
                            size="small"
                            sx={{flexShrink: 0}}
                            onClick={() => {
                                void copyToClipboard(value, copyLabel);
                            }}
                        >
                            <ContentCopyOutlinedIcon fontSize="small"/>
                        </IconButton>
                    </Tooltip>
                </Stack>
            </Grid>
        </>
    );

    const renderExpandButton = (path: string): ReactNode => {
        const normalizedPath = normalizeDirectoryPath(path);
        const isLoadingPath = treeLoadingPaths[normalizedPath] === true;
        const cachedChildren = folderCache[normalizedPath];
        const hasChildren = (cachedChildren?.length ?? 0) > 0;
        const wasProbed = probedPaths[normalizedPath] === true;

        return (
            <Box
                sx={{
                    width: 28,
                    minWidth: 28,
                    display: 'inline-flex',
                    justifyContent: 'center',
                    alignItems: 'center',
                }}
            >
                {isLoadingPath ? (
                    <CircularProgress size={13}/>
                ) : ((cachedChildren != null || wasProbed) && !hasChildren) ? (
                    <Tooltip
                        title="Keine Unterordner gefunden"
                        arrow={true}
                    >
                        <span>
                            <IconButton
                                size="small"
                                disabled={true}
                                sx={{opacity: 0.45, p: 0.25}}
                            >
                                <ChevronRightOutlinedIcon fontSize="small"/>
                            </IconButton>
                        </span>
                    </Tooltip>
                ) : (
                    <Tooltip
                        title={isExpanded(normalizedPath) ? 'Ordner einklappen' : 'Unterordner laden'}
                        arrow={true}
                    >
                        <IconButton
                            size="small"
                            onClick={(event) => {
                                event.stopPropagation();
                                toggleFolder(normalizedPath);
                            }}
                            sx={{p: 0.25}}
                        >
                            {isExpanded(normalizedPath)
                                ? <ExpandMoreOutlinedIcon fontSize="small"/>
                                : <ChevronRightOutlinedIcon fontSize="small"/>}
                        </IconButton>
                    </Tooltip>
                )}
            </Box>
        );
    };

    const renderTree = (path: string, depth: number): ReactNode => {
        const children = folderCache[normalizeDirectoryPath(path)] ?? [];
        if (children.length === 0) {
            return null;
        }

        return children.map((folder) => {
            const pathFromRoot = normalizeDirectoryPath(folder.pathFromRoot);

            return (
                <Box
                    key={pathFromRoot}
                    sx={{mb: `${treeItemGapPx}px`}}
                >
                    <ListItemButton
                        dense={false}
                        selected={currentPath === pathFromRoot}
                        onClick={() => {
                            navigateToFolder(pathFromRoot);
                        }}
                        sx={{
                            pl: 0.75 + depth * 2.1,
                            pr: 0.5,
                            py: 0.5,
                            borderRadius: 1,
                            minHeight: treeItemHeightPx,
                        }}
                    >
                        {renderExpandButton(pathFromRoot)}

                        <ListItemIcon sx={{minWidth: 26}}>
                            <FolderOutlinedIcon fontSize="small"/>
                        </ListItemIcon>
                        <ListItemText
                            primary={(
                                <Typography
                                    variant="body2"
                                    title={folder.filename}
                                    sx={{
                                        display: 'block',
                                        whiteSpace: 'nowrap',
                                        overflow: 'hidden',
                                        textOverflow: 'ellipsis',
                                        maxWidth: '100%',
                                    }}
                                >
                                    {folder.filename}
                                </Typography>
                            )}
                        />
                    </ListItemButton>

                    {isExpanded(pathFromRoot) && renderTree(pathFromRoot, depth + 1)}
                </Box>
            );
        });
    };

    const dataGridSx = useMemo<SxProps<Theme>>(() => ({
        minHeight: minGridHeight,
        borderRadius: 1,
        '& .MuiDataGrid-columnHeader': {
            alignItems: 'center',
            backgroundColor: 'transparent',
        },
        '& .MuiDataGrid-columnHeaders': {
            backgroundColor: (theme: Theme) => alpha(theme.palette.primary.main, theme.palette.action.selectedOpacity),
        },
        '& .MuiDataGrid-columnHeader .MuiDataGrid-columnSeparator': {
            color: 'rgba(20, 38, 56, 0.2)',
        },
        '& .MuiDataGrid-cell': {
            display: 'flex',
            alignItems: 'center',
        },
        '& .MuiDataGrid-footerContainer': {
            minHeight: treeItemHeightPx,
            height: treeItemHeightPx,
        },
        '& .MuiTablePagination-root, & .MuiTablePagination-toolbar': {
            minHeight: treeItemHeightPx,
            height: treeItemHeightPx,
        },
        '& .MuiDataGrid-row': {
            cursor: 'pointer',
        },
        '& .MuiDataGrid-row:last-of-type .MuiDataGrid-cell': {
            borderBottom: '1px solid',
            borderBottomColor: 'divider',
        },
        '& .MuiDataGrid-main': {
            overflow: 'hidden',
        },
        '& .MuiDataGrid-scrollbar--horizontal': {
            bottom: 0,
        },
        '& .MuiDataGrid-cell:focus, & .MuiDataGrid-cell:focus-within, & .MuiDataGrid-columnHeader:focus, & .MuiDataGrid-columnHeader:focus-within': {
            outline: 'none',
        },
    }), [minGridHeight, treeItemHeightPx]);

    if (provider == null) {
        return null;
    }

    const content = (
        <Stack
            spacing={2}
            sx={sx}
        >
            {showTopNavigationBar && (
                <Stack
                    direction="row"
                    spacing={0.5}
                    alignItems="center"
                    sx={{
                        px: 1,
                        py: 0.5,
                        border: '1px solid',
                        borderColor: 'divider',
                        borderRadius: 1,
                    }}
                >
                    <Tooltip
                        title="Zum Wurzelordner"
                        arrow={true}
                    >
                        <span>
                            <IconButton
                                size="small"
                                onClick={() => {
                                    navigateToFolder(ROOT_PATH);
                                }}
                                disabled={isRootPath}
                            >
                                <HomeOutlinedIcon fontSize="small"/>
                            </IconButton>
                        </span>
                    </Tooltip>

                    <Tooltip
                        title="Eine Ebene nach oben"
                        arrow={true}
                    >
                        <span>
                            <IconButton
                                size="small"
                                onClick={() => {
                                    navigateToFolder(parentPath);
                                }}
                                disabled={isRootPath}
                            >
                                <ArrowUpwardOutlinedIcon fontSize="small"/>
                            </IconButton>
                        </span>
                    </Tooltip>

                    <Divider
                        orientation="vertical"
                        flexItem={true}
                        sx={{mx: 0.5}}
                    />

                    <Breadcrumbs
                        separator="›"
                        aria-label="Ordnerpfad"
                        maxItems={6}
                        itemsBeforeCollapse={2}
                        itemsAfterCollapse={2}
                        sx={{
                            flexWrap: 'nowrap',
                            overflow: 'hidden',
                            '& .MuiBreadcrumbs-ol': {
                                flexWrap: 'nowrap',
                                overflow: 'hidden',
                            },
                        }}
                    >
                        <Button
                            size="small"
                            onClick={() => {
                                navigateToFolder(ROOT_PATH);
                            }}
                            sx={{minWidth: 'auto', px: 0.75}}
                        >
                            {provider.name}
                        </Button>

                        {breadcrumbParts.map((part, index) => {
                            const fullPath = normalizeDirectoryPath(`/${breadcrumbParts.slice(0, index + 1).join('/')}`);

                            return (
                                <Button
                                    key={fullPath}
                                    size="small"
                                    onClick={() => {
                                        navigateToFolder(fullPath);
                                    }}
                                    sx={{
                                        minWidth: 'auto',
                                        px: 0.75,
                                        maxWidth: 220,
                                        overflow: 'hidden',
                                    }}
                                    title={part}
                                >
                                    <Typography
                                        variant="body2"
                                        noWrap={true}
                                        sx={{
                                            width: '100%',
                                            maxWidth: '100%',
                                            overflow: 'hidden',
                                            textOverflow: 'ellipsis',
                                            whiteSpace: 'nowrap',
                                        }}
                                    >
                                        {part}
                                    </Typography>
                                </Button>
                            );
                        })}
                    </Breadcrumbs>
                </Stack>
            )}

            <Grid container
                  sx={{alignItems: 'flex-start'}}>
                <Grid
                    size={{xs: 12, md: 3}}
                    sx={{
                        pr: {xs: 0, md: 2},
                        borderRight: {xs: 'none', md: '1px solid'},
                        borderRightColor: {xs: 'transparent', md: 'divider'},
                        mb: {xs: 2, md: 0},
                        display: 'flex',
                        flexDirection: 'column',
                        height: {xs: 'auto', md: `${structureColumnHeight}px`},
                        maxHeight: {xs: 'none', md: `${structureColumnHeight}px`},
                        overflow: 'hidden',
                    }}
                >
                    <Typography
                        variant="subtitle2"
                        sx={{pt: 2, px: 0.75, pb: 1.75, textTransform: 'uppercase', color: 'text.secondary'}}
                    >Ordnerstruktur</Typography>

                    <Box
                        sx={{
                            flex: 1,
                            minHeight: 0,
                            '& .simplebar-scrollbar:before': {
                                backgroundColor: 'rgba(20, 38, 56, 0.35)',
                                left: '3px',
                                right: '3px',
                            },
                        }}
                    >
                        <SimpleBar
                            style={{
                                height: '100%',
                                overflowX: 'hidden',
                                paddingRight: '2px',
                            }}
                        >
                            <List
                                dense={false}
                                disablePadding={true}
                                sx={{pr: 0.25}}
                            >
                                <ListItemButton
                                    dense={false}
                                    selected={isRootPath}
                                    onClick={() => {
                                        navigateToFolder(ROOT_PATH);
                                    }}
                                    sx={{
                                        pl: 0.75,
                                        pr: 0.5,
                                        py: 0.5,
                                        borderRadius: 1,
                                        minHeight: treeItemHeightPx,
                                        mb: `${treeItemGapPx}px`,
                                    }}
                                >
                                    {renderExpandButton(ROOT_PATH)}
                                    <ListItemIcon sx={{minWidth: 26}}>
                                        <HomeOutlinedIcon fontSize="small"/>
                                    </ListItemIcon>
                                    <ListItemText
                                        primary={(
                                            <Typography
                                                variant="body2"
                                                title={provider.name}
                                                sx={{
                                                    display: 'block',
                                                    fontWeight: 600,
                                                    whiteSpace: 'nowrap',
                                                    overflow: 'hidden',
                                                    textOverflow: 'ellipsis',
                                                    maxWidth: '100%',
                                                }}
                                            >
                                                {provider.name}
                                            </Typography>
                                        )}
                                    />
                                </ListItemButton>

                                {isExpanded(ROOT_PATH) && renderTree(ROOT_PATH, 1)}
                            </List>
                        </SimpleBar>
                    </Box>
                </Grid>

                <Grid
                    size={{xs: 12, md: 9}}
                    sx={{pl: {xs: 0, md: 2}}}
                >
                    <Box ref={rightColumnContentRef}>
                        <Stack
                            direction="row"
                            spacing={1.25}
                            alignItems="center"
                            sx={{mb: 1.5}}
                        >
                            <TextField
                                size="small"
                                placeholder="Dateien oder Ordner suchen"
                                value={search}
                                onChange={(event) => {
                                    setSearch(event.target.value);
                                }}
                                sx={{minWidth: 240, flexGrow: 1}}
                                InputProps={{
                                    startAdornment: (
                                        <InputAdornment position="start">
                                            <SearchOutlinedIcon fontSize="small"/>
                                        </InputAdornment>
                                    ),
                                    endAdornment: search.trim().length > 0 ? (
                                        <InputAdornment position="end">
                                            <Tooltip
                                                title="Suche leeren"
                                                arrow={true}
                                            >
                                                <IconButton
                                                    size="small"
                                                    onClick={() => {
                                                        setSearch('');
                                                    }}
                                                >
                                                    <ClearOutlinedIcon fontSize="small"/>
                                                </IconButton>
                                            </Tooltip>
                                        </InputAdornment>
                                    ) : undefined,
                                }}
                            />
                            <Typography
                                variant="body2"
                                color="text.secondary"
                                sx={{whiteSpace: 'nowrap', pl: 1}}
                            >
                                {folderCount} Ordner, {fileCount} Dateien
                            </Typography>
                        </Stack>

                        <DataGrid
                            rows={rows}
                            columns={columns}
                            getRowId={(row) => row.pathFromRoot}
                            disableRowSelectionOnClick={true}
                            disableColumnMenu={true}
                            density="standard"
                            loading={isLoading}
                            autoHeight={true}
                            columnHeaderHeight={treeItemHeightPx}
                            pageSizeOptions={[12, 24, 48, 96]}
                            initialState={{
                                pagination: {
                                    paginationModel: {
                                        page: 0,
                                        pageSize: 12,
                                    },
                                },
                            }}
                            onRowClick={(params) => {
                                const item = params.row;

                                if (isDirectory(item)) {
                                    navigateToFolder(item.pathFromRoot);
                                    const pathFromRoot = normalizeDirectoryPath(item.pathFromRoot);
                                    setExpandedPaths((prev) => (prev.includes(pathFromRoot) ? prev : [...prev, pathFromRoot]));
                                    loadTreeChildren(item.pathFromRoot);
                                    return;
                                }

                                openFileDialog(item);
                            }}
                            sx={dataGridSx}
                            slots={{
                                noRowsOverlay: () => (
                                    <Stack
                                        alignItems="center"
                                        justifyContent="center"
                                        sx={{height: '100%'}}
                                        spacing={1}
                                    >
                                        <InfoOutlinedIcon color="disabled"/>
                                        <Typography
                                            variant="body2"
                                            color="text.secondary"
                                        >
                                            Dieser Ordner ist leer oder es gibt keine Treffer.
                                        </Typography>
                                    </Stack>
                                ),
                            }}
                        />
                    </Box>
                </Grid>
            </Grid>

            <Dialog
                open={isDialogOpen && dialogItem != null}
                onClose={closeFileDialog}
                maxWidth="sm"
                fullWidth={true}
                TransitionProps={{
                    onExited: () => {
                        setDialogItem(undefined);
                    },
                }}
            >
                <DialogTitle sx={{pr: 6}}>
                    <Stack
                        direction="row"
                        spacing={1.25}
                        alignItems="center"
                    >
                        <Box
                            sx={{
                                width: 42,
                                height: 42,
                                borderRadius: 1,
                                display: 'inline-flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                color: 'text.secondary',
                                bgcolor: 'action.hover',
                                border: '1px solid',
                                borderColor: 'rgba(20, 38, 56, 0.2)',
                                flexShrink: 0,
                            }}
                        >
                            {dialogItem != null && getFileTypeIcon(dialogItem.mimeType, {fontSize: 'medium'})}
                        </Box>
                        <Stack sx={{minWidth: 0}}>
                            <Typography
                                variant="subtitle1"
                                sx={{wordBreak: 'break-word'}}
                            >{dialogItem?.filename}</Typography>
                            <Typography
                                variant="caption"
                                color="text.secondary"
                                sx={{mt: -0.25}}
                            >Datei-Informationen</Typography>
                        </Stack>
                    </Stack>
                    <IconButton
                        onClick={closeFileDialog}
                        size="small"
                        sx={{
                            position: 'absolute',
                            right: 12,
                            top: 12,
                        }}
                    >
                        <CloseOutlinedIcon fontSize="small"/>
                    </IconButton>
                </DialogTitle>

                <DialogContent dividers={true}>
                    {dialogItem != null && (
                        <Stack spacing={2}>
                            <Stack
                                direction="row"
                                spacing={1}
                                useFlexGap={true}
                                flexWrap="wrap"
                            >
                                <Chip
                                    label={`Typ: ${dialogItem.mimeType || 'Unbekannt'}`}
                                    size="small"
                                    variant="outlined"
                                />
                                <Chip
                                    label={`Größe: ${humanizeFileSize(dialogItem.sizeInBytes)}`}
                                    size="small"
                                    variant="outlined"
                                />
                                {dialogItem.missing && <Chip
                                    label="Fehlend"
                                    size="small"
                                    color="warning"
                                    variant="outlined"
                                />}
                            </Stack>

                            <Grid
                                container
                                spacing={1.25}
                            >
                                {renderCopyValue('Ordnerpfad', dialogFolderPath, 'Ordnerpfad')}
                                {renderCopyValue('Dateiname', dialogItem.filename, 'Dateiname')}
                                {renderCopyValue('Vollständiger Pfad', dialogItem.pathFromRoot, 'Vollständiger Pfad')}

                                <Grid size={{xs: 12, sm: 4}}>
                                    <Typography
                                        variant="body2"
                                        color="text.secondary"
                                    >Erstellt am</Typography>
                                </Grid>
                                <Grid size={{xs: 12, sm: 8}}>
                                    <Typography variant="body2">{formatDateTime(dialogItem.created)}</Typography>
                                </Grid>

                                <Grid size={{xs: 12, sm: 4}}>
                                    <Typography
                                        variant="body2"
                                        color="text.secondary"
                                    >Zuletzt aktualisiert</Typography>
                                </Grid>
                                <Grid size={{xs: 12, sm: 8}}>
                                    <Typography variant="body2">{formatDateTime(dialogItem.updated)}</Typography>
                                </Grid>
                            </Grid>

                            {metadataEntries.length > 0 && (
                                <>
                                    <Divider sx={{my: 0.25}}/>
                                    <Typography variant="subtitle2">Metadaten</Typography>
                                    {metadataEntries.map(([key, value]) => (
                                        <Stack
                                            key={key}
                                            direction="row"
                                            spacing={1}
                                            alignItems="flex-start"
                                        >
                                            <Typography
                                                variant="body2"
                                                color="text.secondary"
                                                sx={{minWidth: 120}}
                                            >{key}</Typography>
                                            <Typography
                                                variant="body2"
                                                sx={{wordBreak: 'break-word'}}
                                            >{String(value)}</Typography>
                                        </Stack>
                                    ))}
                                </>
                            )}

                            {!allowFileDownload && (
                                <Box sx={{mt: 2}}>
                                    <Typography
                                        variant="caption"
                                        color="text.secondary"
                                    >
                                        Das Herunterladen ist in dieser Ansicht deaktiviert.
                                    </Typography>
                                </Box>
                            )}
                        </Stack>
                    )}
                </DialogContent>

                <DialogActions sx={{pt: 2}}>
                    <Button
                        variant="contained"
                        startIcon={<DownloadOutlinedIcon/>}
                        disabled={!allowFileDownload || isDownloading || dialogItem == null}
                        onClick={() => {
                            if (!allowFileDownload || dialogItem == null) {
                                return;
                            }

                            setIsDownloading(true);
                            Promise
                                // Keep this for possible future extension for real file downloads.
                                // For now, files should be only downloadable, in their respective context (Asset or Process Instance Attachment)
                                .resolve()
                                .catch((err) => {
                                    dispatch(showApiErrorSnackbar(err, 'Die Datei konnte nicht heruntergeladen werden.'));
                                })
                                .finally(() => {
                                    setIsDownloading(false);
                                });
                        }}
                    >
                        Datei herunterladen
                    </Button>
                    <Button
                        onClick={closeFileDialog}
                    >
                        Schließen
                    </Button>
                </DialogActions>
            </Dialog>
        </Stack>
    );

    if (showContainerBorder) {
        return (
            <Paper
                variant="outlined"
                sx={{p: 1.25}}
            >
                {content}
            </Paper>
        );
    }

    return content;
}
