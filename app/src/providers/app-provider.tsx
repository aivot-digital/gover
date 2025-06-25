import React, { ReactNode } from "react";
import { ThemeProvider, type Theme as MuiTheme } from "@mui/material";
import { SnackbarProvider } from "./snackbar-provider";
import { PromptProvider } from "./prompt-provider";
import {ConfirmProvider} from "./confirm-provider";
import { Provider as TextBalanceProvider } from 'react-wrap-balancer'

interface AppProviderProps {
    children: ReactNode;
    theme: (baseTheme: MuiTheme) => MuiTheme;
}

export function AppProvider({ children, theme }: AppProviderProps) {
    return (
        <ThemeProvider theme={theme}>
            <TextBalanceProvider>
                <SnackbarProvider>
                    <PromptProvider>
                        <ConfirmProvider>
                            {children}
                        </ConfirmProvider>
                    </PromptProvider>
                </SnackbarProvider>
            </TextBalanceProvider>
        </ThemeProvider>
    );
}
