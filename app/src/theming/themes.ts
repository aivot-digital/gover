import {createTheme, PaletteOptions, Theme} from '@mui/material';
import _CustomerThemes from '../custom-themes.json';
import {deDE as datePickerLocale} from "@mui/x-date-pickers";
import {deDE as coreLocale} from "@mui/material/locale";

const CustomerThemes = _CustomerThemes as {
    name: string;
    primary: string;
    primaryDark: string;
    accent: string;
}[];

function colorPaletteFactory(main: string, dark: string, accent: string): PaletteOptions {
    return {
        primary: {
            main: main,
            dark: dark,
        },
        secondary: {
            main: accent,
        },
        error: {
            main: '#BF261D',
        },
        warning: {
            main: '#D18D23',
        },
        info: {
            main: '#1D7C9C',
        },
        success: {
            main: '#449456',
        },
    }
}

const defaultColorPalettes: {
    [key: string]: PaletteOptions;
} = {
    Tiefsee: colorPaletteFactory('#113a8d', '#113a8d', '#b6f1dc'),
    Feuersturm: colorPaletteFactory('#d73234', '#a2140f', '#f8edb2'),
    Urwald: colorPaletteFactory('#60865e', '#446249', '#dad8ce'),
    Sonnenschein: colorPaletteFactory('#fdc022', '#dca41a', '#dee2e6'),
    Aubergine: colorPaletteFactory('#bc457c', '#763d55', '#d7f4ea'),
    Mitternacht: colorPaletteFactory('#2e3d45', '#181715', '#e9e0d6'),
}

const customerColorPalettes: {
    [key: string]: PaletteOptions;
} = CustomerThemes.reduce((acc, val) => {
    acc[val.name] = colorPaletteFactory(val.primary, val.primaryDark, val.accent);
    return acc;
}, {} as any);

export const Themes: string[] = [
    ...Object.keys(defaultColorPalettes),
    ...Object.keys(customerColorPalettes),
];

export function getColorPalette(theme?: string): PaletteOptions {
    if (theme == null) {
        return defaultColorPalettes.Mitternacht;
    } else if (theme in defaultColorPalettes) {
        return defaultColorPalettes[theme];
    } else if (theme in customerColorPalettes) {
        return customerColorPalettes[theme];
    } else {
        return defaultColorPalettes.Mitternacht
    }
}

export function createAppTheme(theme?: string, baseTheme?: Theme) {
    const palette = getColorPalette(theme)
    return createTheme({
        ...baseTheme,
        palette,
        components: {
            ...baseTheme?.components,
            MuiStepLabel: {
                ...baseTheme?.components?.MuiStepLabel,
                styleOverrides: {
                    ...baseTheme?.components?.MuiStepLabel?.styleOverrides,
                    label: {
                        // @ts-ignore
                        ...baseTheme?.components?.MuiStepLabel?.styleOverrides?.label,
                        '&.Mui-active': {
                            // @ts-ignore
                            color: palette?.primary?.main,
                        },
                    }
                }
            }
        }
    }, coreLocale, datePickerLocale);
}
