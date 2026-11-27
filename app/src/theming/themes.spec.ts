import {describe, expect, it} from 'vitest';
import {createElement} from 'react';
import {render, screen} from '@testing-library/react';
import {Alert, alertClasses, Button, ThemeProvider} from '@mui/material';
import {DataGrid, gridClasses} from '@mui/x-data-grid';
import {createTheme, decomposeColor, getContrastRatio as getMuiContrastRatio, rgbToHex} from '@mui/material/styles';
import {grey as muiGrey} from '@mui/material/colors';
import {BaseTheme} from './base-theme';
import {createAppTheme, createDefaultAppTheme, resolveAppBackgroundColors} from './themes';
import {getColorContrastRatio, MINIMUM_THEME_CONTRAST, mixHexColors} from './resolve-appearance-colors';

const severityColors = ['error', 'warning', 'info', 'success'] as const;

function getButtonVariantStyle(
    theme: ReturnType<typeof createDefaultAppTheme>,
    variant: 'text' | 'outlined',
    color: 'primary' | 'secondary',
): {color: string} {
    const rootOverrides = theme.components?.MuiButton?.styleOverrides?.root as unknown[];
    const appearanceOverride = rootOverrides[rootOverrides.length - 1] as {
        variants: Array<{
            props: {variant: string; color: string};
            style: {color: string};
        }>;
    };

    return appearanceOverride.variants.find((entry) => (
        entry.props.variant === variant && entry.props.color === color
    ))!.style;
}

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

    it('lets the DataGrid sort button inherit custom header backgrounds', () => {
        const theme = createDefaultAppTheme(BaseTheme, 'dark');
        const sortButtonSelector = `& .${gridClasses.columnHeader} .${gridClasses.sortButton}`;
        const rootOverride = theme.components?.MuiDataGrid?.styleOverrides?.root as (
            props: {theme: typeof theme}
        ) => Record<string, unknown>;

        expect(rootOverride({theme})).toMatchObject({
            [sortButtonSelector]: {
                backgroundColor: 'transparent',
                '&:hover': {
                    backgroundColor: theme.palette.action.hover,
                },
            },
        });

        render(createElement(
            ThemeProvider,
            {theme},
            createElement('div', {style: {width: 400, height: 240}}, createElement(DataGrid, {
                rows: [{id: 1, name: 'Example'}],
                columns: [{field: 'name', headerName: 'Name', width: 180}],
                initialState: {
                    sorting: {
                        sortModel: [{field: 'name', sort: 'asc'}],
                    },
                },
            })),
        ));

        const sortButton = document.querySelector(`.${gridClasses.sortButton}`);
        expect(sortButton).not.toBeNull();
        expect(window.getComputedStyle(sortButton!).backgroundColor).toBe('rgba(0, 0, 0, 0)');
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
        expect(resolvedOverrides[1]).toHaveProperty('.MuiTypography-body2 > a:not(.MuiButtonBase-root), .MuiAccordionDetails-root a:not(.MuiButtonBase-root)');
    });

    it('applies text-link decorations without overriding button links in accordions', () => {
        const theme = createDefaultAppTheme(BaseTheme);
        const baselineOverride = theme.components?.MuiCssBaseline?.styleOverrides as unknown as (
            theme: typeof BaseTheme,
        ) => Record<string, unknown>[];
        const linkStyles = baselineOverride(theme)[1];
        const container = document.createElement('div');
        container.className = 'MuiAccordionDetails-root';
        container.innerHTML = '<p class="MuiTypography-body2"><a href="/help">Hilfe</a><a class="MuiButtonBase-root MuiButton-root" href="/tasks/17/42">Aufgabe aufrufen</a></p>';
        const [textLink, buttonLink] = container.querySelectorAll('a');

        for (const selector of Object.keys(linkStyles)) {
            // Pseudo-elements and hover states must exclude button links as well.
            const elementSelector = selector.replace(/::(?:before|after)|:hover/g, '');
            expect(textLink.matches(elementSelector)).toBe(true);
            expect(buttonLink.matches(elementSelector)).toBe(false);
        }
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
            logoKeyDark: null,
            faviconKey: null,
        }, BaseTheme);
        const primaryTextButtonStyle = getButtonVariantStyle(theme, 'text', 'primary');
        const secondaryTextButtonStyle = getButtonVariantStyle(theme, 'text', 'secondary');
        const tabRootOverrides = theme.components?.MuiTab?.styleOverrides?.root as unknown[];
        const appearanceTabRootOverride = tabRootOverrides[tabRootOverrides.length - 1] as {
            '&.Mui-selected': {color: string};
        };

        expect(theme.palette.primary.main).toBe('#FF613A');
        expect(theme.palette.secondary.main).toBe('#BCE9FF');
        expect(primaryTextButtonStyle.color).not.toBe(theme.palette.primary.main);
        expect(secondaryTextButtonStyle.color).not.toBe(theme.palette.secondary.main);
        expect(getColorContrastRatio(
            primaryTextButtonStyle.color,
            theme.palette.background.paper,
        )).toBeGreaterThanOrEqual(MINIMUM_THEME_CONTRAST);
        expect(getColorContrastRatio(
            appearanceTabRootOverride['&.Mui-selected'].color,
            theme.palette.background.paper,
        )).toBeGreaterThanOrEqual(MINIMUM_THEME_CONTRAST);
    });

    it('applies appearance colors through the MUI v9 root variants', () => {
        const theme = createAppTheme({
            id: 1,
            name: 'Rendered appearance',
            primaryColor: '#FF613A',
            secondaryColor: '#BCE9FF',
            primaryColorDark: null,
            secondaryColorDark: null,
            logoKey: null,
            logoKeyDark: null,
            faviconKey: null,
        }, BaseTheme);
        const expectedStyle = getButtonVariantStyle(theme, 'text', 'primary');

        render(createElement(
            ThemeProvider,
            {theme},
            createElement(Button, {variant: 'text', color: 'primary'}, 'Primary action'),
        ));

        expect(screen.getByRole('button', {name: 'Primary action'})).toHaveStyle({
            color: expectedStyle.color,
        });
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
            logoKeyDark: null,
            faviconKey: null,
        }, BaseTheme, 'dark');
        const primaryTextButtonStyle = getButtonVariantStyle(theme, 'text', 'primary');
        const secondaryTextButtonStyle = getButtonVariantStyle(theme, 'text', 'secondary');
        const expectedBackground = resolveAppBackgroundColors('dark', '#253B5B');

        expect(theme.palette.mode).toBe('dark');
        expect(theme.palette.background.default).toBe(expectedBackground.default);
        expect(theme.palette.background.paper).toBe(expectedBackground.paper);
        expect(theme.palette.primary.main).toBe('#253B5B');
        expect(theme.palette.secondary.main).toBe('#5F6368');
        expect(getColorContrastRatio(
            primaryTextButtonStyle.color,
            theme.palette.background.paper,
        )).toBeGreaterThanOrEqual(MINIMUM_THEME_CONTRAST);
        expect(getColorContrastRatio(
            secondaryTextButtonStyle.color,
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
            logoKeyDark: null,
            faviconKey: null,
        };

        const lightTheme = createAppTheme(appTheme, BaseTheme, 'light');
        const darkTheme = createAppTheme(appTheme, BaseTheme, 'dark');

        expect(lightTheme.palette.primary.main).toBe('#253B5B');
        expect(lightTheme.palette.secondary.main).toBe('#5F6368');
        expect(darkTheme.palette.primary.main).toBe('#8EA9D1');
        expect(darkTheme.palette.secondary.main).toBe('#B1B5BA');
    });

    it.each(['light', 'dark'] as const)('keeps severity text and outlines readable in %s mode', (mode) => {
        const theme = createDefaultAppTheme(BaseTheme, mode);
        // Use the primary colors that produce the lowest-contrast surface tints in each mode.
        const customTheme = createAppTheme({
            primaryColor: '#000000',
            secondaryColor: '#FFFFFF',
            primaryColorDark: '#FFFFFF',
            secondaryColorDark: '#000000',
        }, BaseTheme, mode);

        for (const severity of severityColors) {
            expect(customTheme.palette[severity]).toEqual(theme.palette[severity]);
            const {main, light, dark, contrastText} = theme.palette[severity];
            for (const background of [customTheme.palette.background.paper, customTheme.palette.background.default]) {
                expect(getColorContrastRatio(main, background), severity).toBeGreaterThanOrEqual(MINIMUM_THEME_CONTRAST);
                expect(getColorContrastRatio(light, background), severity).toBeGreaterThanOrEqual(3);
                const buttonHoverBackground = mixHexColors(background, main, theme.palette.action.hoverOpacity);
                expect(getColorContrastRatio(main, buttonHoverBackground), severity)
                    .toBeGreaterThanOrEqual(MINIMUM_THEME_CONTRAST);
            }
            expect(getColorContrastRatio(contrastText, main), severity).toBeGreaterThanOrEqual(MINIMUM_THEME_CONTRAST);
            expect(getColorContrastRatio(contrastText, dark), severity).toBeGreaterThanOrEqual(MINIMUM_THEME_CONTRAST);
        }
    });

    it.each(['light', 'dark'] as const)('keeps MUI alert text readable in %s mode', (mode) => {
        const theme = createDefaultAppTheme(BaseTheme, mode);
        const alertVariants = ['standard', 'outlined', 'filled'] as const;
        render(createElement(
            ThemeProvider,
            {theme},
            severityColors.flatMap((severity) => alertVariants.map((variant) => {
                const label = `${severity} ${variant}`;
                return createElement(Alert, {key: label, 'aria-label': label, severity, variant}, label);
            })),
        ));

        for (const severity of severityColors) {
            for (const variant of alertVariants) {
                const label = `${severity} ${variant}`;
                const {color, backgroundColor} = getComputedStyle(screen.getByRole('alert', {name: label}));
                const background = variant === 'outlined' ? theme.palette.background.paper : rgbToHex(backgroundColor);
                const {values: [red, green, blue, opacity = 1]} = decomposeColor(color);
                const foreground = mixHexColors(background, rgbToHex(`rgb(${red}, ${green}, ${blue})`), opacity);
                expect(getColorContrastRatio(foreground, background), label).toBeGreaterThanOrEqual(MINIMUM_THEME_CONTRAST);
            }
        }
    });

    it('uses the lighter palette for standard warnings, filled alerts and enabled contained buttons', () => {
        const theme = createDefaultAppTheme(BaseTheme, 'light');
        const fills = [
            {severity: 'error', backgroundColor: '#C63946', color: '#FFFFFF'},
            {severity: 'warning', backgroundColor: '#FFCA79', color: '#2E1803'},
            {severity: 'info', backgroundColor: '#2B6CC6', color: '#FFFFFF'},
            {severity: 'success', backgroundColor: '#347F54', color: '#FFFFFF'},
        ] as const;
        render(createElement(ThemeProvider, {theme}, [
            createElement(Alert, {key: 'standard', severity: 'warning', 'aria-label': 'Standard warning'}, 'Warning'),
            createElement(Alert, {
                key: 'local',
                severity: 'warning',
                'aria-label': 'Warning on paper',
                sx: {bgcolor: 'background.paper'},
            }, 'Warning'),
            createElement(Button, {key: 'disabled', variant: 'contained', color: 'warning', disabled: true}, 'Disabled warning action'),
            ...fills.flatMap(({severity}) => [
                createElement(Alert, {key: `alert-${severity}`, color: severity, variant: 'filled', 'aria-label': severity}, severity),
                createElement(Button, {key: `button-${severity}`, color: severity, variant: 'contained'}, severity),
            ]),
        ]));

        const standardAlert = screen.getByRole('alert', {name: 'Standard warning'});
        expect(standardAlert).toHaveStyle({backgroundColor: '#FFF5E5', color: '#733607'});
        expect(standardAlert.querySelector(`.${alertClasses.icon}`)).toHaveStyle({color: '#C65809'});
        expect(screen.getByRole('alert', {name: 'Warning on paper'})).toHaveStyle({backgroundColor: theme.palette.background.paper});

        for (const {severity, ...style} of fills) {
            expect(screen.getByRole('alert', {name: severity})).toHaveStyle(style);
            expect(screen.getByRole('button', {name: severity})).toHaveStyle(style);
        }
        expect(screen.getByRole('button', {name: 'Disabled warning action'})).toHaveStyle({
            backgroundColor: theme.palette.action.disabledBackground,
        });
    });
});
