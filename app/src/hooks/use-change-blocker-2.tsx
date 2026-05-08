import {useCallback, useEffect, useMemo, useState} from 'react';
import {Blocker, useBeforeUnload, useBlocker} from 'react-router-dom';
import {deepEquals, shallowEquals} from '../utils/equality-utils';
import {ConfirmDialog} from '../dialogs/confirm-dialog/confirm-dialog';

interface ChangeBlockerProps<T> {
    original: T;
    edited: T;
    customTitle?: string;
    customMessage?: string;
    customConfirmButtonText?: string;
    useDeepEquals?: boolean;
    isActive?: boolean;
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
        if (!hasChanged) {
            return;
        }

        event.preventDefault();
        event.returnValue = '';
    }, [hasChanged]));

    const [pendingBlocker, setPendingBlocker] = useState<Blocker | null>(null);

    const [showDialog, setShowDialog] = useState(false);

    const blocker = useBlocker(({currentLocation, nextLocation}) => {
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

    useEffect(() => {
        if (blocker.state === 'blocked') {
            setShowDialog(true);
            setPendingBlocker(blocker);
        }
    }, [blocker]);

    const handleConfirm = useCallback(() => {
        if (pendingBlocker && pendingBlocker.proceed) {
            pendingBlocker.proceed();
        }
        setShowDialog(false);
        setPendingBlocker(null);
    }, [pendingBlocker]);

    const handleCancel = useCallback(() => {
        if (pendingBlocker != null && pendingBlocker.reset != null) {
            pendingBlocker.reset();
        }

        setShowDialog(false);
        setPendingBlocker(null);
    }, [pendingBlocker]);

    const dialog = useMemo(() => {
        if (!showDialog) {
            return null;
        }

        return (
            <ConfirmDialog
                title={customTitle}
                onConfirm={handleConfirm}
                onCancel={handleCancel}
                confirmButtonText={customConfirmButtonText}
                confirmButtonColor="error"
            >
                {customMessage}
            </ConfirmDialog>
        );
    }, [showDialog, customTitle, customMessage, handleConfirm, handleCancel]);

    return {
        hasChanged,
        dialog,
    };
}
