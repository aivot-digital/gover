import {SelectAssetDialog} from '../../../dialogs/select-asset-dialog/select-asset-dialog';
import {useEffect, useState} from 'react';
import {
    Box,
    CircularProgress,
    FormControl,
    FormHelperText,
    IconButton,
    InputAdornment,
    InputLabel,
    OutlinedInput,
    Tooltip,
    Typography,
} from '@mui/material';
import ChevronRight from '@aivot/mui-material-symbols-400-n25-outlined/ChevronRight';
import Close from '@aivot/mui-material-symbols-400-n25-outlined/Close';
import ImageOutlined from '@aivot/mui-material-symbols-400-n25-outlined/Image';
import BrokenImageOutlined from '@aivot/mui-material-symbols-400-n25-outlined/BrokenImage';
import {AssetsApiService} from '../assets-api-service';
import {type VStorageIndexItemWithAssetEntity} from '../../storage/entities/storage-index-item-entity';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar} from '../../../slices/snackbar-slice';
import {useHasSystemPermission} from '../../permissions/hooks/use-permissions';
import {Permission} from '../../../data/permissions/permission';
import {isApiError} from '../../../models/api-error';

interface ImageSelectorProps {
    label: string;
    hint: string;
    selectLabel: string;
    size: {
        width: number | string;
        height: number | string;
    } | {
        aspectRatio: number;
    };
    value: string | null;
    onChange: (value: string | null) => void;
    disabled?: boolean;
    required?: boolean;
    error?: string;
    previewBackgroundColor?: string;
    previewForegroundColor?: string;
    previewBorderColor?: string;
    previewImageFilter?: string;
}

