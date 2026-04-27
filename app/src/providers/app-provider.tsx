import React, {PropsWithChildren, useMemo} from 'react';
import {CssBaseline, Theme, ThemeProvider} from '@mui/material';
import {SnackbarProvider} from './snackbar-provider';
import {PromptProvider} from './prompt-provider';
import {ConfirmProvider} from './confirm-provider';
import {Provider as TextBalanceProvider} from 'react-wrap-balancer';
import {BaseTheme} from '../theming/base-theme';
import {createAppTheme, createDefaultAppTheme} from '../theming/themes';

export function AppProvider({children, theme: __theme}: PropsWithChildren<{ theme?: Theme }>) {
    const theme = useMemo(() => {
        if (AppConfig.systemTheme == null) {
            return createDefaultAppTheme(BaseTheme);
        }
        return createAppTheme(AppConfig.systemTheme, BaseTheme);
    }, []);

    return (
        <ThemeProvider theme={theme}>
            <CssBaseline/>
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
