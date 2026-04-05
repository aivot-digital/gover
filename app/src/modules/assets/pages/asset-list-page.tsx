import {GenericListPage} from '../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {Stack, Typography} from '@mui/material';
import {CreateNewFolderOutlined} from '@mui/icons-material';
import React, {useEffect, useMemo, useRef, useState} from 'react';
import {CellLink} from '../../../components/cell-link/cell-link';
import {Asset} from '../models/asset';
import {AssetsApiService} from '../assets-api-service';
import {showApiErrorSnackbar, showErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {getFileTypeIcon} from '../../../utils/file-type-icon';
import DriveFolderUploadOutlinedIcon from '@mui/icons-material/DriveFolderUploadOutlined';
import {getFileTypeLabel} from '../../../utils/file-type-label';
import Chip from '@mui/material/Chip';
import {CellContentWrapper} from '../../../components/cell-content-wrapper/cell-content-wrapper';
import {useParams, useSearchParams} from 'react-router-dom';
import {StorageProvidersApiService} from '../../storage/storage-providers-api-service';
import {useApi} from '../../../hooks/use-api';
import {ListControlRef} from '../../../components/generic-list/generic-list-props';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import {useConfirm} from '../../../providers/confirm-provider';
import {Breadcrumbs} from '../../../components/breadcrumbs/breadcrumbs';
import {usePrompt} from '../../../providers/prompt-provider';
import {isStringNullOrEmpty} from '../../../utils/string-utils';
import {VStorageIndexItemWithAssetEntity} from '../../storage/entities/storage-index-item-entity';


export function AssetListPage() {
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

        new StorageProvidersApiService()
            .retrieve(parsedStorageProviderId)
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

    return (
        <PageWrapper
            title={headerTitle}
            fullWidth
            background
        >
            <GenericListPage<VStorageIndexItemWithAssetEntity>
                header={{
                    icon: <DriveFolderUploadOutlinedIcon/>,
                    title: headerTitle,
                    actions: [
                        {
                            icon: <CreateNewFolderOutlined/>,
                            tooltip: 'Neuen Ordner anlegen',
                            disabledTooltip: parsedStorageProviderId == null
                                ? 'Wählen Sie zuerst einen Speicheranbieter aus.'
                                : 'Der ausgewählte Speicheranbieter ist schreibgeschützt.',
                            disabled: parsedStorageProviderId == null || storageProviderReadOnly,
                            onClick: () => {
                                if (parsedStorageProviderId == null || storageProviderReadOnly) {
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
                                    .then((r) => {
                                        if (r == null) {
                                            return null;
                                        }
                                        dispatch(showSuccessSnackbar('Ordner erfolgreich angelegt.'));
                                        listControlRef.current?.refresh();
                                    })
                                    .catch((err) => {
                                        dispatch(showApiErrorSnackbar(err, 'Der Ordner konnte nicht angelegt werden.'));
                                    });
                            },
                        },
                        'separator',
                        {
                            label: 'Datei hochladen',
                            icon: <AddOutlinedIcon/>,
                            tooltip: 'Neues Dokument oder Medieninhalt anlegen',
                            disabledTooltip: parsedStorageProviderId == null
                                ? 'Wählen Sie zuerst einen Speicheranbieter aus.'
                                : 'Der ausgewählte Speicheranbieter ist schreibgeschützt.',
                            disabled: parsedStorageProviderId == null || storageProviderReadOnly,
                            to: uploadRoute,
                            variant: 'contained',
                        },
                    ],
                    helpDialog: {
                        title: 'Hilfe zu Dokumenten & Medieninhalten',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography>
                                    Dokumente und Medieninhalte sind Dateien, die in der Anwendung hochgeladen und
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
                }}
                searchLabel="Datei suchen"
                searchPlaceholder="Name der Datei eingeben…"
                preSearchElements={[
                    <Breadcrumbs
                        key={currentFolderPath + storageProviderName}
                        prefix={`/assets/providers/${storageProviderId}`}
                        path={currentFolderPath}
                        rootLabel={storageProviderName ?? 'Alle Dateien'}
                    />,
                ]}
                fetch={(options) => {
                    if (parsedStorageProviderId == null) {
                        return Promise.resolve({
                            content: [] as VStorageIndexItemWithAssetEntity[],
                            last: true,
                            totalElements: 0,
                            totalPages: 0,
                            size: options.size,
                            number: options.page,
                            first: true,
                            numberOfElements: 0,
                            empty: true,
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
                                last: end >= totalElements,
                                totalElements: totalElements,
                                totalPages: totalPages,
                                size: options.size,
                                number: options.page,
                                first: options.page === 0,
                                numberOfElements: pageItems.length,
                                empty: pageItems.length === 0,
                            };
                        });
                }}
                columnDefinitions={[
                    {
                        field: 'icon',
                        headerName: '',
                        renderCell: (params) => {
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
                        renderCell: (params) => {
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
                                    title={isDirectory ? 'Ordner öffnen' : (storageProviderReadOnly ? 'Datei ansehen' : 'Datei bearbeiten')}
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
                        renderCell: (params) => {
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
                                        variant={'outlined'}
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
                        renderCell: (params) => {
                            if (!params.row.created) return '—';
                            const date = new Date(params.row.created);
                            return new Intl.DateTimeFormat('de-DE', {
                                day: '2-digit',
                                month: '2-digit',
                                year: 'numeric',
                                hour: '2-digit',
                                minute: '2-digit',
                                hour12: false,
                            }).format(date).replace(',', ' –') + ' Uhr';
                        },
                    },
                ]}
                getRowIdentifier={row => row.directory ? `dir:${row.pathFromRoot}` : (row.assetKey || `file:${row.pathFromRoot}`)}
                noDataPlaceholder="Keine Dateien hochgeladen"
                noSearchResultsPlaceholder="Keine Dateien gefunden"
                rowActionsCount={1}
                rowActions={(item: VStorageIndexItemWithAssetEntity) => item.directory ? [
                    {
                        icon: <Delete/>,
                        tooltip: 'Ordner löschen',
                        disabled: storageProviderReadOnly,
                        onClick: async () => {
                            const providerId = item.storageProviderId ?? parsedStorageProviderId;
                            if (providerId == null || storageProviderReadOnly) {
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
                                    listControlRef.current?.refresh();
                                })
                                .catch((err) => {
                                    dispatch(showApiErrorSnackbar(err, 'Der Ordner konnte nicht gelöscht werden.'));
                                });
                        },
                    },
                ] : []}
                defaultSortField="filename"
                disableFullWidthToggle={true}
                controlRef={listControlRef}
            />
        </PageWrapper>
    );
}