export function ImageSelector(props: ImageSelectorProps) {
    const {
        label,
        hint,
        selectLabel,
        size,
        value,
        onChange,
        disabled,
        required,
        error,
        previewBackgroundColor,
        previewForegroundColor,
        previewBorderColor,
        previewImageFilter,
    } = props;

    const dispatch = useAppDispatch();
    const canReadAssets = useHasSystemPermission(Permission.ASSET_READ);
    const [showSelectAssetDialog, setShowSelectAssetDialog] = useState(false);
    const [asset, setAsset] = useState<VStorageIndexItemWithAssetEntity>();
    const [assetLoadFailed, setAssetLoadFailed] = useState(false);
    const [imageStatus, setImageStatus] = useState<'idle' | 'loading' | 'loaded' | 'failed'>(
        value == null ? 'idle' : 'loading',
    );

    const link = value == null
        ? null
        : AssetsApiService.useAssetLink(value);

    useEffect(() => {
        setImageStatus(link == null ? 'idle' : 'loading');
    }, [link]);

    useEffect(() => {
        let active = true;

        setAsset(undefined);
        setAssetLoadFailed(false);

        if (value == null) {
            return () => {
                active = false;
            };
        }

        // Existing public images can still be previewed by key. Metadata lookup requires asset.read and
        // should not create permission toasts when the surrounding form is opened read-only.
        if (!canReadAssets) {
            setAssetLoadFailed(true);
            return () => {
                active = false;
            };
        }

        void new AssetsApiService()
            .retrieveByKey(value)
            .then((res) => {
                if (active) {
                    setAsset(res);
                }
            })
            .catch((err) => {
                if (active) {
                    setAssetLoadFailed(true);
                    if (!isApiError(err) || err.status !== 403) {
                        dispatch(showApiErrorSnackbar(err, 'Asset konnte nicht geladen werden'));
                    }
                }
            });

        return () => {
            active = false;
        };
    }, [canReadAssets, dispatch, value]);

    const handleOpenDialog = () => {
        if (!disabled) {
            setShowSelectAssetDialog(true);
        }
    };

    const handleClear = (event: React.MouseEvent<HTMLButtonElement>) => {
        event.preventDefault();
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

    const isLoadingAsset = value != null && asset == null && !assetLoadFailed;
    const helperText = error ?? hint;
    const hasImageFailed = imageStatus === 'failed';
    const hasExplicitSize = 'width' in size;
    const previewWidth = hasExplicitSize ? size.width : '8rem';
    const previewHeight = hasExplicitSize ? size.height : undefined;
    const previewAspectRatio = hasExplicitSize ? undefined : size.aspectRatio;
    const fieldCursor = disabled ? 'default' : 'pointer';
    const primaryContentColor = disabled ? 'text.disabled' : 'text.primary';
    const secondaryContentColor = disabled ? 'text.disabled' : 'text.secondary';
    const previewIconColor = hasImageFailed
        ? 'error.main'
        : previewForegroundColor ?? (disabled ? 'text.disabled' : 'action.active');
    const endIconColor = disabled ? 'text.disabled' : 'action.active';
    const selectedAssetPath = asset?.pathFromRoot;

    return (
        <>
            <FormControl
                fullWidth
                variant="outlined"
                error={error != null}
                disabled={disabled}
                required={required}
            >
                <InputLabel
                    shrink
                    title={label}
                >
                    {label}
                </InputLabel>

                <OutlinedInput
                    label={label}
                    notched
                    value=""
                    readOnly
                    disabled={disabled}
                    onClick={handleOpenDialog}
                    onKeyDown={handleKeyDown}
                    inputProps={{
                        readOnly: true,
                        'aria-label': label,
                    }}
                    startAdornment={(
                        <InputAdornment
                            position="start"
                            sx={{
                                minWidth: 0,
                                flex: 1,
                                alignItems: 'center',
                                maxHeight: 'none',
                                mr: 1,
                            }}
                        >
                            <Box
                                component="span"
                                sx={(theme) => ({
                                    position: 'relative',
                                    display: 'inline-flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    flexShrink: 0,
                                    width: previewWidth,
                                    height: previewHeight,
                                    aspectRatio: previewAspectRatio,
                                    maxWidth: hasExplicitSize ? undefined : {
                                        xs: '5.5rem',
                                        sm: '8rem',
                                    },
                                    maxHeight: hasExplicitSize ? undefined : '5rem',
                                    overflow: 'hidden',
                                    border: `1px solid ${previewBorderColor ?? theme.palette.divider}`,
                                    borderRadius: 1,
                                    backgroundColor: previewBackgroundColor ?? (
                                        disabled ? theme.palette.action.disabledBackground : theme.palette.action.hover
                                    ),
                                    color: previewIconColor,
                                })}
                            >
                                {
                                    link != null && !hasImageFailed &&
                                    <Box
                                        component="img"
                                        key={link}
                                        src={link}
                                        alt=""
                                        onLoad={() => {
                                            setImageStatus('loaded');
                                        }}
                                        onError={() => {
                                            setImageStatus('failed');
                                        }}
                                        sx={{
                                            position: 'absolute',
                                            inset: 0,
                                            width: '100%',
                                            height: '100%',
                                            objectFit: 'contain',
                                            display: 'block',
                                            p: 0.75,
                                            filter: previewImageFilter,
                                        }}
                                    />
                                }

                                {
                                    imageStatus === 'loading' &&
                                    <CircularProgress
                                        size={22}
                                        color="inherit"
                                    />
                                }

                                {
                                    (link == null || hasImageFailed) &&
                                    (
                                        hasImageFailed
                                            ? <BrokenImageOutlined fontSize="small" />
                                            : <ImageOutlined fontSize="small" />
                                    )
                                }
                            </Box>

                            <Box
                                component="span"
                                sx={{
                                    minWidth: 0,
                                    flex: 1,
                                    ml: 1.5,
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
                                            Lade Bild
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
                                    ) : assetLoadFailed ? (
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
                                                }}
                                            >
                                                Bild ausgewählt
                                            </Typography>

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
                                            >
                                                Metadaten nicht verfügbar
                                            </Typography>
                                        </>
                                    ) : (
                                        <>
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
                                                Kein Bild ausgewählt
                                            </Typography>

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
                                            >
                                                Zum Auswählen klicken
                                            </Typography>
                                        </>
                                    )
                                }
                            </Box>
                        </InputAdornment>
                    )}
                    endAdornment={(
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
                                    title={disabled || value == null ? '' : 'Auswahl entfernen'}
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
                                            aria-label={`${label} entfernen`}
                                        >
                                            <Close fontSize="small" />
                                        </IconButton>
                                    </span>
                                </Tooltip>

                                <ChevronRight
                                    fontSize="small"
                                    sx={{color: endIconColor}}
                                />
                            </Box>
                        </InputAdornment>
                    )}
                    sx={{
                        minHeight: 96,
                        alignItems: 'center',
                        cursor: fieldCursor,
                        '& .MuiOutlinedInput-input': {
                            width: 0,
                            minWidth: 0,
                            flex: '0 0 0',
                            p: 0,
                            cursor: fieldCursor,
                            caretColor: 'transparent',
                        },
                        '& .MuiInputAdornment-root': {
                            pointerEvents: disabled ? 'none' : 'auto',
                        },
                    }}
                />

                {
                    helperText != null &&
                    <FormHelperText
                        title={helperText}
                        sx={{whiteSpace: 'normal'}}
                    >
                        {helperText}
                    </FormHelperText>
                }
            </FormControl>

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
                mode="public"
                mimetype="image"
            />
        </>
    );
}
