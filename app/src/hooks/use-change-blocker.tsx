import { useState, useEffect, useCallback } from 'react';
import { useBlocker, Blocker } from 'react-router-dom';
import { shallowEquals } from '../utils/equality-utils';
import {ConfirmDialog} from "../dialogs/confirm-dialog/confirm-dialog";

export function useChangeBlocker(
    original: any,
    edited: any,
    customTitle?: string,
    customMessage?: string,
) {
    const hasChanged = !shallowEquals(original, edited);
    const [pendingBlocker, setPendingBlocker] = useState<Blocker | null>(null);
    const [showDialog, setShowDialog] = useState(false);

    const blocker = useBlocker(hasChanged);

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
