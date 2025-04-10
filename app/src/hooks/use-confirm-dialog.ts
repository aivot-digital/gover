import {ReactNode, useCallback, useState} from 'react';

export interface ConfirmDialogOptions<T> {
    title: string;
    state: T;
    onRender: (state: T, updateState: (update: Partial<T>) => void) => ReactNode;
    onConfirm: (state: T) => void;
    onCancel: () => void;
}

export function useConfirmDialog<T>() {
    const [confirmOptions, setConfirmOptions] = useState<ConfirmDialogOptions<T>>();

    const showConfirmDialog = useCallback((options: ConfirmDialogOptions<T>) => {
        setConfirmOptions(options);
    }, [setConfirmOptions]);

    const hideConfirmDialog = useCallback(() => {
        setConfirmOptions(undefined);
    }, [setConfirmOptions]);

    return {
        confirmOptions,
        showConfirmDialog,
        hideConfirmDialog,
    };
}