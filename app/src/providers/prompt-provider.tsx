import React, { createContext, useState, useContext, ReactNode } from "react";
import {PromptDialog, PromptDialogProps} from "../dialogs/prompt-dialog/prompt-dialog";

interface PromptContextProps {
    showPrompt: (options: Omit<PromptDialogProps, "onConfirm" | "onCancel">) => Promise<string | null>;
}

const PromptContext = createContext<PromptContextProps | null>(null);

export const usePrompt = () => {
    const context = useContext(PromptContext);
    if (!context) {
        throw new Error("usePrompt must be used within a PromptProvider");
    }
    return context.showPrompt;
};

export const PromptProvider = ({ children }: { children: ReactNode }) => {
    const [dialogOptions, setDialogOptions] = useState<PromptDialogProps | null>(null);
    const [resolveFn, setResolveFn] = useState<(value: string | null) => void>();

    const showPrompt = (options: Omit<PromptDialogProps, "onConfirm" | "onCancel">) => {
        return new Promise<string | null>((resolve) => {
            setDialogOptions({
                ...options,
                onConfirm: (value) => {
                    resolve(value);
                    setDialogOptions(null);
                },
                onCancel: () => {
                    resolve(null);
                    setDialogOptions(null);
                },
            });
            setResolveFn(() => resolve);
        });
    };

    return (
        <PromptContext.Provider value={{ showPrompt }}>
            {children}
            {dialogOptions && <PromptDialog {...dialogOptions} />}
        </PromptContext.Provider>
    );
};
