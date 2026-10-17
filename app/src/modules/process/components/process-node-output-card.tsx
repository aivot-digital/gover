import React, {useId} from 'react';
import {Box, Button, type SxProps, type Theme, Typography} from '@mui/material';
import Info from '@aivot/mui-material-symbols-400-n25-outlined/Info';
import {ProcessNodeOutputReference} from './process-node-output-reference';

interface ProcessNodeOutputCardProps {
    label: string;
    outputKey: string;
    description: string;
    onShowTypeDefinition?: () => void;
    sx?: SxProps<Theme>;
}

export function ProcessNodeOutputCard(props: ProcessNodeOutputCardProps): React.ReactNode {
    const {
        label,
        outputKey,
        description,
        onShowTypeDefinition,
        sx,
    } = props;
    const titleId = useId();
    const descriptionId = useId();

    return (
        <Box
            component="section"
            aria-labelledby={titleId}
            aria-describedby={descriptionId}
            sx={[
                {
                    p: 1.5,
                    border: '1px solid',
                    borderColor: 'divider',
                    borderRadius: 1.5,
                },
                ...(sx == null ? [] : Array.isArray(sx) ? sx : [sx]),
            ]}
        >
            <Box sx={{display: 'flex', alignItems: 'center', gap: 1}}>
                <Typography id={titleId} variant="body1" sx={{fontWeight: 600, minWidth: 0, flex: 1, overflowWrap: 'anywhere'}}>
                    {label}
                </Typography>
                {onShowTypeDefinition != null && (
                    <Button
                        size="small"
                        variant="text"
                        startIcon={<Info/>}
                        onClick={onShowTypeDefinition}
                        aria-label={`Details zu ${label} anzeigen`}
                        aria-haspopup="dialog"
                        sx={{flexShrink: 0, mr: -0.75}}
                    >
                        Details anzeigen
                    </Button>
                )}
            </Box>
            <Typography id={descriptionId} variant="body2" color="text.secondary" sx={{mt: 0.5, fontSize: '0.875rem'}}>
                {description}
            </Typography>

            <Box sx={{mt: 1, px: 1, py: 0.5, bgcolor: 'action.hover', borderRadius: 1}}>
                <ProcessNodeOutputReference value={outputKey}/>
            </Box>
        </Box>
    );
}
