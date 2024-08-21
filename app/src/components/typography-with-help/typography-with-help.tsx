import {TypographyWithHelpProps} from './typography-with-help-props';
import {Box, Typography, TypographyProps} from '@mui/material';
import React from 'react';
import {Hint} from '../hint/hint';

export function TypographyWithHelp(props: TypographyWithHelpProps & TypographyProps) {
    const {hintDetailsTitle, hintDetails, hintSummary, children, sx, ...rest} = props;

    return (
        <Box
            display="flex"
            alignItems="center"
            sx={sx}
        >
            <Typography {...rest} >
                {children}
            </Typography>

            <Hint
                detailsTitle={hintDetailsTitle}
                details={hintDetails}
                summary={hintSummary}
            />
        </Box>
    );
}