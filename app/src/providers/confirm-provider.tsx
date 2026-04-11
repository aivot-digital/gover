import React, {createContext, type ReactNode, useCallback, useContext, useState} from 'react';
import {ConfirmDialog} from '../dialogs/confirm-dialog/confirm-dialog';
import {type Theme, ThemeProvider, useTheme} from '@mui/material';

interface ConfirmDialogOptions {
    title: string;
    confirmationText?: string;
    inputLabel?: string;
    inputPlaceholder?: string;
    isDestructive?: boolean;
    confirmButtonText?: string;
    hideCancelButton?: boolean;
    children?: React.ReactNode;
    theme?: Theme;
    width?: 'sm' | 'md' | 'lg' | 'xl';
}

interface ConfirmContextProps {
    showConfirm: (options: ConfirmDialogOptions) => Promise<boolean>;
}

const ConfirmContext = createContext<ConfirmContextProps | null>(null);

export const useConfirm = () => {
    const context = useContext(ConfirmContext);
    const theme = useTheme();

    if (context == null) {
        throw new Error('useConfirm must be used within a ConfirmProvider');
    }

    return useCallback((options: ConfirmDialogOptions) => {
        // Preserve the caller's theme because the provider renders the dialog at app level.
        return context.showConfirm({
            theme: theme,
            ...options,
        });
    }, [context, theme]);
};

export const ConfirmProvider = ({children}: { children: ReactNode }): ReactNode => {
    const baseTheme = useTheme();
    const [dialogOptions, setDialogOptions] = useState<ConfirmDialogOptions | null>(null);
    const [resolveFn, setResolveFn] = useState<(value: boolean) => void>();

    const showConfirm = (options: ConfirmDialogOptions): Promise<boolean> => {
        return new Promise<boolean>((resolve) => {
            setDialogOptions({
                ...options,
            });
            setResolveFn(() => resolve);
        });
    };

    const handleConfirm = (): void => {
        if (resolveFn != null) {
            resolveFn(true);
        }
        setDialogOptions(null);
    };

    const handleCancel = (): void => {
        if (resolveFn != null) {
            resolveFn(false);
        }
        setDialogOptions(null);
    };

    return (
        <ConfirmContext.Provider value={{showConfirm}}>
            {children}
            {(dialogOptions != null) && (
                <ThemeProvider theme={dialogOptions.theme ?? baseTheme}>
                    <ConfirmDialog
                        {...dialogOptions}
                        onConfirm={handleConfirm}
                        onCancel={handleCancel}
                    >
                        {dialogOptions.children}
                    </ConfirmDialog>
                </ThemeProvider>
            )}
        </ConfirmContext.Provider>
    );
};
