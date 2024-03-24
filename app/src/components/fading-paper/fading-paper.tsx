import {Box, useTheme} from '@mui/material';
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
                background: 'linear-gradient(270deg, #FCFCFC 7.88%, #EEF2EE 100%, #EEF2EE 100%)',
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
