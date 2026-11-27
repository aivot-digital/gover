import {Box, useTheme} from '@mui/material';
import {alpha} from '@mui/material/styles';
import React from 'react';
import {FadingPaperProps} from './fading-paper-props';

export function FadingPaper(props: FadingPaperProps) {
    const theme = useTheme();

    return (
        <Box
            sx={{
                position: 'relative',
                overflow: 'hidden',
                p: 3,
                mt: 3,
                background: (theme) => `linear-gradient(
                    270deg,
                    ${alpha(theme.palette.text.primary, 0.02)} 7.88%,
                    ${alpha(theme.palette.text.primary, 0.06)} 100%
                )`,
                [theme.breakpoints.up('md')]: {
                    p: 6,
                    mt: 5,
                },
            }}
        >
            {props.children}
        </Box>
    );
}
