import {useCallback, useEffect, useMemo, useState} from 'react';
import {Blocker, useBlocker} from 'react-router-dom';
import {deepEquals, shallowEquals} from '../utils/equality-utils';
import {ConfirmDialog} from '../dialogs/confirm-dialog/confirm-dialog';

interface ChangeBlockerProps<T> {
    original: T;
    edited: T;
    customTitle?: string;
    customMessage?: string;
    useDeepEquals?: boolean;
    isActive?: boolean;
}

const DEFAULT_TITLE = 'Ungespeicherte Änderungen';
const DEFAULT_MESSAGE = 'Sie haben ungespeicherte Änderungen. Möchten Sie die Seite wirklich verlassen? Dabei gehen alle ungespeicherten Änderungen verloren.';

export function useChangeBlocker<T>(props: ChangeBlockerProps<T>) {
    const {
        original,
        edited,
        customTitle = DEFAULT_TITLE,
        customMessage = DEFAULT_MESSAGE,
        useDeepEquals = true,
        isActive = true,
    } = props;

    const hasChanged = useMemo(() => {
        if (!isActive) {
            return false;
        }

        if (useDeepEquals) {
            return !deepEquals(original, edited);
        }

        return shallowEquals(original, edited);
    }, [original, edited, useDeepEquals, isActive]);

    const [pendingBlocker, setPendingBlocker] = useState<Blocker | null>(null);

    const [showDialog, setShowDialog] = useState(false);

    const blocker = useBlocker(({currentLocation, nextLocation}) => {
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
