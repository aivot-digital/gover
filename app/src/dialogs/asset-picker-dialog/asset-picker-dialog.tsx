import React, {type PropsWithChildren, useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {
    Alert,
    Box,
    CircularProgress,
    Dialog,
    DialogContent,
    MenuItem,
    Stack,
    TextField,
    Typography,
} from '@mui/material';
import {Link} from 'react-router-dom';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {useApi} from '../../hooks/use-api';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {showApiErrorSnackbar, showErrorSnackbar, showSuccessSnackbar} from '../../slices/snackbar-slice';
import {type StorageIndexItem} from '../../modules/storage/entities/storage-index-item-entity';
import {StorageProvidersApiService} from '../../modules/storage/storage-providers-api-service';
import {StorageProviderType} from '../../modules/storage/enums/storage-provider-type';
import {AssetsApiService} from '../../modules/assets/assets-api-service';
import {type Asset} from '../../modules/assets/models/asset';
import {type StorageProviderEntity} from '../../modules/storage/entities/storage-provider-entity';
import {AssetExplorer} from '../../modules/storage/components/asset-explorer';
import {useConfirm} from '../../providers/confirm-provider';

export interface AssetPickerDialogProps {
    title: string;
    show: boolean;
    mimeType?: string | string[];
    mode?: 'public' | 'all';
    onSelectAsset: (assetKey: string, storagePathFromRoot: string, storageProviderId: number) => void;
    onCancel: () => void;
}

export function AssetPickerDialog(props: PropsWithChildren<AssetPickerDialogProps>) {
    const {
        title,
        show,
        mimeType,
        mode = 'all',
        onSelectAsset,
        onCancel,
        children,
    } = props;

    const api = useApi();
    const dispatch = useAppDispatch();
    const confirm = useConfirm();
    const [providers, setProviders] = useState<StorageProviderEntity[]>([]);
    const [isLoadingProviders, setIsLoadingProviders] = useState(false);
    const [selectedProviderId, setSelectedProviderId] = useState<number>();
    const [isProcessingSelection, setIsProcessingSelection] = useState(false);
    const [selectionProcessingMessage, setSelectionProcessingMessage] = useState<string>();
    const isProcessingSelectionRef = useRef(false);

    useEffect(() => {
        if (!show) {
            return;
        }

        let isActive = true;
        setIsLoadingProviders(true);

        new StorageProvidersApiService()
            .listAll({
                type: StorageProviderType.Assets,
            })
            .then(({content: loadedProviders}) => {
                if (!isActive) {
                    return;
                }

                const sortedProviders = loadedProviders
                    .slice()
                    .sort((a, b) => a.name.localeCompare(b.name, 'de', {sensitivity: 'base'}));

                setProviders(sortedProviders);
                setSelectedProviderId((previousProviderId) => {
                    if (previousProviderId != null && sortedProviders.some((provider) => provider.id === previousProviderId)) {
                        return previousProviderId;
                    }

                    return sortedProviders[0]?.id;
                });
            })
            .catch((err) => {
                if (!isActive) {
                    return;
                }

                setProviders([]);
                setSelectedProviderId(undefined);
                dispatch(showApiErrorSnackbar(err, 'Die Liste der Speicheranbieter konnte nicht geladen werden.'));
            })
            .finally(() => {
                if (isActive) {
                    setIsLoadingProviders(false);
                }
            });

        return () => {
            isActive = false;
        };
    }, [dispatch, show]);

    useEffect(() => {
        if (show) {
            return;
        }

        setIsProcessingSelection(false);
        setSelectionProcessingMessage(undefined);
        isProcessingSelectionRef.current = false;
    }, [show]);

    const normalizedMimeTypes = useMemo(() => {
        if (mimeType == null) {
            return [];
        }

        const mimeTypes = Array.isArray(mimeType) ? mimeType : [mimeType];
        return mimeTypes
            .map((entry) => entry.trim())
            .filter((entry) => entry.length > 0);
    }, [mimeType]);

    const handleDialogClose = useCallback(() => {
        if (isProcessingSelection) {
            return;
        }

        onCancel();
    }, [isProcessingSelection, onCancel]);

    const startSelectionProcessing = useCallback((message: string) => {
        isProcessingSelectionRef.current = true;
        setSelectionProcessingMessage(message);
        setIsProcessingSelection(true);
    }, []);

    const stopSelectionProcessing = useCallback(() => {
        isProcessingSelectionRef.current = false;
        setIsProcessingSelection(false);
        setSelectionProcessingMessage(undefined);
    }, []);

    const loadAssetForSelection = useCallback(async (item: StorageIndexItem): Promise<Asset | null> => {
        try {
            const asset = await new AssetsApiService(api).retrieveInStorageProvider(item.pathFromRoot, item.storageProviderId);
            if (asset.key.trim().length === 0) {
                dispatch(showErrorSnackbar('Die ausgewählte Datei ist keiner Datei zugeordnet und kann nicht ausgewählt werden.'));
                return null;
            }

            return asset;
        } catch (err) {
            dispatch(showApiErrorSnackbar(err, 'Die ausgewählte Datei konnte nicht geladen werden.'));
            return null;
        }
    }, [api, dispatch]);

    const handleSelectFile = useCallback(async (item: StorageIndexItem) => {
        if (item.directory || isProcessingSelectionRef.current) {
            return;
        }

        const itemAssetKey = typeof item.assetKey === 'string'
            ? item.assetKey.trim()
            : '';
        let assetKey = itemAssetKey;
        let assetIsPrivate = typeof item.assetIsPrivate === 'boolean'
            ? item.assetIsPrivate
            : undefined;
        let resolvedAsset: Asset | null = null;

        try {
            startSelectionProcessing('Datei wird geprüft…');

            if (assetKey.length === 0 || (mode === 'public' && assetIsPrivate == null)) {
                resolvedAsset = await loadAssetForSelection(item);
                if (resolvedAsset == null) {
                    return;
                }

                assetKey = resolvedAsset.key.trim();
                assetIsPrivate = resolvedAsset.isPrivate;
            }

            if (assetKey.length === 0) {
                dispatch(showErrorSnackbar('Die ausgewählte Datei ist nicht zugeordnet und kann nicht ausgewählt werden.'));
                return;
            }

            if (mode === 'public' && assetIsPrivate === true) {
                stopSelectionProcessing();

                const confirmed = await confirm({
                    title: 'Datei öffentlich schalten?',
                    confirmButtonText: 'Öffentlich schalten und auswählen',
                    width: 'md',
                    children: (
                        <Stack spacing={2}>
                            <Typography variant="body2">
                                Die ausgewählte Datei ist aktuell nicht öffentlich erreichbar.
                            </Typography>
                            <Typography variant="body2">
                                Wenn Sie fortfahren, wird der öffentliche Zugriff ohne Authentifizierung aktiviert und
                                die Datei danach direkt ausgewählt.
                            </Typography>
                            <Alert severity="warning">
                                Nutzen Sie diese Option nur für Dateien, die öffentlich sein müssen, und niemals für
                                sicherheitsrelevante Dateien wie Zertifikate.
                            </Alert>
                        </Stack>
                    ),
                });

                if (!confirmed) {
                    return;
                }

                startSelectionProcessing('Datei wird veröffentlicht…');

                const assetToPublish = resolvedAsset ?? await loadAssetForSelection(item);
                if (assetToPublish == null) {
                    return;
                }

                try {
                    const updatedAsset = await new AssetsApiService(api).updateInStorageProvider(
                        assetToPublish.storagePathFromRoot,
                        {
                            ...assetToPublish,
                            isPrivate: false,
                        },
                        assetToPublish.storageProviderId,
                    );

                    dispatch(showSuccessSnackbar('Die Datei wurde veröffentlicht und ausgewählt.'));
                    onSelectAsset(updatedAsset.key, updatedAsset.storagePathFromRoot, updatedAsset.storageProviderId);
                    return;
                } catch (err) {
                    dispatch(showApiErrorSnackbar(err, 'Die Datei konnte nicht veröffentlicht werden.'));
                    return;
                }
            }

            if (resolvedAsset != null) {
                onSelectAsset(resolvedAsset.key, resolvedAsset.storagePathFromRoot, resolvedAsset.storageProviderId);
                return;
            }

            onSelectAsset(assetKey, item.pathFromRoot, item.storageProviderId);
        } finally {
            stopSelectionProcessing();
        }
    }, [
        api,
        confirm,
        dispatch,
        loadAssetForSelection,
        mode,
        onSelectAsset,
        startSelectionProcessing,
        stopSelectionProcessing,
    ]);

    return (
        <Dialog
            fullWidth
            maxWidth="lg"
            open={show}
            onClose={handleDialogClose}
        >
            <DialogTitleWithClose onClose={handleDialogClose}>
                {title} ({mimeType}) {mode === 'public' && `(nur öffentlich auswählbar)`}
            </DialogTitleWithClose>

            <DialogContent tabIndex={0}
                           sx={{
                               paddingTop: '0.5rem !important',
                           }}
            >
                {children != null && (
                    <Box sx={{mb: 2}}>
                        {children}
                    </Box>
                )}

                <Stack spacing={2}>
                    {mode === 'public' && (
                        <Alert severity="info">
                            Nicht öffentliche Dateien werden angezeigt und grau markiert. Wenn Sie eine solche Datei
                            auswählen, können Sie es direkt hier öffentlich schalten.
                        </Alert>
                    )}

                    <TextField
                        select={true}
                        size="small"
                        label="Speicheranbieter"
                        value={selectedProviderId ?? ''}
                        onChange={(event) => {
                            const nextProviderId = Number.parseInt(event.target.value, 10);
                            setSelectedProviderId(Number.isNaN(nextProviderId) ? undefined : nextProviderId);
                        }}
                        disabled={isLoadingProviders || providers.length === 0 || isProcessingSelection}
                    >
                        {providers.map((provider) => (
                            <MenuItem key={provider.id}
                                      value={provider.id}>
                                {provider.name}
                            </MenuItem>
                        ))}
                    </TextField>

                    {isLoadingProviders && (
                        <Stack
                            direction="row"
                            spacing={1.5}
                            alignItems="center"
                            sx={{py: 3, justifyContent: 'center'}}
                        >
                            <CircularProgress size={18}/>
                            <Typography variant="body2"
                                        color="text.secondary">
                                Speicheranbieter werden geladen…
                            </Typography>
                        </Stack>
                    )}

                    {!isLoadingProviders && providers.length === 0 && (
                        <Alert severity="info">
                            Es sind keine Speicheranbieter konfiguriert. Gehen Sie zu{' '}
                            <Link to="/storage-providers"
                                  style={{color: 'inherit'}}>
                                Speicheranbieter
                            </Link>
                            {' '}und richten Sie einen Speicheranbieter ein.
                        </Alert>
                    )}

                    {!isLoadingProviders && providers.length > 0 && selectedProviderId == null && (
                        <Alert severity="info">
                            Wählen Sie einen Speicheranbieter aus, um dessen Ordner und Dateien zu durchsuchen.
                        </Alert>
                    )}

                    {!isLoadingProviders && selectedProviderId != null && (
                        <Box sx={{position: 'relative'}}>
                            <AssetExplorer
                                providerId={selectedProviderId}
                                onFileSelect={handleSelectFile}
                                filterMimeTypes={normalizedMimeTypes}
                                showTopNavigationBar={true}
                                minGridHeight={460}
                            />

                            {isProcessingSelection && (
                                <Stack
                                    spacing={1.5}
                                    alignItems="center"
                                    justifyContent="center"
                                    sx={{
                                        position: 'absolute',
                                        inset: 0,
                                        zIndex: 1,
                                        bgcolor: 'rgba(255, 255, 255, 0.5)',
                                        pointerEvents: 'all',
                                    }}
                                >
                                    <CircularProgress size={20}/>
                                    <Typography
                                        variant="body2"
                                        color="text.secondary"
                                    >
                                        {selectionProcessingMessage ?? 'Bitte warten…'}
                                    </Typography>
                                </Stack>
                            )}
                        </Box>
                    )}
                </Stack>
            </DialogContent>
        </Dialog>
    );
}
