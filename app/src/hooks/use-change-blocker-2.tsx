import {useCallback, useLayoutEffect, useMemo, useRef, useState} from 'react';
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
    /** A reload or account change starts a new visit even when the route stays the same. */
    visitKey?: string | null;
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
        visitKey,
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

    const visit = useMemo(() => ({key: visitKey, active: false, pending: false}), [visitKey]);
    const previousVisit = useRef(visit);
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

    useLayoutEffect(() => {
        visit.active = true;
        return () => { visit.active = false; };
    }, [visit]);

    useLayoutEffect(() => {
        if (previousVisit.current === visit) return;
        previousVisit.current = visit;
        setConfirming(false);
        setConfirmationFailed(false);
        if (blocker.state === 'blocked') blocker.reset();
    }, [blocker, visit]);

    const handleConfirm = async () => {
        if (!visit.active || blocker.state !== 'blocked' || visit.pending) {
            return;
        }

        visit.pending = true;
        setConfirming(true);
        setConfirmationFailed(false);
        try {
            if (onConfirmNavigation != null) {
                await onConfirmNavigation();
            }
            if (visit.active) blocker.proceed();
        } catch (error) {
            if (visit.active) {
                console.error(error);
                setConfirmationFailed(true);
            }
        } finally {
            visit.pending = false;
            if (visit.active) setConfirming(false);
        }
    };

    const handleCancel = () => {
        if (visit.active && blocker.state === 'blocked' && !visit.pending) {
            setConfirmationFailed(false);
            blocker.reset();
        }
    };

    const dialog = blocker.state === 'blocked' && previousVisit.current === visit ? (
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
