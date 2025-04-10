import React, { ReactNode } from "react";
import { ThemeProvider, type Theme as MuiTheme } from "@mui/material";
import { SnackbarProvider } from "./snackbar-provider";
import { PromptProvider } from "./prompt-provider";
import {ConfirmProvider} from "./confirm-provider";

interface AppProviderProps {
    children: ReactNode;
    theme: (baseTheme: MuiTheme) => MuiTheme;
}

export function AppProvider({ children, theme }: AppProviderProps) {
    return (
        <ThemeProvider theme={theme}>
            <SnackbarProvider>
                <PromptProvider>
                    <ConfirmProvider>
                        {children}
                    </ConfirmProvider>
                </PromptProvider>
            </SnackbarProvider>
        </ThemeProvider>
    );
}
