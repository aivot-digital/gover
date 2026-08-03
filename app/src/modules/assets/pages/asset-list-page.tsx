import {
    GenericListPage,
    type GenericListPagePermissionConfig,
    type GenericListPagePermissionState,
} from '../../../components/generic-list-page/generic-list-page';
import {EmptyDataListPlaceholder} from '../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import {Stack, Typography} from '@mui/material';
import CreateNewFolderOutlined from '@aivot/mui-material-symbols-400-n25-outlined/CreateNewFolder';
import React, {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {CellLink} from '../../../components/cell-link/cell-link';
import {AssetsApiService} from '../assets-api-service';
import {showApiErrorSnackbar, showErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {getFileTypeIcon} from '../../../utils/file-type-icon';
import DriveFolderUploadOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/DriveFolderUpload';
import {getFileTypeLabel} from '../../../utils/file-type-label';
import Chip from '@mui/material/Chip';
import {CellContentWrapper} from '../../../components/cell-content-wrapper/cell-content-wrapper';
import {useParams, useSearchParams, useNavigate} from 'react-router-dom';
import {useApi} from '../../../hooks/use-api';
import {ListControlRef} from '../../../components/generic-list/generic-list-props';
import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import {useConfirm} from '../../../providers/confirm-provider';
import {Breadcrumbs} from '../../../components/breadcrumbs/breadcrumbs';
import {usePrompt} from '../../../providers/prompt-provider';
import {isStringNullOrEmpty} from '../../../utils/string-utils';
import {VStorageIndexItemWithAssetEntity} from '../../storage/entities/storage-index-item-entity';
import {Permission} from '../../../data/permissions/permission';
import {formatInstantInApplicationTimeZone} from '../../../utils/temporal-utils';

const assetListPermissionCheck: GenericListPagePermissionConfig<VStorageIndexItemWithAssetEntity> = {
    scope: {
        type: 'system',
    },
    read: Permission.ASSET_READ,
    create: Permission.ASSET_CREATE,
    update: Permission.ASSET_UPDATE,
};

export function AssetListPage() {
    const navigate = useNavigate();
    const dispatch = useAppDispatch();
    const api = useApi();
    const {storageProviderId} = useParams<{ storageProviderId?: string }>();
    const [searchParams] = useSearchParams();
    const listControlRef = useRef<ListControlRef>(null);

    const confirm = useConfirm();
    const prompt = usePrompt();

    const parsedStorageProviderId = useMemo(() => {
        if (storageProviderId == null) {
            return undefined;
        }

        const parsed = Number.parseInt(storageProviderId, 10);
        if (Number.isNaN(parsed) || parsed <= 0) {
            return undefined;
        }

        return parsed;
    }, [storageProviderId]);

    const [storageProviderName, setStorageProviderName] = useState<string>();
    const [storageProviderReadOnly, setStorageProviderReadOnly] = useState(false);

    const currentFolderPath = useMemo(() => {
        return AssetsApiService.normalizeFolderPath(searchParams.get('path') ?? '/');
    }, [searchParams]);

    useEffect(() => {
        if (parsedStorageProviderId == null) {
            setStorageProviderName(undefined);
            setStorageProviderReadOnly(false);
            return;
        }

        new AssetsApiService()
            .retrieveStorageProvider(parsedStorageProviderId)
            .then((provider) => {
                setStorageProviderName(provider.name);
                setStorageProviderReadOnly(provider.readOnlyStorage);
            })
            .catch((err) => {
                setStorageProviderName(undefined);
                setStorageProviderReadOnly(false);
                dispatch(showApiErrorSnackbar(err, 'Der Speicheranbieter konnte nicht geladen werden.'));
            });
    }, [dispatch, parsedStorageProviderId]);

    const headerTitle = storageProviderName != null && storageProviderName.length > 0
        ? `Dateien & Medien - ${storageProviderName}`
        : 'Dateien & Medien';

    const uploadRoute = parsedStorageProviderId != null
        ? `/assets/providers/${parsedStorageProviderId}/files/new?path=${encodeURIComponent(currentFolderPath)}`
        : '/assets';

    const handleListRefresh = useCallback(() => {
        listControlRef.current?.refresh();
    }, []);

    const handleCreateFolder = useCallback((canCreateAsset: boolean) => {
        if (parsedStorageProviderId == null || !canCreateAsset || storageProviderReadOnly) {
            return;
        }

        prompt({
            title: 'Neuer Ordner',
            message: 'Bitte geben Sie den Namen des neuen Ordners ein.',
            inputLabel: 'Names des Ordners',
            confirmButtonText: 'Ordner anlegen',
        })
            .then((folderName) => {
                if (folderName == null || isStringNullOrEmpty(folderName)) {
                    return null;
                }

                const trimmedFolderName = folderName.trim().replaceAll('/', '');
                if (trimmedFolderName.length === 0) {
                    dispatch(showErrorSnackbar('Bitte geben Sie einen gültigen Ordnernamen ein.'));
                    return null;
                }

                const targetPath = AssetsApiService.normalizeFolderPath(`${currentFolderPath}${trimmedFolderName}/`);

                return new AssetsApiService(api)
                    .createFolder(parsedStorageProviderId, targetPath)
                    .then(() => true);
            })
            .then((result) => {
                if (result == null) {
                    return null;
                }

                dispatch(showSuccessSnackbar('Ordner erfolgreich angelegt.'));
                handleListRefresh();
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Der Ordner konnte nicht angelegt werden.'));
            });
    }, [
        api,
        currentFolderPath,
        dispatch,
        handleListRefresh,
        parsedStorageProviderId,
        prompt,
        storageProviderReadOnly,
    ]);

    const header = useCallback((permissions: GenericListPagePermissionState<VStorageIndexItemWithAssetEntity>) => {
        const createAssetDisabledTooltip = resolveCreateAssetDisabledTooltip(
            permissions,
            parsedStorageProviderId,
            storageProviderReadOnly,
        );
        const canCreateAssetInCurrentProvider = canCreateAssetForCurrentProvider(
            permissions,
            parsedStorageProviderId,
            storageProviderReadOnly,
        );

        return ({
            icon: <DriveFolderUploadOutlinedIcon/>,
            title: headerTitle,
            actions: [
                {
                    icon: <CreateNewFolderOutlined/>,
                    tooltip: 'Neuen Ordner anlegen',
                    disabledTooltip: createAssetDisabledTooltip,
                    disabled: !canCreateAssetInCurrentProvider,
                    onClick: () => handleCreateFolder(permissions.canCreate),
                },
                'separator' as const,
                {
                    label: 'Datei hochladen',
                    icon: <AddOutlinedIcon/>,
                    tooltip: 'Neues Dokument oder Medieninhalt anlegen',
                    disabledTooltip: createAssetDisabledTooltip,
                    disabled: !canCreateAssetInCurrentProvider,
                    to: uploadRoute,
                    variant: 'contained' as const,
                },
            ],
            helpDialog: {
                title: 'Hilfe zu Dokumenten & Medieninhalten',
                tooltip: 'Hilfe anzeigen',
                content: (
                    <>
                        <Typography>
                            Dateien und Medieninhalte sind Dateien, die in der Anwendung hochgeladen und
                            verwaltet werden können.
                            In dieser Oberfläche können Sie die im System verfügbaren Dateien einsehen und
                            bearbeiten.
                        </Typography>
                        <Typography sx={{mt: 2}}>
                            Sie können die hochgeladenen Dateien u.A. in Formularen verwenden, um z.B. Bilder
                            oder PDFs einzubinden.
                            Darüber hinaus können Systemdateien (wie Zertifikate oder Templates) z.B. für die
                            Konfiguration von
                            Zahlungsdienstleistern oder der Dokumentengenerierung genutzt werden.
                        </Typography>
                    </>
                ),
            },
        });
    }, [
        handleCreateFolder,
        headerTitle,
        parsedStorageProviderId,
        storageProviderReadOnly,
        uploadRoute,
    ]);

    const preSearchElements = useMemo(() => [
        <Breadcrumbs
            key={`${currentFolderPath}:${storageProviderName ?? 'all'}`}
            prefix={parsedStorageProviderId != null ? `/assets/providers/${parsedStorageProviderId}` : '/assets'}
            path={currentFolderPath}
            rootLabel={storageProviderName ?? 'Alle Dateien'}
        />,
    ], [currentFolderPath, parsedStorageProviderId, storageProviderName]);

    const fetchAssets = useCallback((options: {
        api: any;
        page: number;
        size: number;
        search?: string;
    }) => {
        if (parsedStorageProviderId == null) {
            return Promise.resolve({
                content: [] as VStorageIndexItemWithAssetEntity[],
                page: {
                    size: options.size,
                    number: options.page,
                    totalElements: 0,
                    totalPages: 0,
                },
            });
        }

        return new AssetsApiService(options.api)
            .listFolderContent(parsedStorageProviderId, currentFolderPath)
            .then((items) => {
                const searchTerm = options.search?.trim().toLowerCase() ?? '';
                const filteredItems = searchTerm.length > 0
                    ? items.filter((item) => item.filename.toLowerCase().includes(searchTerm))
                    : items;

                const sortedItems = filteredItems.sort((a, b) => {
                    if ((a.directory ?? false) !== (b.directory ?? false)) {
                        return (a.directory ?? false) ? -1 : 1;
                    }
                    return a.filename.localeCompare(b.filename, 'de', {sensitivity: 'base'});
                });

                const start = options.page * options.size;
                const end = start + options.size;
                const pageItems = sortedItems.slice(start, end);

                const totalElements = sortedItems.length;
                const totalPages = totalElements === 0 ? 0 : Math.ceil(totalElements / options.size);

                return {
                    content: pageItems,
                    page: {
                        size: options.size,
                        number: options.page,
                        totalElements,
                        totalPages,
                    },
                };
            });
    }, [currentFolderPath, parsedStorageProviderId]);

    const columnDefinitions = useCallback((permissions: GenericListPagePermissionState<VStorageIndexItemWithAssetEntity>) => [
        {
            field: 'icon',
            headerName: '',
            renderCell: (params: any) => {
                const fileType = getFileTypeLabel(params.row.mimeType ?? 'application/octet-stream');
                return (
                    <CellContentWrapper title={fileType}>
                        {getFileTypeIcon(params.row.mimeType ?? 'application/octet-stream')}
                    </CellContentWrapper>
                );
            },
            disableColumnMenu: true,
            width: 24,
            sortable: false,
        },
        {
            field: 'filename',
            headerName: 'Dateiname',
            flex: 2,
            renderCell: (params: any) => {
                const providerId = params.row.storageProviderId ?? parsedStorageProviderId;
                const isDirectory = params.row.directory === true;
                const targetPath = isDirectory
                    ? (providerId != null
                        ? `/assets/providers/${providerId}?path=${encodeURIComponent(AssetsApiService.normalizeFolderPath(params.row.pathFromRoot ?? '/'))}`
                        : '/assets')
                    : (() => {
                        const encodedPath = AssetsApiService.encodeStoragePathForRoute(params.row.pathFromRoot ?? '/');
                        return providerId != null
                            ? `/assets/providers/${providerId}/files/${encodedPath}`
                            : '/assets';
                    })();

                return (
                    <CellLink
                        to={targetPath}
                        title={isDirectory ? 'Ordner öffnen' : (
                            permissions.canUpdate(params.row) && !storageProviderReadOnly ? 'Datei bearbeiten' : 'Datei ansehen'
                        )}
                    >
                        {String(params.value)}
                    </CellLink>
                );
            },
        },
        {
            field: 'contentType',
            headerName: 'Dateityp',
            flex: 1.5,
            renderCell: (params: any) => {
                if (params.row.directory) {
                    return null;
                }

                const fileType = getFileTypeLabel(params.row.mimeType ?? 'application/octet-stream');
                return (
                    <Stack
                        direction="row"
                        spacing={1}
                        alignItems="center"
                    >
                        <span>{params.row.directory ? 'Ordner' : fileType}</span>
                        <Chip
                            label={params.row.directory ? 'inode/directory' : params.row.mimeType}
                            size="small"
                            variant="outlined"
                            sx={{fontSize: '0.75rem'}}
                        />
                    </Stack>
                );
            },
        },
        {
            field: 'created',
            headerName: 'Hochgeladen am',
            flex: 1,
            renderCell: (params: any) => {
                if (!params.row.created) return '—';
                const formatted = formatInstantInApplicationTimeZone(params.row.created, 'dd.MM.yyyy – HH:mm');
                return formatted != null ? `${formatted} Uhr` : '—';
            },
        },
    ], [parsedStorageProviderId, storageProviderReadOnly]);

    const getRowIdentifier = useCallback((row: VStorageIndexItemWithAssetEntity) => (
        row.directory ? `dir:${row.pathFromRoot}` : (row.assetKey || `file:${row.pathFromRoot}`)
    ), []);

    const rowActions = useCallback((
        item: VStorageIndexItemWithAssetEntity,
        permissions: GenericListPagePermissionState<VStorageIndexItemWithAssetEntity>,
    ) => {
        if (!item.directory) {
            return [];
        }

        const canDeleteAsset = permissions.hasPermission(Permission.ASSET_DELETE);
        const deleteAssetDisabledTooltip = resolveDeleteAssetDisabledTooltip(
            permissions,
            canDeleteAsset,
            storageProviderReadOnly,
        );

        return [
            {
                icon: <Delete/>,
                tooltip: 'Ordner löschen',
                disabled: !canDeleteAsset || storageProviderReadOnly,
                disabledTooltip: deleteAssetDisabledTooltip,
                onClick: async () => {
                    const providerId = item.storageProviderId ?? parsedStorageProviderId;
                    if (providerId == null || !canDeleteAsset || storageProviderReadOnly) {
                        return;
                    }

                    confirm({
                        title: `Ordner ${item.filename} löschen`,
                        children: (
                            <Typography>
                                Soll der Ordner <strong>{item.filename}</strong> wirklich gelöscht werden?
                                Alle darin enthaltenen Dateien und Unterordner werden ebenfalls gelöscht und
                                können nicht wiederhergestellt werden.
                                Dies kann nicht rückgängig gemacht werden.
                            </Typography>
                        ),
                        confirmationText: item.filename,
                        confirmButtonText: 'Löschen',
                    })
                        .then((confirmed) => {
                            if (!confirmed) {
                                return;
                            }

                            return new AssetsApiService(api)
                                .deleteFolder(
                                    providerId,
                                    AssetsApiService.normalizeFolderPath(item.pathFromRoot),
                                );
                        })
                        .then(() => {
                            dispatch(showSuccessSnackbar('Ordner erfolgreich gelöscht.'));
                            handleListRefresh();
                        })
                        .catch((err) => {
                            dispatch(showApiErrorSnackbar(err, 'Der Ordner konnte nicht gelöscht werden.'));
                        });
                },
            },
        ];
    }, [api, confirm, dispatch, handleListRefresh, parsedStorageProviderId, storageProviderReadOnly]);

    const noDataPlaceholder = useCallback((permissions: GenericListPagePermissionState<VStorageIndexItemWithAssetEntity>) => {
        const createAssetDisabledTooltip = resolveCreateAssetDisabledTooltip(
            permissions,
            parsedStorageProviderId,
            storageProviderReadOnly,
        );

        return (
            <EmptyDataListPlaceholder
                title="Keine Dateien vorhanden"
                description="Dateien sind hochgeladene Anlagen und Medien, die in Formularen, Prozessen oder Konfigurationen verwendet werden können."
                addText={parsedStorageProviderId != null ? "Datei hochladen" : undefined}
                onAdd={parsedStorageProviderId != null ? () => navigate(uploadRoute) : undefined}
                addDisabled={!canCreateAssetForCurrentProvider(permissions, parsedStorageProviderId, storageProviderReadOnly)}
                addDisabledTooltip={createAssetDisabledTooltip}
            />
        );
    }, [navigate, parsedStorageProviderId, storageProviderReadOnly, uploadRoute]);

    return (
        <PageWrapper
            title={headerTitle}
            fullWidth
            background
        >
            <GenericListPage<VStorageIndexItemWithAssetEntity>
                header={header}
                permissionCheck={assetListPermissionCheck}
                searchLabel="Datei suchen"
                searchPlaceholder="Name der Datei eingeben…"
                preSearchElements={preSearchElements}
                fetch={fetchAssets}
                columnDefinitions={columnDefinitions}
                getRowIdentifier={getRowIdentifier}
                noDataPlaceholder={noDataPlaceholder}
                noSearchResultsPlaceholder="Keine Dateien gefunden"
                rowActionsCount={1}
                rowActions={rowActions}
                defaultSortField="filename"
                disableFullWidthToggle={true}
                controlRef={listControlRef}
            />
        </PageWrapper>
    );
}

function canCreateAssetForCurrentProvider(
    permissions: GenericListPagePermissionState<VStorageIndexItemWithAssetEntity>,
    parsedStorageProviderId: number | undefined,
    storageProviderReadOnly: boolean,
): boolean {
    return parsedStorageProviderId != null && permissions.canCreate && !storageProviderReadOnly;
}

function resolveCreateAssetDisabledTooltip(
    permissions: GenericListPagePermissionState<VStorageIndexItemWithAssetEntity>,
    parsedStorageProviderId: number | undefined,
    storageProviderReadOnly: boolean,
): string | undefined {
    if (parsedStorageProviderId == null) {
        return 'Wählen Sie zuerst einen Speicheranbieter aus.';
    }

    if (!permissions.canCreate) {
        return permissions.createDisabledTooltip;
    }

    if (storageProviderReadOnly) {
        return 'Der ausgewählte Speicheranbieter ist schreibgeschützt.';
    }

    return undefined;
}

function resolveDeleteAssetDisabledTooltip(
    permissions: GenericListPagePermissionState<VStorageIndexItemWithAssetEntity>,
    canDeleteAsset: boolean,
    storageProviderReadOnly: boolean,
): string | undefined {
    if (!canDeleteAsset) {
        return permissions.getMissingPermissionTooltip(Permission.ASSET_DELETE);
    }

    if (storageProviderReadOnly) {
        return 'Der ausgewählte Speicheranbieter ist schreibgeschützt.';
    }

    return undefined;
}
