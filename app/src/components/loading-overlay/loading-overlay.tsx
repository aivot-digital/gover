import {Backdrop, Container, Paper} from '@mui/material';
import React from 'react';
import {Loader} from '../loader/loader';

export function LoadingOverlay({message, isLoading, value}: { message?: string, isLoading?: boolean, value?: number }) {
    return (
        <Backdrop
            open={isLoading ? isLoading : false}
            sx={{zIndex: (theme) => theme.zIndex.modal + 1}}
        >
            <Container
                maxWidth="sm"
            >
                <Paper sx={{p: 4}}>
                    <Loader
                        message={message}
                        value={value}
                    />
                </Paper>
            </Container>
        </Backdrop>
    );
}