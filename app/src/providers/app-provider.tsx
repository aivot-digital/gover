import React, {ReactNode} from 'react';
import {type Theme as MuiTheme, ThemeProvider} from '@mui/material';
import {SnackbarProvider} from './snackbar-provider';
import {PromptProvider} from './prompt-provider';
import {ConfirmProvider} from './confirm-provider';
import {Provider as TextBalanceProvider} from 'react-wrap-balancer';

interface AppProviderProps {
    children: ReactNode;
    theme: MuiTheme;
}

export function AppProvider({children, theme}: AppProviderProps) {
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
