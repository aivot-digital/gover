import {GenericListPage} from '../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {Stack, Typography} from '@mui/material';
import {DownloadOutlined, EditOutlined, VisibilityOutlined} from '@mui/icons-material';
import React, {useEffect, useMemo, useState} from 'react';
import {CellLink} from '../../../components/cell-link/cell-link';
import {Asset} from '../models/asset';
import {AssetsApiService} from '../assets-api-service';
import ContentPasteOutlinedIcon from '@mui/icons-material/ContentPasteOutlined';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {getFileTypeIcon} from '../../../utils/file-type-icon';
import DriveFolderUploadOutlinedIcon from '@mui/icons-material/DriveFolderUploadOutlined';
import {getFileTypeLabel} from '../../../utils/file-type-label';
import Chip from '@mui/material/Chip';
import {CellContentWrapper} from '../../../components/cell-content-wrapper/cell-content-wrapper';
import {copyToClipboardText} from '../../../utils/copy-to-clipboard';
import {useParams} from 'react-router-dom';
import {StorageProvidersApiService} from '../../storage/storage-providers-api-service';
import {showApiErrorSnackbar} from '../../../slices/snackbar-slice';

export function AssetListPage() {
    const dispatch = useAppDispatch();
    const {storageProviderId} = useParams<{ storageProviderId?: string }>();

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
        ? `/assets/providers/${parsedStorageProviderId}/new`
        : '/assets/new';

    return (
        <PageWrapper
            title={headerTitle}
            fullWidth
            background
        >
            <GenericListPage<Asset>
                header={{
                    icon: <DriveFolderUploadOutlinedIcon />,
                    title: headerTitle,
                    actions: [
                        {
                            label: 'Datei hochladen',
                            icon: <AddOutlinedIcon />,
                            tooltip: 'Neues Dokument oder Medieninhalt anlegen',
                            disabledTooltip: 'Der ausgewählte Speicheranbieter ist schreibgeschützt.',
                            disabled: storageProviderReadOnly,
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
                                    Dokumente und Medieninhalte sind Dateien, die in der Anwendung hochgeladen und verwaltet werden können.
                                    In dieser Oberfläche können Sie die im System verfügbaren Dateien einsehen und bearbeiten.
                                </Typography>
                                <Typography sx={{mt: 2}}>
                                    Sie können die hochgeladenen Dateien u.A. in Formularen verwenden, um z.B. Bilder oder PDFs einzubinden.
                                    Darüber hinaus können Systemdateien (wie Zertifikate oder Templates) z.B. für die Konfiguration von
                                    Zahlungsdienstleistern oder der Dokumentengenerierung genutzt werden.
                                </Typography>
                            </>
                        ),
                    },
                }}
                searchLabel="Datei suchen"
                searchPlaceholder="Name der Datei eingeben…"
                fetch={(options) => {
                    return new AssetsApiService(options.api)
                        .list(
                            options.page,
                            options.size,
                            options.sort,
                            options.order,
                            {
                                filename: options.search,
                                storageProviderId: parsedStorageProviderId,
                            },
                        );
                }}
                columnDefinitions={[
                    {
                        field: 'icon',
                        headerName: '',
                        renderCell: (params) => {
                            const fileType = getFileTypeLabel(params.row.contentType ?? 'application/octet-stream');
                            return (
                                <CellContentWrapper title={fileType}>
                                    {getFileTypeIcon(params.row.contentType ?? 'application/octet-stream')}
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
                        renderCell: (params) => (
                            <CellLink
                                to={parsedStorageProviderId != null ? `/assets/providers/${parsedStorageProviderId}/${params.id}` : `/assets/${params.id}`}
                                title={storageProviderReadOnly ? 'Datei ansehen' : 'Datei bearbeiten'}
                            >
                                {String(params.value)}
                            </CellLink>
                        )
                    },
                    {
                        field: 'contentType',
                        headerName: 'Dateityp',
                        flex: 1.5,
                        renderCell: (params) => {
                            const fileType = getFileTypeLabel(params.row.contentType ?? 'application/octet-stream');
                            return (
                                <Stack direction="row" spacing={1} alignItems="center">
                                    <span>{fileType}</span>
                                    <Chip
                                        label={params.row.contentType}
                                        size="small"
                                        variant={'outlined'}
                                        sx={{ fontSize: '0.75rem' }}
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
                        }
                    },

                ]}
                getRowIdentifier={row => row.key}
                noDataPlaceholder="Keine Dateien hochgeladen"
                noSearchResultsPlaceholder="Keine Dateien gefunden"
                rowActionsCount={3}
                rowActions={(item: Asset) => [
                    {
                        icon: storageProviderReadOnly ? <VisibilityOutlined /> : <EditOutlined />,
                        to: parsedStorageProviderId != null ? `/assets/providers/${parsedStorageProviderId}/${item.key}` : `/assets/${item.key}`,
                        tooltip: storageProviderReadOnly ? 'Datei ansehen' : 'Datei bearbeiten',
                    },
                    {
                        icon: <ContentPasteOutlinedIcon />,
                        onClick: async () => {
                            const link = AssetsApiService.useAssetLinkOfAsset(item);
                            const success = await copyToClipboardText(link);
                            if (success) {
                                dispatch(showSuccessSnackbar('Link in Zwischenablage kopiert!'));
                            } else {
                                dispatch(showErrorSnackbar('Fehler beim Kopieren des Links!'));
                            }
                        },
                        tooltip: item.isPrivate ? `Öffentlicher Zugriff für Datei deaktiviert` : `Link zur Datei in Zwischenablage kopieren`,
                        disabled: item.isPrivate ?? undefined,
                    },
                    {
                        icon: <DownloadOutlined />,
                        href: AssetsApiService.useAssetLinkOfAsset(item)+'?download=true',
                        tooltip: item.isPrivate ? `Öffentlicher Zugriff für Datei deaktiviert` : `Datei herunterladen`,
                        disabled: item.isPrivate ?? undefined,

                    },
                ]}
                defaultSortField="filename"
                disableFullWidthToggle={true}
            />
        </PageWrapper>
    );
}
