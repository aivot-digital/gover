import React from 'react';
import {render, screen} from '@testing-library/react';
import {createTheme, ThemeProvider} from '@mui/material';
import {afterEach, beforeEach, describe, expect, it} from 'vitest';
import {Logo} from './logo';

describe('Logo', () => {
    let originalLogoUrl: string | null;
    let originalLogoUrlDark: string | null;

    beforeEach(() => {
        originalLogoUrl = AppConfig.logoUrl;
        originalLogoUrlDark = AppConfig.logoUrlDark;
        AppConfig.logoUrl = '/system-light.svg';
        AppConfig.logoUrlDark = '/system-dark.svg';
    });

    afterEach(() => {
        AppConfig.logoUrl = originalLogoUrl;
        AppConfig.logoUrlDark = originalLogoUrlDark;
    });

    it('does not render the system logo for an explicitly media-less theme', () => {
        render(<Logo src={null} srcDark={null}/>);

        expect(screen.queryByRole('img')).not.toBeInTheDocument();
    });

    it('uses the system theme when no specific theme media is supplied', () => {
        render(<Logo/>);

        expect(screen.getByRole('img')).toHaveAttribute('src', '/system-light.svg');
    });

    it('uses the light logo of the same theme when its dark logo is missing', () => {
        const darkTheme = createTheme({palette: {mode: 'dark'}});

        render(
            <ThemeProvider theme={darkTheme}>
                <Logo src="/theme-light.svg" srcDark={null}/>
            </ThemeProvider>,
        );

        expect(screen.getByRole('img')).toHaveAttribute('src', '/theme-light.svg');
    });
});
