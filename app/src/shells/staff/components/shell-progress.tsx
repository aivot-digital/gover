import {LinearProgress} from '@mui/material';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectLoadingMessage, ShellLoadingMessage} from '../../../slices/shell-slice';
import React, {useEffect, useRef, useState} from 'react';
import {LoadingOverlay} from '../../../components/loading-overlay/loading-overlay';

interface ShellProgressBuffer {
    message: ShellLoadingMessage;
    started: number;
}

const MAX_PROGRESS = 0.9;
const MAX_RUNTIME = 5000; // 5 seconds
const PROGRESS_UPDATE_INTERVAL = 100; // 0.1 seconds

export function ShellProgress() {
    const loadingMessage = useAppSelector(selectLoadingMessage);

    const [buffer, setBuffer] = useState<ShellProgressBuffer | null>(null);
    const hideTimeoutRef = useRef<NodeJS.Timeout>(null);
    useEffect(() => {
        if (loadingMessage != null) {
            // If a new loading message appears, reset the buffer
            setBuffer({
                message: loadingMessage,
                started: new Date().getTime(),
            });

            // Check if a hide timeout is in progress and clear it
            if (hideTimeoutRef.current != null) {
                clearTimeout(hideTimeoutRef.current);
            }
        } else {
            if (buffer != null) {
                hideTimeoutRef.current = setTimeout(() => {
                    setBuffer(null);
                }, 300);
            }
        }
    }, [loadingMessage]);

    const [progress, setProgress] = useState(0);
    const checkProgressIntervalRef = useRef<NodeJS.Timeout>(null);
    useEffect(() => {
        if (checkProgressIntervalRef.current != null) {
            clearInterval(checkProgressIntervalRef.current);
        }

        if (buffer != null) {
            checkProgressIntervalRef.current = setInterval(() => {
                const now = new Date().getTime();
                const passed = now - buffer.started;

                const estimatedTime = buffer.message.estimatedTime ?? MAX_RUNTIME;
                let newProgress = Math.min(MAX_PROGRESS, passed / estimatedTime);
                if (newProgress < progress) {
                    newProgress = progress;
                }
                setProgress(newProgress);
            }, PROGRESS_UPDATE_INTERVAL);
        } else {
            setProgress(0);
        }
    }, [buffer]);


    if (loadingMessage == null) {
        return null;
    }

    if (loadingMessage.blocking) {
        return (
            <LoadingOverlay
                isLoading={true}
                message={loadingMessage.message}
                progresValue={progress * 100}
                progresVariant={progress < MAX_PROGRESS ? 'determinate' : 'indeterminate'}
            />
        );
    }

    return (
        <>
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
                value={progress * 100}
                variant={progress < MAX_PROGRESS ? 'determinate' : 'indeterminate'}
            />
        </>
    );
}