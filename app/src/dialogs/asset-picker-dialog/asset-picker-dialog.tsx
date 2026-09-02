import React, {type PropsWithChildren, useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {
    Alert,
    Box,
    Button,
    Chip,
    CircularProgress,
    Dialog,
    DialogContent,
    Grid,
    Stack,
    Typography,
} from '@mui/material';
import {Link} from 'react-router-dom';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {useApi} from '../../hooks/use-api';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {showApiErrorSnackbar, showErrorSnackbar, showSuccessSnackbar} from '../../slices/snackbar-slice';
import {type StorageIndexItem} from '../../modules/storage/entities/storage-index-item-entity';
import {AssetsApiService} from '../../modules/assets/assets-api-service';
import {type Asset} from '../../modules/assets/models/asset';
import {AssetExplorer} from '../../modules/storage/components/asset-explorer';
import {useConfirm} from '../../providers/confirm-provider';
import {getFileTypeFilterSummary} from '../../utils/file-type-label';
import {type AssetStorageProvider} from '../../modules/assets/models/asset-storage-provider';
import {useHasSystemPermission} from '../../modules/permissions/hooks/use-permissions';
import {Permission} from '../../data/permissions/permission';
import {formatMissingPermissionTooltip} from '../../modules/permissions/utils/permission-utils';
import {isApiError} from '../../models/api-error';
import {SelectFieldComponent} from '../../components/select-field/select-field-component';
import {FormField} from '../../components/form-field';
import {FormFieldTokens} from '../../theming/form-field-tokens';

type ProviderLoadError = 'permission' | 'generic';

export interface AssetPickerDialogProps {
    id?: string;
    title: string;
    show: boolean;
    mimeType?: string | string[];
    mode?: 'public' | 'all';
    onSelectAsset: (assetKey: string, storagePathFromRoot: string, storageProviderId: number) => void;
    onCancel: () => void;
}

export function AssetPickerDialog(props: PropsWithChildren<AssetPickerDialogProps>) {
    const {
        id,
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
    const canReadAssets = useHasSystemPermission(Permission.ASSET_READ);
    const canUpdateAssets = useHasSystemPermission(Permission.ASSET_UPDATE);
    const [providers, setProviders] = useState<AssetStorageProvider[]>([]);
    const [isLoadingProviders, setIsLoadingProviders] = useState(false);
    const [providerLoadError, setProviderLoadError] = useState<ProviderLoadError>();
    const [selectedProviderId, setSelectedProviderId] = useState<number>();
    const [isProcessingSelection, setIsProcessingSelection] = useState(false);
    const [selectionProcessingMessage, setSelectionProcessingMessage] = useState<string>();
    const isProcessingSelectionRef = useRef(false);
    const providerLoadRequestRef = useRef(0);

    const loadProviders = useCallback(() => {
        const requestId = providerLoadRequestRef.current + 1;
        providerLoadRequestRef.current = requestId;
        setProviders([]);
        setSelectedProviderId(undefined);
        setProviderLoadError(undefined);

        // The picker is embedded in forms that may still be readable without asset access.
        if (!canReadAssets) {
            setIsLoadingProviders(false);
            setProviderLoadError('permission');
            return;
        }

        setIsLoadingProviders(true);

        new AssetsApiService()
            .listStorageProviders()
            .then((loadedProviders) => {
                if (providerLoadRequestRef.current !== requestId) {
                    return;
                }

                const sortedProviders = loadedProviders
                    .slice()
                    .sort((a, b) => a.name.localeCompare(b.name, 'de', {sensitivity: 'base'}));

                setProviders(sortedProviders);
                setProviderLoadError(undefined);
                setSelectedProviderId((previousProviderId) => {
                    if (previousProviderId != null && sortedProviders.some((provider) => provider.id === previousProviderId)) {
                        return previousProviderId;
                    }

                    return sortedProviders[0]?.id;
                });
            })
            .catch((err) => {
                if (providerLoadRequestRef.current !== requestId) {
                    return;
                }

                setProviders([]);
                setSelectedProviderId(undefined);
                if (isApiError(err) && err.status === 403) {
                    setProviderLoadError('permission');
                    return;
                }

                setProviderLoadError('generic');
                dispatch(showApiErrorSnackbar(err, 'Die Liste der Speicheranbieter konnte nicht geladen werden.'));
            })
            .finally(() => {
                if (providerLoadRequestRef.current === requestId) {
                    setIsLoadingProviders(false);
                }
            });
    }, [canReadAssets, dispatch]);

    useEffect(() => {
        if (!show) {
            return;
        }

        loadProviders();

        return () => {
            providerLoadRequestRef.current += 1;
        };
    }, [loadProviders, show]);

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

    const mimeTypeFilterLabel = useMemo(() => getFileTypeFilterSummary(normalizedMimeTypes), [normalizedMimeTypes]);
    const hasSelectionCriteria = normalizedMimeTypes.length > 0 || mode === 'public';

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

        if (item.missing) {
            dispatch(showErrorSnackbar('Die ausgewählte Datei fehlt im Speicher und kann nicht ausgewählt werden.'));
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
                if (!canUpdateAssets) {
                    dispatch(showErrorSnackbar(
                        `Die Datei kann nicht ausgewählt werden, weil sie dafür veröffentlicht werden müsste. ${formatMissingPermissionTooltip(Permission.ASSET_UPDATE)}`,
                    ));
                    return;
                }

                const providerName = providers.find((provider) => provider.id === item.storageProviderId)?.name
                    ?? 'Unbekannter Speicheranbieter';

                const confirmed = await confirm({
                    title: 'Datei öffentlich schalten?',
                    confirmButtonText: 'Öffentlich schalten und auswählen',
                    width: 'md',
                    children: (
                        <Stack spacing={2}>
                            <Typography variant="body2">
                                Die ausgewählte Datei ist aktuell nicht öffentlich erreichbar.
                            </Typography>
                            <Box
                                sx={{
                                    border: '1px solid',
                                    borderColor: 'divider',
                                    borderRadius: 1,
                                    bgcolor: 'action.hover',
                                    p: 1.5,
                                }}
                            >
                                <Stack spacing={0.75}>
                                    <Typography variant="body2">
                                        <Box component="span"
                                             sx={{fontWeight: 600}}>Datei:</Box> {item.filename}
                                    </Typography>
                                    <Typography variant="body2">
                                        <Box component="span"
                                             sx={{fontWeight: 600}}>Pfad:</Box> {item.pathFromRoot}
                                    </Typography>
                                    <Typography variant="body2">
                                        <Box component="span"
                                             sx={{fontWeight: 600}}>Speicheranbieter:</Box> {providerName}
                                    </Typography>
                                </Stack>
                            </Box>
                            <Typography variant="body2">
                                Wenn Sie fortfahren, wird der öffentliche Zugriff aktiviert und die Datei danach direkt
                                ausgewählt.
                            </Typography>
                            <Alert severity="warning">
                                Nach dem Veröffentlichen kann die Datei ohne Anmeldung abgerufen werden. Stellen Sie
                                sicher, dass sie keine vertraulichen oder personenbezogenen Daten und keine
                                sicherheitsrelevanten Dateien wie Zertifikate oder private Schlüssel enthält.
                            </Alert>
                        </Stack>
                    ),
                });

                if (!confirmed) {
                    return;
                }

                startSelectionProcessing('Datei wird veröffentlicht…');

                const assetToPublish = resolvedAsset ?? (await loadAssetForSelection(item));
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
        canUpdateAssets,
        confirm,
        dispatch,
        loadAssetForSelection,
        mode,
        onSelectAsset,
        providers,
        startSelectionProcessing,
        stopSelectionProcessing,
    ]);

    return (
        <Dialog
            fullWidth
            maxWidth="lg"
            open={show}
            onClose={handleDialogClose}
            slotProps={{
                paper: {
                    id,
                },
            }}
        >
            <DialogTitleWithClose onClose={handleDialogClose}>
                {title}
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
                    <Grid container={true}
                          spacing={1.5}>
                        <Grid size={{xs: 12, sm: hasSelectionCriteria ? 6 : 12}}>
                            <SelectFieldComponent
                                label="Speicheranbieter"
                                value={selectedProviderId?.toString()}
                                onChange={(nextProviderId) => {
                                    if (nextProviderId == null) {
                                        setSelectedProviderId(undefined);
                                        return;
                                    }

                                    const parsedProviderId = Number.parseInt(nextProviderId, 10);
                                    setSelectedProviderId(Number.isNaN(parsedProviderId) ? undefined : parsedProviderId);
                                }}
                                options={providers.map((provider) => ({
                                    value: provider.id.toString(),
                                    label: provider.name,
                                }))}
                                includeEmptyOption={false}
                                emptyStatePlaceholder="Keine Speicheranbieter verfügbar"
                                disabled={isLoadingProviders || providers.length === 0 || isProcessingSelection}
                                showOptionalIndicator={false}
                                margin="none"
                            />
                        </Grid>

                        {hasSelectionCriteria && (
                            <Grid size={{xs: 12, sm: 6}}>
                                <FormField
                                    label="Auswahlkriterien"
                                    readOnly
                                    showOptionalIndicator={false}
                                    margin="none"
                                >
                                    {(field) => (
                                        <Box
                                            id={field.controlId}
                                            role="group"
                                            {...field.ariaProps}
                                            sx={{
                                                minHeight: FormFieldTokens.controlMinHeight,
                                                display: 'flex',
                                                alignItems: 'center',
                                                px: 1.5,
                                                py: 0.5,
                                                border: '1px solid',
                                                borderColor: 'divider',
                                                borderRadius: 1,
                                            }}
                                        >
                                            <Stack
                                                direction="row"
                                                spacing={0.75}
                                                useFlexGap
                                                sx={{
                                                    minWidth: 0,
                                                    flexWrap: 'wrap',
                                                    alignItems: 'center',
                                                }}
                                            >
                                                {normalizedMimeTypes.length > 0 && (
                                                    <Chip
                                                        size="small"
                                                        variant="outlined"
                                                        label={`Dateityp: ${mimeTypeFilterLabel}`}
                                                    />
                                                )}
                                                {mode === 'public' && (
                                                    <Chip
                                                        size="small"
                                                        variant="outlined"
                                                        color="info"
                                                        label="Öffentlicher Zugriff erforderlich"
                                                    />
                                                )}
                                            </Stack>
                                        </Box>
                                    )}
                                </FormField>
                            </Grid>
                        )}
                    </Grid>

                    {isLoadingProviders && (
                        <Stack
                            direction="row"
                            spacing={1.5}
                            sx={{
                                alignItems: "center",
                                py: 3,
                                justifyContent: 'center'
                            }}>
                            <CircularProgress size={18}/>
                            <Typography variant="body2"
                                        sx={{
                                            color: "text.secondary"
                                        }}>
                                Speicheranbieter werden geladen…
                            </Typography>
                        </Stack>
                    )}

                    {!isLoadingProviders && providerLoadError === 'permission' && (
                        <Alert severity="warning">
                            Sie haben keine Berechtigung zum Anzeigen von Dateien und Medien. Für die Auswahl einer
                            Datei ist die Berechtigung <Box component="code">asset.read</Box> erforderlich.
                        </Alert>
                    )}

                    {!isLoadingProviders && providerLoadError === 'generic' && (
                        <Alert
                            severity="error"
                            action={(
                                <Button
                                    color="inherit"
                                    size="small"
                                    onClick={loadProviders}
                                >
                                    Erneut versuchen
                                </Button>
                            )}
                        >
                            Die Speicheranbieter konnten nicht geladen werden.
                        </Alert>
                    )}

                    {!isLoadingProviders && providerLoadError == null && providers.length === 0 && (
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
                                disableMissingFiles={true}
                                showTopNavigationBar={true}
                                minGridHeight={460}
                            />

                            {isProcessingSelection && (
                                <Stack
                                    spacing={1.5}
                                    sx={{
                                        alignItems: "center",
                                        justifyContent: "center",
                                        position: 'absolute',
                                        inset: 0,
                                        zIndex: 1,
                                        bgcolor: 'rgba(255, 255, 255, 0.5)',
                                        pointerEvents: 'all'
                                    }}>
                                    <CircularProgress size={20}/>
                                    <Typography
                                        variant="body2"
                                        sx={{
                                            color: "text.secondary"
                                        }}
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
