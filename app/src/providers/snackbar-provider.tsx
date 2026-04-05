import React, {useEffect, useRef} from 'react';
import {closeSnackbar, SnackbarKey, SnackbarProvider as NotistackProvider, useSnackbar} from 'notistack';
import {useAppSelector} from '../hooks/use-app-selector';
import {useAppDispatch} from '../hooks/use-app-dispatch';
import {removeSnackbar} from '../slices/snackbar-slice';
import {Alert, CircularProgress, Fade, GlobalStyles} from '@mui/material';
import {useTheme} from '@mui/material/styles';
import {selectMinimizeDrawer, selectSnackbarMessages, SnackbarSeverity, SnackbarType} from '../slices/shell-slice';

const FLOW_EDITOR_SNACKBAR_EXTRA_LEFT_OFFSET = 52;

// Default auto-hide durations for different severities
const SNACKBAR_AUTO_HIDE_DURATION_MS: Record<SnackbarSeverity, number> = {
    [SnackbarSeverity.Error]: 7000,
    [SnackbarSeverity.Warning]: 6000,
    [SnackbarSeverity.Info]: 5000,
    [SnackbarSeverity.Success]: 4000,
};

const SnackbarConsumer = () => {
    const theme = useTheme();
    const dispatch = useAppDispatch();

    // list of all active snackbar messages from the store
    const snackbarMessages = useAppSelector(selectSnackbarMessages);

    const {
        enqueueSnackbar,
    } = useSnackbar();

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
        snackbarMessages.forEach(({key, message, severity, type}) => {
            if (displayedMessages.current.has(key)) {
                return;
            }

            // Store the key of the displayed message
            displayedMessages.current.add(key);

            // Determine the auto-hide duration based on the given duration or the default duration for the given severity
            const autoHideDuration = SNACKBAR_AUTO_HIDE_DURATION_MS[severity];

            // replace hyphens with non-breaking hyphens, because Chrome does not respect word-break: keep-all
            const formattedMessage = message.replace(/-/g, '\u2011');

            enqueueSnackbar(message, {
                key,
                variant: severity,
                autoHideDuration: type === SnackbarType.AutoHiding ? autoHideDuration : null,
                persist: type !== SnackbarType.AutoHiding,
                preventDuplicate: true,
                content: (key) => {
                    return (
                        <Alert
                            onClose={
                                (
                                    type === SnackbarType.Loading ||
                                    type == SnackbarType.Permanent
                                ) ?
                                    undefined
                                    : () => {
                                        closeSnackbar(key);
                                        dispatch(removeSnackbar(key as string));
                                        displayedMessages.current.delete(key);
                                    }
                            }
                            severity={severity}
                            icon={type === SnackbarType.Loading ? <CircularProgress size={22} /> : undefined}
                            sx={{
                                width: '100%',
                                maxWidth: 460,
                                backgroundColor: 'white',
                                fontSize: '0.9375rem',
                                borderRadius: '6px',
                                padding: '6px 14px',
                                position: 'relative',
                                wordBreak: 'keep-all',
                                hyphens: 'manual',
                                boxShadow: 'rgba(2, 6, 12, 0.31) 0px 0px 1px 0px, rgba(2, 6, 12, 0.25) 0px 6px 12px -4px',
                                borderLeft: type !== SnackbarType.Loading ? `4px solid ${
                                    severity === 'error' ? theme.palette.error.light :
                                        severity === 'warning' ? theme.palette.warning.light :
                                            severity === 'info' ? theme.palette.info.light :
                                                severity === 'success' ? theme.palette.success.light :
                                                    theme.palette.grey[400]
                                }` : `4px solid ${theme.palette.grey[400]}`,
                            }}
                        >
                            {formattedMessage}
                        </Alert>
                    );
                },
            });

            // remove Snackbar after autoHideDuration
            if (type === SnackbarType.AutoHiding) {
                setTimeout(() => {
                    dispatch(removeSnackbar(key));
                    displayedMessages.current.delete(key);
                }, autoHideDuration);
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

export const SnackbarProvider = ({children}: { children: React.ReactNode }) => {
    const isDrawerMinimized = useAppSelector(selectMinimizeDrawer);
    const snackbarLeftOffset = isDrawerMinimized ? 88 : 280;
    const flowEditorSnackbarLeftOffset = snackbarLeftOffset + FLOW_EDITOR_SNACKBAR_EXTRA_LEFT_OFFSET;

    return (
        <>
            <NotistackProvider
                maxSnack={5}
                anchorOrigin={{
                    vertical: 'bottom',
                    horizontal: 'left',
                }}
                TransitionComponent={Fade}
            >
                <SnackbarConsumer />
                {children}
            </NotistackProvider>

            <GlobalStyles
                styles={{
                    '.notistack-SnackbarContainer': {
                        left: `${snackbarLeftOffset}px`,
                    },
                    'body[data-has-flow-editor="true"] .notistack-SnackbarContainer': {
                        left: `${flowEditorSnackbarLeftOffset}px`,
                    },
                }}
            />
        </>
    );
};
