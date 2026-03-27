import {SelectAssetDialog} from '../../../dialogs/select-asset-dialog/select-asset-dialog';
import {useMemo, useState} from 'react';
import {Box, Fab, Stack, Typography} from '@mui/material';
import Edit from '@aivot/mui-material-symbols-400-outlined/dist/edit/Edit';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import {AssetsApiService} from '../assets-api-service';

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
    } = props;

    const [showSelectAssetDialog, setShowSelectAssetDialog] = useState(false);

    const link = useMemo(() => {
        if (value == null) {
            return null;
        }

        return AssetsApiService
            .useAssetLink(value);
    }, [value]);

    return (
        <Box>
            <Typography>
                {label}
            </Typography>

            <Box
                sx={{
                    position: 'relative',
                    width: 'width' in size ? size.width : '100%',
                    height: 'height' in size ? size.height : undefined,
                    aspectRatio: 'aspectRatio' in size ? size.aspectRatio : undefined,
                    border: '1px solid #aaa',
                    backgroundColor: '#f0f0f0',
                    backgroundImage: link != null ? `url(${link})` : undefined,
                    backgroundSize: 'cover',
                    backgroundPosition: 'center',
                    borderRadius: '0.5rem',
                }}
            >
                {
                    link == null &&
                    <Typography
                        variant="body2"
                        color="text.secondary"
                        sx={{
                            position: 'absolute',
                            inset: 0,
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            textAlign: 'center',
                            px: 2,
                        }}
                    >
                        Kein Bild ausgewählt
                    </Typography>
                }
                {
                    disabled !== true &&
                    <Stack
                        sx={{
                            position: 'absolute',
                            bottom: '0.5rem',
                            right: '0.5rem',
                        }}
                        direction="row"
                        spacing={1}
                    >
                        {
                            value != null &&
                            <Fab
                                color="inherit"
                                size="small"
                                aria-label={`${label} entfernen`}
                                onClick={() => {
                                    onChange(null);
                                }}
                            >
                                <Delete />
                            </Fab>
                        }
                        <Fab
                            color="inherit"
                            size="small"
                            aria-label={`${label} auswählen`}
                            onClick={() => {
                                setShowSelectAssetDialog(true);
                            }}
                        >
                            <Edit />
                        </Fab>
                    </Stack>
                }
            </Box>


            <Typography variant="caption">
                {hint}
            </Typography>

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
        </Box>
    );
}
