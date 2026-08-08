import {describe, expect, it} from 'vitest';
import {createTheme, getContrastRatio as getMuiContrastRatio} from '@mui/material/styles';
import {grey as muiGrey} from '@mui/material/colors';
import {BaseTheme} from './base-theme';
import {createAppTheme, createDefaultAppTheme, resolveAppBackgroundColors} from './themes';
import {getColorContrastRatio, MINIMUM_THEME_CONTRAST} from './resolve-appearance-colors';

describe('createAppTheme', () => {
    it.each([
        ['light', '#733635', '#A0C9CB'],
        ['dark', '#FF613A', '#A0C9CB'],
    ] as const)('maps the default %s-mode colors directly to the final MUI palette', (mode, primary, secondary) => {
        const theme = createDefaultAppTheme(BaseTheme, mode);

        expect(theme.palette.contrastThreshold).toBe(MINIMUM_THEME_CONTRAST);
        expect(theme.palette.primary.main).toBe(primary);
        expect(theme.palette.secondary.main).toBe(secondary);
    });

    it.each(['light', 'dark'] as const)('differentiates the DataGrid header in %s mode', (mode) => {
        const theme = createDefaultAppTheme(BaseTheme, mode);
        const dataGridPalette = (theme.palette as typeof theme.palette & {
            DataGrid: {bg: string; headerBg: string};
        }).DataGrid;

        expect(dataGridPalette.headerBg).not.toBe(dataGridPalette.bg);
        expect(dataGridPalette.bg).toBe(theme.palette.background.paper);
    });

    it.each(['light', 'dark'] as const)('subtly tints the neutral palette in %s mode', (mode) => {
        const theme = createDefaultAppTheme(BaseTheme, mode);
        const greyShades = [50, 100, 200, 300, 400, 500, 600, 700, 800, 900] as const;

        expect(theme.palette.grey[500]).not.toBe(muiGrey[500]);
        expect(theme.palette.background).toMatchObject(resolveAppBackgroundColors(
            mode,
            theme.palette.primary.main,
        ));

        for (let index = 1; index < greyShades.length; index += 1) {
            expect(getColorContrastRatio(theme.palette.grey[greyShades[index - 1]], '#000000'))
                .toBeGreaterThan(getColorContrastRatio(theme.palette.grey[greyShades[index]], '#000000'));
        }
    });

    it('preserves base component styles when adding appearance colors', () => {
        const theme = createDefaultAppTheme(BaseTheme);
        const labelOverride = theme.components?.MuiStepLabel?.styleOverrides?.label;
        const tabRootOverride = theme.components?.MuiTab?.styleOverrides?.root;

        expect(Array.isArray(labelOverride)).toBe(true);
        const labelOverrides = labelOverride as unknown[];
        expect(labelOverrides[0]).toBe(BaseTheme.components?.MuiStepLabel?.styleOverrides?.label);
        expect(labelOverrides[1]).toMatchObject({
            '&.Mui-active': {
                color: expect.any(String),
            },
        });

        expect(Array.isArray(tabRootOverride)).toBe(true);
        const tabRootOverrides = tabRootOverride as unknown[];
        expect(tabRootOverrides[0]).toBe(BaseTheme.components?.MuiTab?.styleOverrides?.root);
        expect(tabRootOverrides[1]).toMatchObject({
            '&.Mui-selected': {
                color: expect.any(String),
            },
        });
    });

    it('keeps existing global baseline styles', () => {
        const baseTheme = createTheme({
            components: {
                MuiCssBaseline: {
                    styleOverrides: {
                        body: {
                            scrollbarColor: 'red blue',
                        },
                    },
                },
            },
        });
        const theme = createDefaultAppTheme(baseTheme);
        const baselineOverride = theme.components?.MuiCssBaseline?.styleOverrides as unknown as (
            theme: typeof baseTheme,
        ) => unknown[];
        const resolvedOverrides = baselineOverride(theme);

        expect(resolvedOverrides[0]).toMatchObject({
            body: {
                scrollbarColor: 'red blue',
            },
        });
        expect(resolvedOverrides[1]).toHaveProperty('.MuiTypography-body2 > a, .MuiAccordionDetails-root a');
    });

    it('uses the same restrained elevation for drawers and dialogs', () => {
        const theme = createDefaultAppTheme(BaseTheme);
        const dialogPaperProps = theme.components?.MuiDialog?.defaultProps?.slotProps?.paper;

        expect(theme.components?.MuiDrawer?.defaultProps?.elevation).toBe(1);
        expect(dialogPaperProps).toMatchObject({elevation: 1});
    });

    it('keeps configured main colors exact and supplies accessible text button colors', () => {
        const theme = createAppTheme({
            id: 1,
            name: 'Vivid appearance',
            primaryColor: '#FF613A',
            secondaryColor: '#BCE9FF',
            primaryColorDark: null,
            secondaryColorDark: null,
            logoKey: null,
            faviconKey: null,
        }, BaseTheme);
        const buttonOverrides = theme.components?.MuiButton?.styleOverrides as Record<string, Record<string, unknown>>;
        const tabRootOverrides = theme.components?.MuiTab?.styleOverrides?.root as unknown[];
        const appearanceTabRootOverride = tabRootOverrides[tabRootOverrides.length - 1] as {
            '&.Mui-selected': {color: string};
        };

        expect(theme.palette.primary.main).toBe('#FF613A');
        expect(theme.palette.secondary.main).toBe('#BCE9FF');
        expect(buttonOverrides.textPrimary.color).not.toBe(theme.palette.primary.main);
        expect(buttonOverrides.textSecondary.color).not.toBe(theme.palette.secondary.main);
        expect(getColorContrastRatio(
            buttonOverrides.textPrimary.color as string,
            theme.palette.background.paper,
        )).toBeGreaterThanOrEqual(MINIMUM_THEME_CONTRAST);
        expect(getColorContrastRatio(
            appearanceTabRootOverride['&.Mui-selected'].color,
            theme.palette.background.paper,
        )).toBeGreaterThanOrEqual(MINIMUM_THEME_CONTRAST);
    });

    it('provides accessible contrast text for all fill colors', () => {
        const theme = createDefaultAppTheme(BaseTheme);

        for (const color of ['primary', 'secondary', 'error', 'warning', 'info', 'success'] as const) {
            expect(getMuiContrastRatio(
                theme.palette[color].contrastText,
                theme.palette[color].main,
            )).toBeGreaterThanOrEqual(MINIMUM_THEME_CONTRAST);
        }
    });

    it('supports dark mode without changing the configured MUI main colors', () => {
        const theme = createAppTheme({
            id: 1,
            name: 'Dark appearance',
            primaryColor: '#253B5B',
            secondaryColor: '#5F6368',
            primaryColorDark: null,
            secondaryColorDark: null,
            logoKey: null,
            faviconKey: null,
        }, BaseTheme, 'dark');
        const buttonOverrides = theme.components?.MuiButton?.styleOverrides as Record<string, Record<string, unknown>>;
        const expectedBackground = resolveAppBackgroundColors('dark', '#253B5B');

        expect(theme.palette.mode).toBe('dark');
        expect(theme.palette.background.default).toBe(expectedBackground.default);
        expect(theme.palette.background.paper).toBe(expectedBackground.paper);
        expect(theme.palette.primary.main).toBe('#253B5B');
        expect(theme.palette.secondary.main).toBe('#5F6368');
        expect(getColorContrastRatio(
            buttonOverrides.textPrimary.color as string,
            theme.palette.background.paper,
        )).toBeGreaterThanOrEqual(MINIMUM_THEME_CONTRAST);
        expect(getColorContrastRatio(
            buttonOverrides.textSecondary.color as string,
            theme.palette.background.paper,
        )).toBeGreaterThanOrEqual(MINIMUM_THEME_CONTRAST);
    });

    it('uses optional dark-mode colors only for the dark palette', () => {
        const appTheme = {
            id: 1,
            name: 'Dark appearance variants',
            primaryColor: '#253B5B',
            secondaryColor: '#5F6368',
            primaryColorDark: '#8EA9D1',
            secondaryColorDark: '#B1B5BA',
            logoKey: null,
            faviconKey: null,
        };

        const lightTheme = createAppTheme(appTheme, BaseTheme, 'light');
        const darkTheme = createAppTheme(appTheme, BaseTheme, 'dark');

        expect(lightTheme.palette.primary.main).toBe('#253B5B');
        expect(lightTheme.palette.secondary.main).toBe('#5F6368');
        expect(darkTheme.palette.primary.main).toBe('#8EA9D1');
        expect(darkTheme.palette.secondary.main).toBe('#B1B5BA');
    });

    it.each(['light', 'dark'] as const)('keeps the MUI %s-mode severity palette', (mode) => {
        const theme = createDefaultAppTheme(BaseTheme, mode);
        const muiTheme = createTheme({palette: {mode}});

        for (const color of ['error', 'warning', 'info', 'success'] as const) {
            expect(theme.palette[color].main).toBe(muiTheme.palette[color].main);
        }
    });
});
