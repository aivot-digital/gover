import {useState, useEffect, useCallback, useMemo} from 'react';
import { useBlocker, Blocker } from 'react-router-dom';
import {deepEquals, shallowEquals} from '../utils/equality-utils';
import {ConfirmDialog} from "../dialogs/confirm-dialog/confirm-dialog";

export function useChangeBlocker(
    original: any,
    edited: any,
    customTitle?: string,
    customMessage?: string,
    useDeepEquals: boolean = true
) {
    const hasChanged = useMemo(() => {
        if (useDeepEquals) {
            return !deepEquals(original, edited);
        }
        return shallowEquals(original, edited);
    }, [original, edited, useDeepEquals]);

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
    }, [pendingBlocker]);

    const handleCancel = useCallback(() => {
        setShowDialog(false);
    }, []);

    return {
        hasChanged,
        dialog: showDialog ? (
            <ConfirmDialog
                title={customTitle || "Ungespeicherte Änderungen"}
                onConfirm={handleConfirm}
                onCancel={handleCancel}
            >
                {customMessage ||
                    "Sie haben ungespeicherte Änderungen. Möchten Sie die Seite wirklich verlassen? Dabei gehen alle ungespeicherten Änderungen verloren."}
            </ConfirmDialog>
        ) : null
    };
}
