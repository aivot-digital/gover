import {GenericListPage} from '../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {Box, Stack, Typography} from '@mui/material';
import {DownloadOutlined, EditOutlined} from '@mui/icons-material';
import React from 'react';
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

export function AssetListPage() {
    const dispatch = useAppDispatch();

    return (
        <PageWrapper
            title="Dokumente & Medieninhalte"
            fullWidth
            background
        >
            <GenericListPage<Asset>
                header={{
                    icon: <DriveFolderUploadOutlinedIcon />,
                    title: 'Dokumente & Medieninhalte',
                    actions: [
                        {
                            label: 'Datei hochladen',
                            icon: <AddOutlinedIcon />,
                            tooltip: 'Neues Dokument oder Medieninhalt anlegen',
                            to: '/assets/new',
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
                                <Box sx={{display: 'flex', alignItems: 'center'}} title={fileType}>
                                    {getFileTypeIcon(params.row.contentType ?? 'application/octet-stream')}
                                </Box>
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
                                to={`/assets/${params.id}`}
                                title={`Datei bearbeiten`}
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
                        icon: <EditOutlined />,
                        to: `/assets/${item.key}`,
                        tooltip: 'Datei bearbeiten',
                    },
                    {
                        icon: <ContentPasteOutlinedIcon />,
                        onClick: () => {
                            const link = AssetsApiService.useAssetLinkOfAsset(item)
                            navigator
                                .clipboard
                                .writeText(link)
                                .then(() => {
                                    dispatch(showSuccessSnackbar('Link in Zwischenablage kopiert!'));
                                })
                                .catch((err) => {
                                    console.error(err);
                                    dispatch(showErrorSnackbar('Fehler beim Kopieren des Links!'));
                                });
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