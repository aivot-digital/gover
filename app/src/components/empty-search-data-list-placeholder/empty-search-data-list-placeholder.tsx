import {Box, Typography} from '@mui/material';
import React from 'react';
import {EmptySearchDataListPlaceholderProps} from './empty-search-data-list-placeholder-props';

export function EmptySearchDataListPlaceholder(props: EmptySearchDataListPlaceholderProps) {
    return (
        <Box sx={{textAlign: 'center'}}>
            <Typography>
                {props.helperText}
            </Typography>
        </Box>
    );
}
