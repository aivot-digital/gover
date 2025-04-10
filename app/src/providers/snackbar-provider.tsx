import React, {useEffect, useRef} from 'react';
import {useSnackbar, SnackbarProvider as NotistackProvider, closeSnackbar, SnackbarKey} from 'notistack';
import { useAppSelector} from '../hooks/use-app-selector';
import { useAppDispatch} from '../hooks/use-app-dispatch';
import {removeSnackbar} from '../slices/snackbar-slice';
import {Alert, CircularProgress, Fade} from '@mui/material';
import {useTheme} from '@mui/material/styles';

const SnackbarConsumer = () => {
    const dispatch = useAppDispatch();
    const { enqueueSnackbar } = useSnackbar();
    const snackbarMessages = useAppSelector(state => state.snackbar.messages);
    const theme = useTheme();

    // prevents duplicate Snackbars
    const displayedMessages = useRef<Set<SnackbarKey>>(new Set());

    // handler for Escape key, to close oldest Snackbar
    useEffect(() => {
        const handleEscape = (event: KeyboardEvent) => {
            if (event.key === 'Escape' && snackbarMessages.length > 0) {
                event.preventDefault();
                const oldestSnackbar = snackbarMessages[snackbarMessages.length - 1];
                closeSnackbar(oldestSnackbar.key);
                dispatch(removeSnackbar(oldestSnackbar.key));
                displayedMessages.current.delete(oldestSnackbar.key); // also remove from set
            }
        };

        document.addEventListener('keydown', handleEscape);
        return () => document.removeEventListener('keydown', handleEscape);
    }, [snackbarMessages, dispatch]);

    useEffect(() => {
        snackbarMessages.forEach(({ key, message, severity }, index) => {
            if (!displayedMessages.current.has(key)) {
                displayedMessages.current.add(key);

                const autoHideDuration = severity === 'error' ? 7000 :
                    severity === 'warning' ? 6000 :
                        severity === 'info' ? 5000 :
                            4000; // standard for 'success'

                // replace hyphens with non-breaking hyphens, because Chrome does not respect word-break: keep-all
                const formattedMessage = message.replace(/-/g, '\u2011');

                const isLoading = severity === 'info' && key === 'loading-toast';

                enqueueSnackbar(message, {
                    key,
                    variant: severity,
                    autoHideDuration: isLoading ? null : autoHideDuration,
                    persist: isLoading,
                    preventDuplicate: true,
                    content: (key) => {
                        const isLoading = severity === 'info' && key === 'loading-toast';

                        return (<Alert
                            onClose={
                                isLoading
                                    ? undefined
                                    : () => {
                                        closeSnackbar(key);
                                        dispatch(removeSnackbar(key as string));
                                        displayedMessages.current.delete(key);
                                    }
                            }
                            severity={severity}
                            icon={isLoading ? <CircularProgress size={22} /> : undefined}
                            sx={{
                                width: '100%',
                                maxWidth: 460,
                                backgroundColor: "white",
                                fontSize: '0.9375rem',
                                borderRadius: '6px',
                                padding: '6px 14px',
                                position: 'relative',
                                wordBreak: 'keep-all',
                                hyphens: 'manual',
                                boxShadow: 'rgba(2, 6, 12, 0.31) 0px 0px 1px 0px, rgba(2, 6, 12, 0.25) 0px 6px 12px -4px',
                                borderLeft: !isLoading ? `4px solid ${
                                    severity === 'error' ? theme.palette.error.light :
                                        severity === 'warning' ? theme.palette.warning.light :
                                            severity === 'info' ? theme.palette.info.light :
                                                severity === 'success' ? theme.palette.success.light :
                                                    theme.palette.grey[400]
                                }` : `4px solid ${theme.palette.grey[400]}`,
                            }}
                        >
                            {formattedMessage}
                        </Alert>);
                    },
                });

                // remove Snackbar after autoHideDuration
                if (key !== 'loading-toast') {
                    setTimeout(() => {
                        dispatch(removeSnackbar(key));
                        displayedMessages.current.delete(key);
                    }, autoHideDuration);
                }
            }
        });
    }, [snackbarMessages, enqueueSnackbar, dispatch, theme]);

    // Close any snackbars that were removed from the Redux store but are still shown.
    // This keeps the UI in sync with the state and ensures "manual" snackbars (e.g. loading) are properly dismissed.
    useEffect(() => {
        const displayedKeys = new Set(displayedMessages.current);
        const reduxKeys = new Set(snackbarMessages.map(msg => msg.key));

        const removedKeys: string[] = [];
        displayedKeys.forEach((key) => {
            if (!reduxKeys.has(key as string)) {
                removedKeys.push(key as string);
            }
        });

        removedKeys.forEach(key => {
            closeSnackbar(key);
            displayedMessages.current.delete(key);
        });
    }, [snackbarMessages]);

    return null;
};

export const SnackbarProvider = ({ children }: { children: React.ReactNode }) => {
    return (
        <NotistackProvider
            maxSnack={5}
            anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
            TransitionComponent={Fade}
        >
            <SnackbarConsumer />
            {children}
        </NotistackProvider>
    );
};
