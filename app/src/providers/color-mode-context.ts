import {createContext, useContext} from 'react';
import type {PaletteMode} from '@mui/material';

export type ColorModePreference = PaletteMode | 'system';

export interface ColorModeContextValue {
    mode: PaletteMode;
    preference: ColorModePreference;
    setPreference: (preference: ColorModePreference) => void;
}

export const ColorModeContext = createContext<ColorModeContextValue | undefined>(undefined);

export function useColorMode(): ColorModeContextValue {
    const context = useContext(ColorModeContext);
    if (context == null) {
        throw new Error('useColorMode must be used within AppProvider.');
    }
    return context;
}
