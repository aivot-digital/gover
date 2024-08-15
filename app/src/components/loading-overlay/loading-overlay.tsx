import {Backdrop, Container, Paper} from '@mui/material';
import React from 'react';
import {Loader} from "../loader/loader";

export function LoadingOverlay({message, isLoading}: { message?: string, isLoading?: boolean }) {
    return (
        <Backdrop
            open={isLoading ? isLoading : false}
            sx={{zIndex: (theme) => theme.zIndex.modal + 1}}
        >
            <Container
                maxWidth="sm"
            >
                <Paper sx={{p: 4}}>
                    <Loader message={message} />
                </Paper>
            </Container>
        </Backdrop>
    );
}