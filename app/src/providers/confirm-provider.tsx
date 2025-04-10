import React, { createContext, useState, useContext, ReactNode } from "react";
import { ConfirmDialog } from "../dialogs/confirm-dialog/confirm-dialog";

interface ConfirmDialogOptions {
    title: string;
    confirmationText?: string;
    inputLabel?: string;
    inputPlaceholder?: string;
    isDestructive?: boolean;
    confirmButtonText?: string;
    children?: React.ReactNode;
}

interface ConfirmContextProps {
    showConfirm: (options: ConfirmDialogOptions) => Promise<boolean>;
}

const ConfirmContext = createContext<ConfirmContextProps | null>(null);

export const useConfirm = () => {
    const context = useContext(ConfirmContext);
    if (!context) {
        throw new Error("useConfirm must be used within a ConfirmProvider");
    }
    return context.showConfirm;
};

export const ConfirmProvider = ({ children }: { children: ReactNode }) => {
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
        <ConfirmContext.Provider value={{ showConfirm }}>
            {children}
            {dialogOptions && (
                <ConfirmDialog
                    {...dialogOptions}
                    onConfirm={handleConfirm}
                    onCancel={handleCancel}
                >
                    {dialogOptions.children}
                </ConfirmDialog>
            )}
        </ConfirmContext.Provider>
    );
};
