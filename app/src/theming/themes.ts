import {createTheme, type Theme as MuiTheme} from '@mui/material';
import {deDE as datePickerLocale} from '@mui/x-date-pickers/locales/deDE';
import {deDE as coreLocale} from '@mui/material/locale';
import {type Theme} from '../modules/themes/models/theme';
import {type PaletteOptions} from '@mui/material/styles/createPalette';


export function createDefaultAppTheme(baseTheme: MuiTheme): MuiTheme {
    return createAppTheme(undefined, baseTheme);
}

export function createAppTheme(appTheme: Theme | undefined, baseTheme: MuiTheme): MuiTheme {
    const palette: PaletteOptions = {
        primary: {
            main: appTheme?.main ?? '#253B5B',
            dark: appTheme?.mainDark ?? '#102334',
        },
        secondary: {
            main: appTheme?.accent ?? '#F8D27C',
        },
        error: {
            main: appTheme?.error ?? '#CD362D',
        },
        warning: {
            main: appTheme?.warning ?? '#B55E06',
        },
        info: {
            main: appTheme?.info ?? '#1F7894',
        },
        success: {
            main: appTheme?.success ?? '#378550',
        },
        mode: (appTheme?.name ?? '').startsWith('_') ? 'dark' : 'light',
    };
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
                        // @ts-expect-error
                        ...baseTheme?.components?.MuiStepLabel?.styleOverrides?.label,
                        '&.Mui-active': {
                            // @ts-ignore
                            color: palette.primary?.main ?? '#253e63',
                        },
                    },
                },
            },
        },
    }, coreLocale, datePickerLocale);
}
