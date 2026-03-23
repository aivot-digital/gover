import {Backdrop, Container, Paper} from '@mui/material';
import React from 'react';
import {Loader} from '../loader/loader';

interface LoadingOverlayProps {
    message?: string;
    isLoading?: boolean;
    progresValue?: number;
    progresVariant?: 'determinate' | 'indeterminate';
}

export function LoadingOverlay(props: LoadingOverlayProps) {
    const {
        message,
        isLoading,
        progresValue,
        progresVariant,
    } = props;
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
                        progresValue={progresValue}
                        progresVariant={progresVariant}
                    />
                </Paper>
            </Container>
        </Backdrop>
    );
}