import React from 'react';
import {Box, type SxProps, type Theme, Typography} from '@mui/material';
import {CopyToClipboardButton} from '../../../components/copy-to-clipboard-button/copy-to-clipboard-button';

interface ProcessNodeOutputCardProps {
    label: string;
    outputKey: string;
    description: string;
    sx?: SxProps<Theme>;
}

export function ProcessNodeOutputCard(props: ProcessNodeOutputCardProps): React.ReactNode {
    const {
        label,
        outputKey,
        description,
        sx,
    } = props;

    return (
        <Box
            sx={{
                p: 1.5,
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: 1.5,
                ...sx,
            }}
        >
            <Box
                sx={{
                    display: 'grid',
                    gridTemplateColumns: 'minmax(0, 1fr) auto',
                    alignItems: 'start',
                    justifyContent: 'space-between',
                    gap: 1,
                }}
            >
                <Typography
                    variant="body2"
                    sx={{
                        fontWeight: 600,
                        minWidth: 0,
                        lineHeight: 1.4,
                    }}
                >
                    {label}
                </Typography>

                <Box
                    sx={{
                        display: 'flex',
                        alignItems: 'flex-start',
                        gap: 0.125,
                        color: 'text.secondary',
                        flexShrink: 0,
                        mt: '1px',
                    }}
                >
                    <Typography
                        variant="body2"
                        color="text.secondary"
                        sx={{
                            fontFamily: 'monospace',
                            whiteSpace: 'nowrap',
                            fontSize: '0.8125rem',
                            lineHeight: 1.4,
                        }}
                    >
                        {outputKey}
                    </Typography>
                    <CopyToClipboardButton
                        text={outputKey}
                        tooltip="Key kopieren"
                        copiedTooltip="Key kopiert!"
                        ariaLabel={`Ausgangsdaten-Key ${outputKey} kopieren`}
                        size="small"
                        sx={{
                            mt: -1,
                            ml: 0.125,
                        }}
                    />
                </Box>
            </Box>

            <Typography
                variant="body2"
                color="text.secondary"
                sx={{
                    mt: 0.75,
                }}
            >
                {description}
            </Typography>
        </Box>
    );
}
