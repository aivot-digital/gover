import {Box, Button, Container, Paper, Stack, Typography} from '@mui/material';
import React, {useEffect, useMemo, useState} from 'react';
import {useNavigate, useParams, useSearchParams} from 'react-router-dom';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar, showErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {useChangeBlocker} from '../../../hooks/use-change-blocker-2';
import {AssetsApiService} from '../assets-api-service';
import {FileUploadComponent} from '../../../components/file-upload-field/file-upload-component';
import {hideLoadingOverlayWithTimeout, showLoadingOverlay} from '../../../slices/loading-overlay-slice';
import {GenericDetailsSkeleton} from '../../../components/generic-details-page/generic-details-skeleton';
import {StorageMetadataAttributesEditor} from '../../storage/components/storage-metadata-attributes-editor';
import {isStringNullOrEmpty} from '../../../utils/string-utils';
import {StorageProviderEntity} from '../../storage/entities/storage-provider-entity';
import {StorageProvidersApiService} from '../../storage/storage-providers-api-service';
import {GenericPageHeader} from '../../../components/generic-page-header/generic-page-header';
import {ModuleIcons} from '../../../shells/staff/data/module-icons';
import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';
import {clearLoadingMessage, setLoadingMessage} from '../../../slices/shell-slice';

const DEFAULT_DATA = {
    file: null,
    metadata: null,
};

export function AssetDetailsPageNew() {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();
    const {storageProviderId} = useParams<{
        storageProviderId: string;
    }>();
    const [searchParams] = useSearchParams();

    const [storageProvider, setStorageProvider] = useState<StorageProviderEntity | null>(null);
    const [newAssetData, setNewAssetData] = useState<{
        file: File | null;
        metadata: Record<string, string> | null;
    }>(DEFAULT_DATA);
    const [isBusy, setIsBusy] = useState<boolean>(false);

    useEffect(() => {
        if (storageProviderId == null) {
            setStorageProvider(null);
            return;
        }

        const sid = parseInt(storageProviderId);

        new StorageProvidersApiService()
            .retrieve(sid)
            .then(setStorageProvider)
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Der angeforderte Speicheranbieter konnte nicht geladen werden. Bitte versuchen Sie es später erneut.', true));
            });
    }, [storageProviderId]);

    const uploadFolderPath = useMemo(() => {
        const pathRaw = searchParams.get('path');

        let path: string;
        if (pathRaw == null || isStringNullOrEmpty(pathRaw)) {
            path = '/';
        } else {
            path = decodeURIComponent(pathRaw);
        }

        if (!path.startsWith('/')) {
            path = '/' + path;
        }

        if (!path.endsWith('/')) {
            path = '/' + path;
        }

        return path;
    }, [searchParams]);

    const [uploadError, setUploadError] = useState<string>();

    const {
        dialog: changeBlockerDialog,
    } = useChangeBlocker({
        original: DEFAULT_DATA,
        edited: newAssetData,
        useDeepEquals: true,
    });

    if (storageProvider == null) {
        return (
            <GenericDetailsSkeleton/>
        );
    }

    const handleUpload = (): void => {
        if (storageProvider.readOnlyStorage) {
            return;
        }

        if (newAssetData.file == null) {
            return;
        }

        const filename = newAssetData.file.name;
        const targetFolderPath = AssetsApiService.normalizeFolderPath(searchParams.get('path') ?? '/');
        const storagePathFromRoot = `${targetFolderPath}${filename}`;

        setIsBusy(true);

        dispatch(setLoadingMessage({
            message: 'Datei wird hochgeladen',
            estimatedTime: 500,
            blocking: false,
        }));

        new AssetsApiService()
            .upload(newAssetData.file, storageProvider.id, storagePathFromRoot, {
                metadata: newAssetData.metadata ?? undefined,
            })
            .then((newAsset) => {
                dispatch(showSuccessSnackbar('Neue Datei erfolgreich angelegt.'));

                // Reset asset data
                setNewAssetData(DEFAULT_DATA);

                // use setTimeout instead of useEffect to prevent unnecessary rerender
                setTimeout(() => {
                    const detailsRoute = `/assets/providers/${storageProvider.id}/files/${AssetsApiService.encodeStoragePathForRoute(newAsset.storagePathFromRoot)}`;
                    navigate(detailsRoute, {
                        replace: true,
                    });
                }, 0);

            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Speichern fehlgeschlagen. Bitte überprüfen Sie Ihre Eingaben.', true));
            })
            .finally(() => {
                setIsBusy(false);
                dispatch(clearLoadingMessage());
            });
    };

    return (
        <PageWrapper
            title="Datei hochladen"
            fullWidth
            background
        >
            <Container>
                <GenericPageHeader
                    icon={ModuleIcons.assets}
                    title={`Neue Datei hochladen nach ${storageProvider.name}${uploadFolderPath}`}
                />

                <Paper
                    sx={{
                        marginTop: 3.5,
                        padding: 2,
                    }}
                >
                    <Typography
                        variant="h5"
                        sx={{mb: 1}}
                    >
                        Neue Datei hochladen
                    </Typography>

                    <Typography sx={{mb: 2, maxWidth: 900}}>
                        {
                            storageProvider.readOnlyStorage
                                ? 'Der Speicheranbieter ist schreibgeschützt. Sie können keine Dateien hochladen'
                                : 'Wählen Sie eine einzelne Datei zum Hochladen aus oder ziehen Sie die Datei in das Feld.'
                        }
                    </Typography>

                    <FileUploadComponent
                        id="asset-upload"
                        value={newAssetData.file != null ? [newAssetData.file] : undefined}
                        onChange={files => {
                            if (storageProvider.readOnlyStorage) {
                                return;
                            }

                            setNewAssetData(prev => {
                                if (files != null && files.length > 0) {
                                    return {
                                        ...prev,
                                        file: files[0],
                                    };
                                } else {
                                    return {
                                        ...prev,
                                        file: null,
                                    };
                                }
                            });

                            setUploadError(undefined);
                        }}
                        label="Datei"
                        isMultifile={false}
                        minFiles={1}
                        maxFiles={1}
                        required={true}
                        error={uploadError}
                        disabled={storageProvider.readOnlyStorage || isBusy}
                    />

                    {
                        storageProvider.metadataAttributes.length > 0 && (
                            <Box
                                sx={{
                                    mt: 3,
                                    maxWidth: 900,
                                }}
                            >
                                <Typography variant="h6"
                                            sx={{mb: 1}}>
                                    Metadaten
                                </Typography>

                                <Typography sx={{mb: 2}}>
                                    Hinterlegen Sie optional zusätzliche Metadaten für die Datei entsprechend den
                                    konfigurierten Attributen des Speicheranbieters.
                                </Typography>

                                <StorageMetadataAttributesEditor
                                    storageProvider={storageProvider}
                                    metadata={newAssetData.metadata ?? {}}
                                    onChange={(metadata) => {
                                        setNewAssetData(prev => ({
                                            ...prev,
                                            metadata: metadata,
                                        }));
                                    }}
                                />
                            </Box>
                        )
                    }

                    <Stack
                        direction="row"
                        justifyContent="flex-start"
                        alignItems="center"
                    >
                        <Button
                            variant="contained"
                            disabled={newAssetData.file == null || storageProvider.readOnlyStorage || isBusy}
                            onClick={handleUpload}
                        >
                            Hochladen
                        </Button>
                    </Stack>
                </Paper>
            </Container>
            {changeBlockerDialog}
        </PageWrapper>
    );
}
