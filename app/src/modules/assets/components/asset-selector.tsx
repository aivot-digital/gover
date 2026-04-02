import {SelectAssetDialog} from '../../../dialogs/select-asset-dialog/select-asset-dialog';
import {useEffect, useMemo, useState} from 'react';
import {Box, Button, CircularProgress, Fab, Stack, Typography} from '@mui/material';
import Edit from '@aivot/mui-material-symbols-400-outlined/dist/edit/Edit';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import {AssetsApiService} from '../assets-api-service';
import {Actions} from '../../../components/actions/actions';
import {VStorageIndexItemWithAssetEntity} from '../../storage/entities/storage-index-item-entity';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar} from '../../../slices/snackbar-slice';
import {LoadingPlaceholder} from '../../../components/loading-placeholder/loading-placeholder';

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

export function AssetSelector(props: AssetSelectorProps) {
    const {
        label,
        hint,
        selectLabel,
        value,
        onChange,
        disabled,
        required,
        error,
        mimetype,
        onlyPublic,
        placeholder,
    } = props;

    const dispatch = useAppDispatch();
    const [showSelectAssetDialog, setShowSelectAssetDialog] = useState(false);
    const [asset, setAsset] = useState<VStorageIndexItemWithAssetEntity>();

    useEffect(() => {
        setAsset(undefined);

        if (value == null) {
            return;
        }

        new AssetsApiService()
            .retrieveByKey(value)
            .then((res) => {
                setAsset(res);
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Asset konnte nicht geladen werden'));
            });
    }, [value]);

    return (
        <Box sx={{width: '100%'}}>
            <Typography>
                {label}
                {required ? ' *' : ''}
            </Typography>

            <Stack
                direction="row"
                marginTop={1}
            >
                {
                    value != null &&
                    asset == null &&
                    <Stack
                        direction="row"
                    >
                        <CircularProgress size={24}/>
                        <Typography
                            marginLeft={1}
                            color="textSecondary"
                        >
                            Lade {label}
                        </Typography>
                    </Stack>
                }

                {
                    value == null &&
                    asset == null &&
                    <Typography
                        color="textSecondary"
                    >
                        {placeholder ?? 'Keine Datei ausgewählt'}
                    </Typography>
                }

                {
                    value != null &&
                    asset != null &&
                    asset.pathFromRoot
                }

                <Actions
                    size="small"
                    dense={true}
                    sx={{
                        ml: 'auto',
                    }}
                    actions={[
                        {
                            icon: <Edit/>,
                            tooltip: `${label} auswählen`,
                            onClick: () => {
                                setShowSelectAssetDialog(true);
                            },
                            disabled: disabled,
                        },
                        {
                            icon: <Delete/>,
                            tooltip: `${label} entfernen`,
                            onClick: () => {
                                onChange(null);
                            },
                            disabled: disabled || value == null,
                        },
                    ]}
                />
            </Stack>

            {
                error != null &&
                <Typography
                    variant="caption"
                    color="error"
                    sx={{display: 'block', mt: 0.75}}
                >
                    {error}
                </Typography>
            }

            {
                hint != null &&
                <Typography
                    variant="caption"
                    color={error != null ? 'text.secondary' : undefined}
                    sx={{display: 'block', mt: error != null ? 0.25 : 0}}
                >
                    {hint}
                </Typography>
            }

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
        </Box>
    );
}
