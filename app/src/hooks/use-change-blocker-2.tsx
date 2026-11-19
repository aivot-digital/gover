import {useCallback, useMemo, useRef, useState} from 'react';
import Alert from '@mui/material/Alert';
import {useBeforeUnload, useBlocker} from 'react-router-dom';
import {deepEquals, shallowEquals} from '../utils/equality-utils';
import {ConfirmDialog} from '../dialogs/confirm-dialog/confirm-dialog';
import {AuthService} from '../services/auth-service';

interface ChangeBlockerProps<T> {
    original: T;
    edited: T;
    customTitle?: string;
    customMessage?: string;
    customConfirmButtonText?: string;
    useDeepEquals?: boolean;
    isActive?: boolean;
    onConfirmNavigation?: () => void | Promise<void>;
    shouldAllowNavigation?: (navigation: {
        currentLocation: {
            pathname: string;
            search: string;
            state: unknown;
        };
        nextLocation: {
            pathname: string;
            search: string;
            state: unknown;
        };
    }) => boolean;
}

const DEFAULT_TITLE = 'Ungespeicherte Änderungen';
const DEFAULT_MESSAGE = 'Sie haben ungespeicherte Änderungen. Möchten Sie die Seite wirklich verlassen? Dabei gehen alle ungespeicherten Änderungen verloren.';
const DEFAULT_CONFIRM_BUTTON_TEXT = 'Änderungen verwerfen';

export function useChangeBlocker<T>(props: ChangeBlockerProps<T>) {
    const {
        original,
        edited,
        customTitle = DEFAULT_TITLE,
        customMessage = DEFAULT_MESSAGE,
        customConfirmButtonText = DEFAULT_CONFIRM_BUTTON_TEXT,
        useDeepEquals = true,
        isActive = true,
        onConfirmNavigation,
        shouldAllowNavigation,
    } = props;

    const hasChanged = useMemo(() => {
        if (!isActive) {
            return false;
        }

        if (useDeepEquals) {
            return !deepEquals(original, edited);
        }

        return !shallowEquals(original, edited);
    }, [original, edited, useDeepEquals, isActive]);

    useBeforeUnload(useCallback((event: BeforeUnloadEvent) => {
        if (!hasChanged || !AuthService.isAuthenticated()) {
            return;
        }

        event.preventDefault();
        event.returnValue = '';
    }, [hasChanged]));

    const confirmationPending = useRef(false);
    const [confirming, setConfirming] = useState(false);
    const [confirmationFailed, setConfirmationFailed] = useState(false);
    const blocker = useBlocker(({currentLocation, nextLocation}) => {
        if (!AuthService.isAuthenticated()) {
            return false;
        }

        if (shouldAllowNavigation?.({currentLocation, nextLocation}) === true) {
            return false;
        }

        // Check if only the hash is changing
        if (currentLocation.pathname === nextLocation.pathname &&
            currentLocation.search === nextLocation.search) {
            return false; // Allow navigation
        }
        return hasChanged;
    });

    const handleConfirm = async () => {
        if (blocker.state !== 'blocked' || confirmationPending.current) {
            return;
        }

        confirmationPending.current = true;
        setConfirming(true);
        setConfirmationFailed(false);
        try {
            if (onConfirmNavigation != null) {
                await onConfirmNavigation();
            }
            blocker.proceed();
        } catch (error) {
            console.error(error);
            setConfirmationFailed(true);
        } finally {
            confirmationPending.current = false;
            setConfirming(false);
        }
    };

    const handleCancel = () => {
        if (blocker.state === 'blocked' && !confirmationPending.current) {
            setConfirmationFailed(false);
            blocker.reset();
        }
    };

    const dialog = blocker.state === 'blocked' ? (
        <ConfirmDialog
            title={customTitle}
            onConfirm={handleConfirm}
            onCancel={handleCancel}
            busy={confirming}
            confirmButtonText={customConfirmButtonText}
            confirmButtonColor="error"
        >
            {customMessage}
            {confirmationFailed && (
                <Alert severity="error" sx={{mt: 2}}>
                    Die Änderungen konnten nicht verworfen werden. Versuchen Sie es erneut.
                </Alert>
            )}
        </ConfirmDialog>
    ) : null;

    return {
        hasChanged,
        dialog,
    };
}
