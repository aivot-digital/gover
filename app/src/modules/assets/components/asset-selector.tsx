import {useEffect, useMemo, useState} from 'react';
import {CircularProgress, type SxProps, type Theme} from '@mui/material';
import FileOpen from '@aivot/mui-material-symbols-400-n25-outlined/FileOpen';
import {SelectAssetDialog} from '../../../dialogs/select-asset-dialog/select-asset-dialog';
import {type FormFieldLayoutProps} from '../../../components/form-field';
import {AssetsApiService} from '../assets-api-service';
import {type VStorageIndexItemWithAssetEntity} from '../../storage/entities/storage-index-item-entity';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar} from '../../../slices/snackbar-slice';
import {AssetSelectionField} from './asset-selection-field';
import {useNormalizedReactId} from '../../../hooks/use-normalized-react-id';

interface AssetSelectorProps extends FormFieldLayoutProps {
    label: string;
    hint?: string;
    selectLabel: string;
    value: string | null;
    onChange: (value: string | null) => void;
    disabled?: boolean;
    readOnly?: boolean;
    required?: boolean;
    error?: string;
    mimetype?: string;
    onlyPublic?: boolean;
    placeholder?: string;
    isBusy?: boolean;
    controlSx?: SxProps<Theme>;
}

export function AssetSelector(props: AssetSelectorProps) {
    const {
        label,
        hint,
        selectLabel,
        value,
        onChange,
        disabled = false,
        readOnly = false,
        required = false,
        error,
        mimetype,
        onlyPublic,
        placeholder = 'Keine Datei ausgewählt',
        isBusy = false,
    } = props;
    const generatedId = useNormalizedReactId();
    const dialogId = `${props.id ?? `asset-selector-${generatedId}`}-dialog`;
    const dispatch = useAppDispatch();
    const [showSelectAssetDialog, setShowSelectAssetDialog] = useState(false);
    const [asset, setAsset] = useState<VStorageIndexItemWithAssetEntity>();
    const [storageProviderName, setStorageProviderName] = useState<string>();
    const [assetLoadFailed, setAssetLoadFailed] = useState(false);

    useEffect(() => {
        let active = true;

        setAsset(undefined);
        setStorageProviderName(undefined);
        setAssetLoadFailed(false);

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

                return new AssetsApiService()
                    .retrieveStorageProvider(res.storageProviderId)
                    .then((provider) => {
                        if (active) {
                            setStorageProviderName(provider.name);
                        }
                    })
                    .catch((err) => {
                        if (active) {
                            dispatch(showApiErrorSnackbar(err, 'Speicheranbieter konnte nicht geladen werden'));
                        }
                    });
            })
            .catch((err) => {
                if (active) {
                    setAssetLoadFailed(true);
                    dispatch(showApiErrorSnackbar(err, 'Asset konnte nicht geladen werden'));
                }
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

    const isLoadingAsset = value != null && asset == null && !assetLoadFailed;
    const primaryText = isLoadingAsset
        ? `Lade ${label}`
        : asset?.filename ?? (assetLoadFailed ? 'Datei ausgewählt' : placeholder);
    const secondaryText = asset != null
        ? selectedAssetPath
        : assetLoadFailed
            ? 'Metadaten nicht verfügbar'
            : undefined;
    const isInteractionDisabled = disabled || readOnly || isBusy;

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
                busy={isBusy}
                margin={props.margin}
                sx={props.sx}
                showOptionalIndicator={props.showOptionalIndicator}
                open={showSelectAssetDialog}
                dialogId={dialogId}
                hasValue={value != null}
                primaryText={primaryText}
                secondaryText={secondaryText}
                leadingVisual={isLoadingAsset ? (
                    <CircularProgress size={20} color="inherit" />
                ) : (
                    <FileOpen
                        sx={{
                            fontSize: 20,
                            color: isInteractionDisabled
                                ? 'text.disabled'
                                : asset != null ? 'primary.main' : 'action.active',
                        }}
                    />
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
                mode={onlyPublic ? 'public' : 'all'}
                mimetype={mimetype}
            />
        </>
    );
}
