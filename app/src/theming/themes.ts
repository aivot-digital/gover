import {alpha, createTheme, type PaletteMode, type Theme as MuiTheme} from '@mui/material';
import {deDE as datePickerLocale} from '@mui/x-date-pickers/locales';
import {deDE as coreLocale} from '@mui/material/locale';
import {grey as muiGrey} from '@mui/material/colors';
import {type Theme} from '../modules/themes/models/theme';
import {type PaletteOptions} from '@mui/material/styles';
import {
    DEFAULT_APPEARANCE_COLORS,
    MINIMUM_THEME_CONTRAST,
    mixHexColors,
    resolveAppearanceColors,
} from './resolve-appearance-colors';

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

/**
 * Untinted neutral foundations for the page and Paper hierarchy. They are kept application-controlled instead of
 * being exposed as additional appearance settings, so an appearance remains defined by two understandable brand
 * colors rather than a complete design-system palette.
 */
export const APP_BACKGROUND_COLORS = {
    light: {
        default: '#F6F6F6',
        paper: '#FFFFFF',
    },
    dark: {
        default: '#121212',
        paper: '#1C1C1C',
    },
} as const;

/**
 * MUI and several application components use `palette.grey` directly for borders, connectors, disabled states, and
 * subtle fills. A very small primary-color admixture keeps those elements visually related to the active appearance
 * instead of placing cool neutral greys next to a potentially warm brand color. The weight deliberately stays low:
 * grey must continue to read as neutral and retain its ordered light-to-dark scale.
 */
const NEUTRAL_TINT_WEIGHT = 0.03;

/**
 * Backgrounds use separate, deliberately subtle weights because they cover much larger areas than controls.
 * The asymmetric values preserve the established page-to-Paper hierarchy in each mode: light Paper stays especially
 * clean, while dark Paper may carry slightly more brand character without becoming a colored surface.
 */
const BACKGROUND_TINT_WEIGHTS = {
    light: {
        default: 0.03,
        paper: 0.01,
    },
    dark: {
        default: 0.02,
        paper: 0.03,
    },
} as const;

/**
 * Derives application backgrounds by mixing the active primary brand color into the neutral foundations.
 * This provides a coherent overall cast without using the primary color itself as a large surface.
 */
export function resolveAppBackgroundColors(
    mode: PaletteMode,
    primaryColor: string,
): {default: string; paper: string} {
    const baseColors = APP_BACKGROUND_COLORS[mode];
    const weights = BACKGROUND_TINT_WEIGHTS[mode];

    return {
        default: mixHexColors(baseColors.default, primaryColor, weights.default),
        paper: mixHexColors(baseColors.paper, primaryColor, weights.paper),
    };
}

/** Applies the same subtle brand cast to every MUI grey shade while preserving the complete shade scale. */
function createTintedGreyPalette(primaryColor: string): typeof muiGrey {
    return Object.fromEntries(
        Object.entries(muiGrey).map(([shade, color]) => [
            shade,
            mixHexColors(color, primaryColor, NEUTRAL_TINT_WEIGHT),
        ]),
    ) as typeof muiGrey;
}

export function createDefaultAppTheme(
    baseTheme: MuiTheme,
    mode: PaletteMode = baseTheme.palette.mode,
): MuiTheme {
    return createAppTheme(undefined, baseTheme, mode);
}

