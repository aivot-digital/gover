import {Box} from '@mui/material';
import React from 'react';
import {useAppSelector} from '../../hooks/use-app-selector';

export function DeveloperToolsTabVisibilities() {
    const visibilities = useAppSelector(state => state.app.visibilities);

    return (
        <Box component="code">
            <Box component="pre">{
                JSON.stringify(visibilities, null, 4)
            }</Box>
        </Box>
    );
}