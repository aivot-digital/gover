import {useEffect, useState} from 'react';
import {Box, CircularProgress, type SxProps, type Theme} from '@mui/material';
import ImageOutlined from '@aivot/mui-material-symbols-400-n25-outlined/Image';
import BrokenImageOutlined from '@aivot/mui-material-symbols-400-n25-outlined/BrokenImage';
import {SelectAssetDialog} from '../../../dialogs/select-asset-dialog/select-asset-dialog';
import {type FormFieldLayoutProps} from '../../../components/form-field';
import {AssetsApiService} from '../assets-api-service';
import {type VStorageIndexItemWithAssetEntity} from '../../storage/entities/storage-index-item-entity';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar} from '../../../slices/snackbar-slice';
import {useHasSystemPermission} from '../../permissions/hooks/use-permissions';
import {Permission} from '../../../data/permissions/permission';
import {isApiError} from '../../../models/api-error';
import {AssetSelectionField} from './asset-selection-field';
import {useNormalizedReactId} from '../../../hooks/use-normalized-react-id';

interface ImageSelectorProps extends FormFieldLayoutProps {
    label: string;
    hint?: string;
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
    readOnly?: boolean;
    busy?: boolean;
    required?: boolean;
    error?: string;
    previewBackgroundColor?: string;
    previewForegroundColor?: string;
    previewBorderColor?: string;
    previewImageFilter?: string;
    controlSx?: SxProps<Theme>;
}

export function ImageSelector(props: ImageSelectorProps) {
    const {
        label,
        hint,
        selectLabel,
        size,
        value,
        onChange,
        disabled = false,
        readOnly = false,
        busy = false,
        required = false,
        error,
        previewBackgroundColor,
        previewForegroundColor,
        previewBorderColor,
        previewImageFilter,
    } = props;
    const generatedId = useNormalizedReactId();
    const dialogId = `${props.id ?? `image-selector-${generatedId}`}-dialog`;
    const dispatch = useAppDispatch();
    const canReadAssets = useHasSystemPermission(Permission.ASSET_READ);
    const [showSelectAssetDialog, setShowSelectAssetDialog] = useState(false);
    const [asset, setAsset] = useState<VStorageIndexItemWithAssetEntity>();
    const [assetLoadFailed, setAssetLoadFailed] = useState(false);
    const [imageStatus, setImageStatus] = useState<'idle' | 'loading' | 'loaded' | 'failed'>(
        value == null ? 'idle' : 'loading',
    );
    const link = value == null ? null : AssetsApiService.useAssetLink(value);

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

    const isLoadingAsset = value != null && asset == null && !assetLoadFailed;
    const hasImageFailed = imageStatus === 'failed';
    const hasExplicitSize = 'width' in size;
    const previewWidth = hasExplicitSize ? size.width : '8rem';
    const previewHeight = hasExplicitSize ? size.height : undefined;
    const previewAspectRatio = hasExplicitSize ? undefined : size.aspectRatio;
    const isInteractionDisabled = disabled || readOnly || busy;
    const previewIconColor = hasImageFailed
        ? 'error.main'
        : previewForegroundColor ?? (isInteractionDisabled ? 'text.disabled' : 'action.active');
    const primaryText = isLoadingAsset
        ? 'Lade Bild'
        : asset?.filename ?? (value != null ? 'Bild ausgewählt' : 'Kein Bild ausgewählt');
    const secondaryText = asset?.pathFromRoot
        ?? (assetLoadFailed ? 'Metadaten nicht verfügbar' : value == null ? 'Zum Auswählen öffnen' : undefined);

    return (
        <>
            <AssetSelectionField
                id={props.id}
                label={label}
                ariaLabel={props.ariaLabel}
                ariaDescribedBy={props.ariaDescribedBy}
                labelAction={props.labelAction}
                hint={hint}
                error={error}
                required={required}
                disabled={disabled}
                readOnly={readOnly}
                busy={busy}
                margin={props.margin}
                sx={props.sx}
                showOptionalIndicator={props.showOptionalIndicator}
                open={showSelectAssetDialog}
                dialogId={dialogId}
                hasValue={value != null}
                primaryText={primaryText}
                secondaryText={secondaryText}
                minHeight={96}
                leadingVisual={(
                    <Box
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
                                isInteractionDisabled
                                    ? theme.palette.action.disabledBackground
                                    : theme.palette.action.hover
                            ),
                            color: previewIconColor,
                        })}
                    >
                        {link != null && !hasImageFailed && (
                            <Box
                                component="img"
                                key={link}
                                src={link}
                                alt=""
                                onLoad={() => setImageStatus('loaded')}
                                onError={() => setImageStatus('failed')}
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
                        )}

                        {imageStatus === 'loading' && (
                            <CircularProgress size={22} color="inherit" />
                        )}

                        {(link == null || hasImageFailed) && (
                            hasImageFailed
                                ? <BrokenImageOutlined fontSize="small" />
                                : <ImageOutlined fontSize="small" />
                        )}
                    </Box>
                )}
                onOpen={() => setShowSelectAssetDialog(true)}
                onClear={() => onChange(null)}
                controlSx={props.controlSx}
            />

            <SelectAssetDialog
                id={dialogId}
                title={selectLabel}
                show={showSelectAssetDialog}
                onSelect={(selectedValue) => {
                    onChange(selectedValue);
                    setShowSelectAssetDialog(false);
                }}
                onCancel={() => setShowSelectAssetDialog(false)}
                mode="public"
                mimetype="image"
            />
        </>
    );
}
