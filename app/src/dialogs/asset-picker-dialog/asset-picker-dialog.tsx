import React, {type PropsWithChildren, useCallback, useEffect, useMemo, useState} from 'react';
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
import {showApiErrorSnackbar, showErrorSnackbar} from '../../slices/snackbar-slice';
import {type StorageIndexItem} from '../../modules/storage/entities/storage-index-item-entity';
import {StorageProvidersApiService} from '../../modules/storage/storage-providers-api-service';
import {StorageProviderType} from '../../modules/storage/enums/storage-provider-type';
import {AssetsApiService} from '../../modules/assets/assets-api-service';
import {type StorageProviderEntity} from '../../modules/storage/entities/storage-provider-entity';
import {AssetExplorer} from '../../modules/storage/components/asset-explorer';

const ASSET_KEY_METADATA_KEY = '__assetPickerAssetKey';
const ASSET_PRIVATE_METADATA_KEY = '__assetPickerIsPrivate';

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
    const [providers, setProviders] = useState<StorageProviderEntity[]>([]);
    const [isLoadingProviders, setIsLoadingProviders] = useState(false);
    const [selectedProviderId, setSelectedProviderId] = useState<number>();

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
                dispatch(showApiErrorSnackbar(err, 'Die Liste der Asset-Speicheranbieter konnte nicht geladen werden.'));
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

    const normalizedMimeTypes = useMemo(() => {
        if (mimeType == null) {
            return [];
        }

        const mimeTypes = Array.isArray(mimeType) ? mimeType : [mimeType];
        return mimeTypes
            .map((entry) => entry.trim())
            .filter((entry) => entry.length > 0);
    }, [mimeType]);

    const handleSelectFile = useCallback(async (item: StorageIndexItem) => {
        if (item.directory) {
            return;
        }

        const rawAssetKey = item.metadata?.[ASSET_KEY_METADATA_KEY];
        const itemIsPrivate = item.metadata?.[ASSET_PRIVATE_METADATA_KEY] === true;
        const assetKey = typeof rawAssetKey === 'string'
            ? rawAssetKey
            : '';

        if (mode === 'public' && itemIsPrivate) {
            dispatch(showErrorSnackbar('Es können nur öffentlich erreichbare Assets ausgewählt werden.'));
            return;
        }

        if (assetKey.length > 0) {
            onSelectAsset(assetKey, item.pathFromRoot, item.storageProviderId);
            return;
        }

        try {
            const asset = await new AssetsApiService(api).retrieveInStorageProvider(item.pathFromRoot, item.storageProviderId);
            if (asset.key.trim().length === 0) {
                dispatch(showErrorSnackbar('Die ausgewählte Datei ist keinem Asset zugeordnet und kann nicht ausgewählt werden.'));
                return;
            }

            if (mode === 'public' && asset.isPrivate) {
                dispatch(showErrorSnackbar('Es können nur öffentlich erreichbare Assets ausgewählt werden.'));
                return;
            }

            onSelectAsset(asset.key, asset.storagePathFromRoot, asset.storageProviderId);
        } catch (err) {
            dispatch(showApiErrorSnackbar(err, 'Die ausgewählte Datei konnte nicht als Asset geladen werden.'));
        }
    }, [api, dispatch, mode, onSelectAsset]);

    return (
        <Dialog
            fullWidth
            maxWidth="lg"
            open={show}
            onClose={onCancel}
        >
            <DialogTitleWithClose onClose={onCancel}>
                {title} ({mimeType}) {mode === 'public' && `(nur öffentlich)`}
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
                    <TextField
                        select={true}
                        size="small"
                        label="Speicheranbieter"
                        value={selectedProviderId ?? ''}
                        onChange={(event) => {
                            const nextProviderId = Number.parseInt(event.target.value, 10);
                            setSelectedProviderId(Number.isNaN(nextProviderId) ? undefined : nextProviderId);
                        }}
                        disabled={isLoadingProviders || providers.length === 0}
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
                            Es sind keine Asset-Speicheranbieter konfiguriert. Gehen Sie zu{' '}
                            <Link to="/storage-providers"
                                  style={{color: 'inherit'}}>
                                Speicheranbieter
                            </Link>
                            {' '}und richten Sie einen Asset-Speicheranbieter ein.
                        </Alert>
                    )}

                    {!isLoadingProviders && providers.length > 0 && selectedProviderId == null && (
                        <Alert severity="info">
                            Wählen Sie einen Speicheranbieter aus, um dessen Ordner und Dateien zu durchsuchen.
                        </Alert>
                    )}

                    {!isLoadingProviders && selectedProviderId != null && (
                        <AssetExplorer
                            providerId={selectedProviderId}
                            onFileSelect={handleSelectFile}
                            filterMimeTypes={normalizedMimeTypes}
                            filterOnlyPublic={mode === 'public'}
                            showTopNavigationBar={true}
                            minGridHeight={460}
                        />
                    )}
                </Stack>
            </DialogContent>
        </Dialog>
    );
}
