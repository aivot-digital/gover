import {Box} from '@mui/material';
import React from 'react';
import {FadingPaperProps} from './fading-paper-props';

export function FadingPaper(props: FadingPaperProps) {
    return (
        <Box
            sx={{
                position: 'relative',
                overflow: 'hidden',
                p: 6,
                mt: 5,
                background: 'linear-gradient(270deg, #FCFCFC 7.88%, #EEF2EE 100%, #EEF2EE 100%)',
            }}
        >
            {props.children}
        </Box>
    );
}