export function createAppTheme(
    appTheme: Theme | undefined,
    baseTheme: MuiTheme,
    mode: PaletteMode = baseTheme.palette.mode,
): MuiTheme {
    // Custom dark colors are optional. When absent, the configured light colors intentionally remain the brand source
    // for both modes; the standard appearance has explicit dark defaults because there is no persisted configuration.
    const appearanceColorInput = appTheme == null
        ? {
            primaryColor: mode === 'dark'
                ? DEFAULT_APPEARANCE_COLORS.primaryColorDark
                : DEFAULT_APPEARANCE_COLORS.primaryColor,
            secondaryColor: mode === 'dark'
                ? DEFAULT_APPEARANCE_COLORS.secondaryColorDark
                : DEFAULT_APPEARANCE_COLORS.secondaryColor,
        }
        : {
            primaryColor: mode === 'dark'
                ? appTheme.primaryColorDark ?? appTheme.primaryColor
                : appTheme.primaryColor,
            secondaryColor: mode === 'dark'
                ? appTheme.secondaryColorDark ?? appTheme.secondaryColor
                : appTheme.secondaryColor,
        };

    // Resolve the effective tinted Paper first: foreground roles must be checked against the surface they are
    // actually rendered on, not against the untinted foundation or the page background.
    const background = resolveAppBackgroundColors(mode, appearanceColorInput.primaryColor);
    const resolvedColors = resolveAppearanceColors(
        appearanceColorInput,
        background.paper,
    );

    // Links sit on Paper rather than on a solid primary fill, so they use the contrast-adjusted foreground role.
    const linkStyles = {
        '.MuiTypography-body2 > a, .MuiAccordionDetails-root a': {
            color: resolvedColors.primaryForeground,
            textDecoration: 'none',
            position: 'relative',
            transition: 'all 150ms ease-in-out',
            zIndex: 1,
            padding: '2px 0',
            display: 'inline-block',
        },
        '.MuiTypography-body2 > a::before, .MuiAccordionDetails-root a::before': {
            content: '""',
            display: 'block',
            position: 'absolute',
            left: 0,
            right: 0,
            bottom: 0,
            height: 1,
            backgroundColor: alpha(mode === 'dark' ? '#FFFFFF' : '#000000', 0.3),
            transition: 'all 150ms ease-in-out',
            zIndex: 0,
        },
        '.MuiTypography-body2 > a::after, .MuiAccordionDetails-root a::after': {
            content: '""',
            display: 'block',
            position: 'absolute',
            width: '100%',
            height: 0,
            bottom: 0,
            left: 0,
            backgroundColor: alpha(resolvedColors.primaryForeground, 0.08),
            transition: 'all 150ms ease-in-out',
            zIndex: -1,
        },
        '.MuiTypography-body2 > a:hover::after, .MuiAccordionDetails-root a:hover::after': {
            height: '100%',
        },
        '.MuiTypography-body2 > a:hover::before, .MuiAccordionDetails-root a:hover::before': {
            backgroundColor: resolvedColors.primaryForeground,
        },
    };
    const baseCssBaselineStyles = baseTheme.components?.MuiCssBaseline?.styleOverrides;
    const palette: PaletteOptions = {
        contrastThreshold: MINIMUM_THEME_CONTRAST,
        // Supplying the tinted scale globally keeps MUI and custom consumers of `palette.grey` consistent.
        grey: createTintedGreyPalette(resolvedColors.primary),
        DataGrid: {
            bg: background.paper,
            headerBg: alpha(mode === 'dark' ? '#FFFFFF' : '#000000', mode === 'dark' ? 0.08 : 0.04),
            pinnedBg: background.paper,
        },
        primary: {
            main: resolvedColors.primary,
            contrastText: resolvedColors.onPrimary,
        },
        secondary: {
            main: resolvedColors.secondary,
            contrastText: resolvedColors.onSecondary,
        },
        mode,
        background,
    };

    // Filled controls can use the configured main colors together with their on-color contrast text. Components that
    // render the brand as text, an outline, or a thin indicator instead use the Paper-safe foreground roles below.
    return createTheme({
        ...baseTheme,
        palette,
        components: {
            ...baseTheme?.components,
            MuiCssBaseline: {
                ...baseTheme.components?.MuiCssBaseline,
                styleOverrides: (theme) => [
                    typeof baseCssBaselineStyles === 'function'
                        ? baseCssBaselineStyles(theme)
                        : baseCssBaselineStyles,
                    linkStyles,
                ],
            },
            MuiButton: {
                ...baseTheme?.components?.MuiButton,
                styleOverrides: {
                    ...baseTheme?.components?.MuiButton?.styleOverrides,
                    textPrimary: {
                        color: resolvedColors.primaryForeground,
                        '&:hover': {
                            backgroundColor: alpha(resolvedColors.primaryForeground, 0.06),
                        },
                    },
                    outlinedPrimary: {
                        color: resolvedColors.primaryForeground,
                        borderColor: alpha(resolvedColors.primaryForeground, 0.5),
                        '&:hover': {
                            borderColor: resolvedColors.primaryForeground,
                            backgroundColor: alpha(resolvedColors.primaryForeground, 0.06),
                        },
                    },
                    textSecondary: {
                        color: resolvedColors.secondaryForeground,
                        '&:hover': {
                            backgroundColor: alpha(resolvedColors.secondaryForeground, 0.06),
                        },
                    },
                    outlinedSecondary: {
                        color: resolvedColors.secondaryForeground,
                        borderColor: alpha(resolvedColors.secondaryForeground, 0.5),
                        '&:hover': {
                            borderColor: resolvedColors.secondaryForeground,
                            backgroundColor: alpha(resolvedColors.secondaryForeground, 0.06),
                        },
                    },
                },
            },
            MuiTab: {
                ...baseTheme.components?.MuiTab,
                styleOverrides: {
                    ...baseTheme.components?.MuiTab?.styleOverrides,
                    root: [
                        baseTheme.components?.MuiTab?.styleOverrides?.root,
                        {
                            '&.Mui-selected': {
                                color: resolvedColors.primaryForeground,
                            },
                        },
                    ],
                },
            },
            MuiTabs: {
                ...baseTheme.components?.MuiTabs,
                styleOverrides: {
                    ...baseTheme.components?.MuiTabs?.styleOverrides,
                    indicator: [
                        baseTheme.components?.MuiTabs?.styleOverrides?.indicator,
                        {
                            backgroundColor: resolvedColors.primaryForeground,
                        },
                    ],
                },
            },
            MuiStepLabel: {
                ...baseTheme?.components?.MuiStepLabel,
                styleOverrides: {
                    ...baseTheme?.components?.MuiStepLabel?.styleOverrides,
                    label: [
                        baseTheme?.components?.MuiStepLabel?.styleOverrides?.label,
                        {
                            '&.Mui-active': {
                                color: resolvedColors.primaryForeground,
                            },
                        },
                    ],
                },
            },
        },
        shadows: softShadows as any,
    }, coreLocale, datePickerLocale);
}
