import {LinearProgress, Backdrop, Paper, Typography, Box} from '@mui/material';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectLoadingMessage} from '../../../slices/shell-slice';
import {Loader} from '../../../components/loader/loader';
import React from 'react';
import {LoadingOverlay} from '../../../components/loading-overlay/loading-overlay';

export function ShellProgress() {
    const loadingMessage = useAppSelector(selectLoadingMessage);

    if (loadingMessage == null) {
        return null;
    }

    if (loadingMessage.blocking) {
        return (
            <LoadingOverlay
                isLoading={true}
                message={loadingMessage.message}
            />
        );
    }

    return (
        <LinearProgress
            sx={{
                position: 'sticky',
                top: 0,
                left: 0,
                width: '100%',
                height: '0.25rem',
                zIndex: (theme) => theme.zIndex.drawer + 2,
            }}
            color="secondary"
        />
    );
}