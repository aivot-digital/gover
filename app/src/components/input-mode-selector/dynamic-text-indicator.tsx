import {Box, Tooltip} from '@mui/material';
import Function from '@aivot/mui-material-symbols-400-n25-outlined/Function';

export const DynamicTextIndicatorLabel = 'Dieser dynamische Text unterstützt Variablen und Bedingungen.';

export function DynamicTextIndicator(props: {decorative?: boolean}) {
    return (
        <Tooltip title={DynamicTextIndicatorLabel} arrow>
            <Box
                component="span"
                role={props.decorative ? undefined : 'img'}
                aria-label={props.decorative ? undefined : DynamicTextIndicatorLabel}
                aria-hidden={props.decorative || undefined}
                sx={{
                    display: 'inline-flex',
                    width: 20,
                    height: 20,
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: 'text.disabled',
                }}
            >
                <Function sx={{fontSize: 16}}/>
            </Box>
        </Tooltip>
    );
}
