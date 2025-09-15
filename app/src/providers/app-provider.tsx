import React, {PropsWithChildren, useMemo} from 'react';
import {CssBaseline, Theme, ThemeProvider} from '@mui/material';
import {SnackbarProvider} from './snackbar-provider';
import {PromptProvider} from './prompt-provider';
import {ConfirmProvider} from './confirm-provider';
import {Provider as TextBalanceProvider} from 'react-wrap-balancer';
import {useAppSelector} from '../hooks/use-app-selector';
//import {selectTheme} from '../slices/shell-slice';
import {BaseTheme} from '../theming/base-theme';
import {createAppTheme} from '../theming/themes';

export function AppProvider({children, theme: __theme}: PropsWithChildren<{theme?: Theme}>) {
    const themeObject = null;// useAppSelector(selectTheme);

    const theme = useMemo(() => {
        if (themeObject == null) {
            return BaseTheme;
        }
        return BaseTheme; //createAppTheme(themeObject, BaseTheme);
    }, [themeObject]);

    return (
        <ThemeProvider theme={theme}>
            <CssBaseline />
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
