import {createTheme, type Theme as MuiTheme} from '@mui/material';
import {deDE as datePickerLocale} from '@mui/x-date-pickers/locales';
import {deDE as coreLocale} from '@mui/material/locale';
import {type Theme} from '../modules/themes/models/theme';
import {type PaletteOptions} from '@mui/material/styles/createPalette';

const softShadows = [
    'none',
    // Level 1 — Default / Base shadow
    '0px 2px 5px rgba(0, 0, 0, 0.1)',
    // Level 2 — Slightly deeper
    '0px 3px 6px rgba(0, 0, 0, 0.12)',
    // Level 3 — Softer spread, a bit more depth
    '0px 4px 8px rgba(0, 0, 0, 0.14)',
    // Level 4 — Combined small + large shadows for softness
    '0px 1px 3px rgba(0, 0, 0, 0.08), 0px 4px 8px rgba(0, 0, 0, 0.12)',
    // Level 5 — Moderate depth (good for cards)
    '0px 2px 4px rgba(0, 0, 0, 0.06), 0px 6px 12px rgba(0, 0, 0, 0.14)',
    // Level 6 — Popovers / Menus
    '0px 3px 5px rgba(0, 0, 0, 0.08), 0px 8px 16px rgba(0, 0, 0, 0.16)',
    // Level 7 — Floating surfaces
    '0px 4px 6px rgba(0, 0, 0, 0.08), 0px 10px 20px rgba(0, 0, 0, 0.18)',
    // Level 8 — Dialogs / Modals
    '0px 6px 10px rgba(0, 0, 0, 0.10), 0px 12px 24px rgba(0, 0, 0, 0.20)',
    // Level 9 — Drawers / elevated overlays
    '0px 8px 12px rgba(0, 0, 0, 0.10), 0px 14px 28px rgba(0, 0, 0, 0.22)',
    // Level 10 — High elevation layers
    '0px 10px 16px rgba(0, 0, 0, 0.12), 0px 16px 32px rgba(0, 0, 0, 0.24)',
    // Levels 11–24 — progressively deeper, smooth falloff
    ...Array.from({ length: 14 }, (_, i) => {
        const n = i + 11;
        const offset = Math.round((n - 10) * 1.5 + 10);
        const blur = offset * 2;
        const alpha1 = (0.12 + (n - 10) * 0.005).toFixed(3);
        const alpha2 = (0.20 + (n - 10) * 0.008).toFixed(3);
        return `0px ${offset}px ${offset * 1.5}px rgba(0,0,0,${alpha1}), 0px ${
            offset + 2
        }px ${blur}px rgba(0,0,0,${alpha2})`;
    }),
];

// In dark mode, shadows should be subtler to simulate less ambient light
const darkSoftShadows = softShadows.map(s =>
    s.replace(/rgba\(0, 0, 0, ([^)]+)\)/g, 'rgba(0,0,0,calc($1 * 0.6))')
);

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
        mode: 'light',
        background: {
            default: "#F6F6F6"
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
                            // @ts-ignore
                            color: palette.primary?.main ?? '#253e63',
                        },
                    },
                },
            },
        },
        shadows: softShadows as any,
    }, coreLocale, datePickerLocale);
}
