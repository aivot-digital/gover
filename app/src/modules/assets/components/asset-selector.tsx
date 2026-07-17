import {SelectAssetDialog} from '../../../dialogs/select-asset-dialog/select-asset-dialog';
import React, {useEffect, useMemo, useState} from 'react';
import {Box, CircularProgress, IconButton, InputAdornment, TextField, Tooltip, Typography} from '@mui/material';
import ChevronRight from '@aivot/mui-material-symbols-400-n25-outlined/ChevronRight';
import Close from '@aivot/mui-material-symbols-400-n25-outlined/Close';
import {AssetsApiService} from '../assets-api-service';
import {type VStorageIndexItemWithAssetEntity} from '../../storage/entities/storage-index-item-entity';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar} from '../../../slices/snackbar-slice';
import {StorageProvidersApiService} from '../../storage/storage-providers-api-service';
import FileOpen from '@aivot/mui-material-symbols-400-n25-outlined/FileOpen';

interface AssetSelectorProps {
    label: string;
    hint?: string;
    selectLabel: string;
    value: string | null;
    onChange: (value: string | null) => void;
    disabled?: boolean;
    required?: boolean;
    error?: string;
    mimetype?: string;
    onlyPublic?: boolean;
    placeholder?: string;
}

export function AssetSelector(props: AssetSelectorProps): React.ReactElement {
    const {
        label,
        hint,
        selectLabel,
        value,
        onChange,
        disabled = false,
        required = false,
        error,
        mimetype,
        onlyPublic,
        placeholder = 'Keine Datei ausgewählt',
    } = props;

    const dispatch = useAppDispatch();
    const [showSelectAssetDialog, setShowSelectAssetDialog] = useState(false);
    const [asset, setAsset] = useState<VStorageIndexItemWithAssetEntity>();
    const [storageProviderName, setStorageProviderName] = useState<string>();

    useEffect(() => {
        let active = true;

        setAsset(undefined);
        setStorageProviderName(undefined);

        if (value == null) {
            return () => {
                active = false;
            };
        }

        void new AssetsApiService()
            .retrieveByKey(value)
            .then((res) => {
                if (!active) {
                    return;
                }

                setAsset(res);

                return new StorageProvidersApiService()
                    .retrieve(res.storageProviderId)
                    .then((provider) => {
                        if (!active) {
                            return;
                        }

                        setStorageProviderName(provider.name);
                    })
                    .catch((err) => {
                        if (!active) {
                            return;
                        }

                        dispatch(showApiErrorSnackbar(err, 'Speicheranbieter konnte nicht geladen werden'));
                    });
            })
            .catch((err) => {
                if (!active) {
                    return;
                }

                dispatch(showApiErrorSnackbar(err, 'Asset konnte nicht geladen werden'));
            });

        return () => {
            active = false;
        };
    }, [dispatch, value]);

    const selectedAssetPath = useMemo(() => {
        if (asset == null) {
            return undefined;
        }

        const providerLabel = storageProviderName?.trim().length
            ? storageProviderName
            : `Speicheranbieter ${asset.storageProviderId}`;
        return `${providerLabel}: ${asset.pathFromRoot}`;
    }, [asset, storageProviderName]);

    const isLoadingAsset = value != null && asset == null;
    const fieldValue = asset?.filename ?? '';
    const interactiveCursor = disabled ? 'default' : 'pointer';
    const primaryContentColor = disabled ? 'text.disabled' : 'text.primary';
    const secondaryContentColor = disabled ? 'text.disabled' : 'text.secondary';
    const iconColor = disabled ? 'text.disabled' : asset != null ? 'primary.main' : 'action.active';
    const endIconColor = disabled ? 'text.disabled' : 'action.active';
    const clearTooltip = disabled || value == null ? '' : 'Auswahl entfernen';

    const handleOpenDialog = () => {
        if (!disabled) {
            setShowSelectAssetDialog(true);
        }
    };

    const handleClear = (event: React.MouseEvent<HTMLButtonElement>) => {
        event.stopPropagation();
        onChange(null);
    };

    const handleKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
        if (disabled) {
            return;
        }

        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            setShowSelectAssetDialog(true);
        }
    };

    return (
        <>
            <TextField
                fullWidth
                label={label}
                value={fieldValue}
                placeholder={placeholder}
                disabled={disabled}
                error={error != null}
                helperText={error ?? hint}
                required={required}
                onClick={handleOpenDialog}
                onKeyDown={handleKeyDown}
                InputLabelProps={{
                    title: label,
                }}
                FormHelperTextProps={{
                    title: error ?? hint,
                    sx: {
                        whiteSpace: 'normal',
                    },
                }}
                inputProps={{
                    readOnly: true,
                    title: asset?.filename,
                    'aria-label': label,
                }}
                InputProps={{
                    startAdornment: (
                        <InputAdornment
                            position="start"
                            sx={{
                                minWidth: 0,
                                flex: 1,
                                alignItems: 'center',
                                mr: 1,
                            }}
                        >
                            <Box
                                component="span"
                                sx={{
                                    display: 'inline-flex',
                                    flexShrink: 0,
                                    mr: 1.25,
                                    color: iconColor,
                                    '& .MuiSvgIcon-root': {
                                        fontSize: 20,
                                    },
                                }}
                            >
                                {
                                    isLoadingAsset ? (
                                        <CircularProgress
                                            size={20}
                                            color="inherit"
                                        />
                                    ) : (
                                        <FileOpen/>
                                    )
                                }
                            </Box>

                            <Box
                                component="span"
                                sx={{
                                    minWidth: 0,
                                    flex: 1,
                                }}
                            >
                                {
                                    isLoadingAsset ? (
                                        <Typography
                                            variant="body2"
                                            component="span"
                                            color={secondaryContentColor}
                                            sx={{
                                                display: 'block',
                                                overflow: 'hidden',
                                                textOverflow: 'ellipsis',
                                                whiteSpace: 'nowrap',
                                            }}
                                        >
                                            Lade {label}
                                        </Typography>
                                    ) : asset != null ? (
                                        <>
                                            <Typography
                                                variant="body2"
                                                component="span"
                                                color={primaryContentColor}
                                                sx={{
                                                    display: 'block',
                                                    overflow: 'hidden',
                                                    textOverflow: 'ellipsis',
                                                    whiteSpace: 'nowrap',
                                                    fontSize: '1rem',
                                                    lineHeight: 1.25,
                                                }}
                                                title={asset.filename}
                                            >
                                                {asset.filename}
                                            </Typography>

                                            {
                                                selectedAssetPath != null &&
                                                <Typography
                                                    variant="caption"
                                                    component="span"
                                                    color={secondaryContentColor}
                                                    sx={{
                                                        display: 'block',
                                                        overflow: 'hidden',
                                                        textOverflow: 'ellipsis',
                                                        whiteSpace: 'nowrap',
                                                        fontSize: '0.75rem',
                                                        lineHeight: 1.2,
                                                    }}
                                                    title={selectedAssetPath}
                                                >
                                                    {selectedAssetPath}
                                                </Typography>
                                            }
                                        </>
                                    ) : (
                                        <Typography
                                            variant="body2"
                                            component="span"
                                            color={secondaryContentColor}
                                            sx={{
                                                display: 'block',
                                                overflow: 'hidden',
                                                textOverflow: 'ellipsis',
                                                whiteSpace: 'nowrap',
                                            }}
                                        >
                                            {placeholder}
                                        </Typography>
                                    )
                                }
                            </Box>
                        </InputAdornment>
                    ),
                    endAdornment: (
                        <InputAdornment position="end">
                            <Box
                                sx={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: 0.5,
                                    mr: -0.5,
                                }}
                            >
                                <Tooltip
                                    title={clearTooltip}
                                    arrow
                                >
                                    <span>
                                        <IconButton
                                            size="small"
                                            onClick={handleClear}
                                            onMouseDown={(event) => {
                                                event.preventDefault();
                                                event.stopPropagation();
                                            }}
                                            disabled={disabled || value == null}
                                            aria-label="Auswahl entfernen"
                                        >
                                            <Close fontSize="small"/>
                                        </IconButton>
                                    </span>
                                </Tooltip>

                                <ChevronRight
                                    fontSize="small"
                                    sx={{color: endIconColor}}
                                />
                            </Box>
                        </InputAdornment>
                    ),
                }}
                sx={{
                    '& .MuiOutlinedInput-root': {
                        cursor: interactiveCursor,
                        height: 56,
                        minHeight: 56,
                    },
                    '& .MuiOutlinedInput-input': {
                        width: 0,
                        minWidth: 0,
                        flex: '0 0 0',
                        p: 0,
                        cursor: interactiveCursor,
                        caretColor: 'transparent',
                    },
                    '& .MuiInputAdornment-root': {
                        pointerEvents: disabled ? 'none' : 'auto',
                    },
                }}
            />

            <SelectAssetDialog
                title={selectLabel}
                show={showSelectAssetDialog}
                onSelect={(val) => {
                    onChange(val);
                    setShowSelectAssetDialog(false);
                }}
                onCancel={() => {
                    setShowSelectAssetDialog(false);
                }}
                mode={onlyPublic ? 'public' : 'all'}
                mimetype={mimetype}
            />
        </>
    );
}
