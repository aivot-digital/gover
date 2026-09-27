import React, {PropsWithChildren, useCallback, useMemo, useState} from 'react';
import {CssBaseline, type PaletteMode, Theme, ThemeProvider, useMediaQuery} from '@mui/material';
import {SnackbarProvider} from './snackbar-provider';
import {PromptProvider} from './prompt-provider';
import {ConfirmProvider} from './confirm-provider';
import {Provider as TextBalanceProvider} from 'react-wrap-balancer';
import {BaseTheme} from '../theming/base-theme';
import {createAppTheme, createDefaultAppTheme} from '../theming/themes';
import {StorageKey} from '../data/storage-key';
import {StorageScope, StorageService} from '../services/storage-service';
import {ColorModeContext, type ColorModePreference} from './color-mode-context';

export function AppProvider({children, theme: __theme}: PropsWithChildren<{ theme?: Theme }>) {
    const systemPrefersDarkMode = useMediaQuery('(prefers-color-scheme: dark)');
    const [preference, setPreferenceState] = useState<ColorModePreference>(() => {
        const storedMode = StorageService.loadString(StorageKey.ColorMode);
        return storedMode === 'light' || storedMode === 'dark' ? storedMode : 'system';
    });
    const mode: PaletteMode = preference === 'system'
        ? (systemPrefersDarkMode ? 'dark' : 'light')
        : preference;
    const setPreference = useCallback((nextPreference: ColorModePreference) => {
        if (nextPreference === 'system') {
            StorageService.clearItem(StorageKey.ColorMode);
        } else {
            StorageService.storeString(StorageKey.ColorMode, nextPreference, StorageScope.Local);
        }
        setPreferenceState(nextPreference);
    }, []);

    const theme = useMemo(() => {
        const baseTheme = __theme ?? BaseTheme;
        if (AppConfig.systemTheme == null) {
            return createDefaultAppTheme(baseTheme, mode);
        }
        return createAppTheme(AppConfig.systemTheme, baseTheme, mode);
    }, [__theme, mode]);
    const colorModeContext = useMemo(
        () => ({mode, preference, setPreference}),
        [mode, preference, setPreference],
    );

    return (
        <ColorModeContext.Provider value={colorModeContext}>
            <ThemeProvider theme={theme}>
                <CssBaseline enableColorScheme/>
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
        </ColorModeContext.Provider>
    );
}
