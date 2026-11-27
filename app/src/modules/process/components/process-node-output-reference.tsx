import {Box, Typography} from '@mui/material';
import {CopyToClipboardButton} from '../../../components/copy-to-clipboard-button/copy-to-clipboard-button';

interface ProcessNodeOutputReferenceProps {
    value: string;
    kind?: 'key' | 'path';
}

export function ProcessNodeOutputReference({value, kind = 'key'}: ProcessNodeOutputReferenceProps) {
    const label = kind === 'path' ? 'Pfad in den Elementdaten' : 'Schlüssel';

    return (
        <Box sx={{display: 'grid', gridTemplateColumns: 'minmax(0, 1fr) auto', alignItems: 'center', gap: 0.5}}>
            <Typography component="div" variant="caption" color="text.secondary" sx={{minWidth: 0, textAlign: 'left'}}>
                <Box component="span" sx={{mr: 0.5}}>{label}:</Box>
                <Box component="code" sx={{fontFamily: 'monospace', fontSize: '0.8125rem', color: 'text.primary', overflowWrap: 'anywhere'}}>
                    {value}
                </Box>
            </Typography>
            <CopyToClipboardButton
                text={value}
                tooltip={`${label} kopieren`}
                copiedTooltip={`${label} kopiert`}
                ariaLabel={`${label} ${value} kopieren`}
                size="small"
            />
        </Box>
    );
}
