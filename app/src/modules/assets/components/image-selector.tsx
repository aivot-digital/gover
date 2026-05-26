import {SelectAssetDialog} from '../../../dialogs/select-asset-dialog/select-asset-dialog';
import {useEffect, useState} from 'react';
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
    required?: boolean;
    error?: string;
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
    } = props;

    const [showSelectAssetDialog, setShowSelectAssetDialog] = useState(false);
    const [imageStatus, setImageStatus] = useState<'idle' | 'loading' | 'loaded' | 'failed'>(
        value == null ? 'idle' : 'loading',
    );

    const link = value == null
        ? null
        : AssetsApiService.useAssetLink(value);

    useEffect(() => {
        setImageStatus(link == null ? 'idle' : 'loading');
    }, [link]);

    return (
        <Box sx={{width: '100%'}}>
            <Typography>
                {label}
                {required ? ' *' : ''}
            </Typography>

            <Box
                sx={{
                    position: 'relative',
                    width: 'width' in size ? size.width : '100%',
                    height: 'height' in size ? size.height : undefined,
                    aspectRatio: 'aspectRatio' in size ? size.aspectRatio : undefined,
                    border: (theme) => `1px solid ${error != null ? theme.palette.error.main : '#aaa'}`,
                    backgroundColor: '#f0f0f0',
                    borderRadius: '0.5rem',
                    overflow: 'hidden',
                }}
            >
                {
                    link != null &&
                    <img
                        key={link}
                        src={link}
                        alt=""
                        onLoad={() => {
                            setImageStatus('loaded');
                        }}
                        onError={() => {
                            setImageStatus('failed');
                        }}
                        style={{
                            position: 'absolute',
                            inset: 0,
                            width: '100%',
                            height: '100%',
                            objectFit: 'cover',
                            display: 'block',
                        }}
                    />
                }
                {
                    (link == null || imageStatus === 'failed') &&
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
                        {link == null ? 'Kein Bild ausgewählt' : 'Bild konnte nicht geladen werden'}
                    </Typography>
                }
                {
                    disabled !== true &&
                    <Stack
                        sx={{
                            position: 'absolute',
                            bottom: '0.5rem',
                            right: '0.5rem',
                            zIndex: 1,
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

            <Typography
                variant="caption"
                color={error != null ? 'text.secondary' : undefined}
                sx={{display: 'block', mt: error != null ? 0.25 : 0}}
            >
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
