import {createTheme, type Theme as MuiTheme} from '@mui/material';
import {deDE as datePickerLocale} from '@mui/x-date-pickers';
import {deDE as coreLocale} from '@mui/material/locale';
import {type Theme} from '../models/entities/theme';


export function createDefaultAppTheme(baseTheme: MuiTheme): MuiTheme {
    return createAppTheme(undefined, baseTheme);
}

export function createAppTheme(appTheme: Theme | undefined, baseTheme: MuiTheme): MuiTheme {
    const palette = {
        primary: {
            main: appTheme?.main ?? '#253e63',
            dark: appTheme?.mainDark ?? '#142638',
        },
        secondary: {
            main: appTheme?.accent ?? '#ffd481',
        },
        error: {
            main: appTheme?.error ?? '#BF261D',
        },
        warning: {
            main: appTheme?.warning ?? '#D18D23',
        },
        info: {
            main: appTheme?.info ?? '#1D7C9C',
        },
        success: {
            main: appTheme?.success ?? '#449456',
        },
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
                            color: palette.primary.main,
                        },
                    },
                },
            },
        },
    }, coreLocale, datePickerLocale);
}
