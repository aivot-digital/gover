import React, {createContext, ReactNode, useContext, useState} from 'react';
import {ConfirmDialog} from '../dialogs/confirm-dialog/confirm-dialog';
import {Theme, ThemeProvider, useTheme} from '@mui/material';

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
}

interface ConfirmContextProps {
    showConfirm: (options: ConfirmDialogOptions) => Promise<boolean>;
}

const ConfirmContext = createContext<ConfirmContextProps | null>(null);

export const useConfirm = () => {
    const context = useContext(ConfirmContext);
    if (!context) {
        throw new Error('useConfirm must be used within a ConfirmProvider');
    }
    return context.showConfirm;
};

export const ConfirmProvider = ({children}: { children: ReactNode }) => {
    const baseTheme = useTheme();
    const [dialogOptions, setDialogOptions] = useState<ConfirmDialogOptions | null>(null);
    const [resolveFn, setResolveFn] = useState<(value: boolean) => void>();

    const showConfirm = (options: ConfirmDialogOptions) => {
        return new Promise<boolean>((resolve) => {
            setDialogOptions({
                ...options,
            });
            setResolveFn(() => resolve);
        });
    };

    const handleConfirm = () => {
        if (resolveFn) resolveFn(true);
        setDialogOptions(null);
    };

    const handleCancel = () => {
        if (resolveFn) resolveFn(false);
        setDialogOptions(null);
    };

    return (
        <ConfirmContext.Provider value={{showConfirm}}>
            {children}
            {dialogOptions && (
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
