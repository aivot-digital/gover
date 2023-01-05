import React from 'react';
import {Box, Paper, Typography} from '@mui/material';
import {selectCustomerInput} from '../../slices/customer-input-slice';
import {selectShowUserInput} from '../../slices/admin-settings-slice';
import {useAppSelector} from '../../hooks/use-app-selector';

export function UserInputDebugger() {
    const showUserInput = useAppSelector(selectShowUserInput);
    const userInput = useAppSelector(selectCustomerInput);

    if (!showUserInput) {
        return null;
    }

    return (
        <Box
            component={Paper}
            sx={{
                position: 'absolute',
                right: 0,
                bottom: 0,
                p: 4,
                width: '100%',
                height: '25vh',
                border: '1px solid red',
                overflowY: 'auto',
            }}
        >
            <Typography variant="overline">
                Nutzereingaben
            </Typography>
            <Box component="code">
                <Box component="pre">{
                    JSON.stringify(userInput, null, 4)
                }</Box>
            </Box>
        </Box>
    );
}
