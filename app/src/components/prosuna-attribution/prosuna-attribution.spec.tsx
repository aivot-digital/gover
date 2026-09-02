import {createTheme, ThemeProvider} from '@mui/material';
import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';
import {ProsunaAttribution} from './prosuna-attribution';

describe('ProsunaAttribution', () => {
    it('links the complete monochrome attribution to the Prosuna website', () => {
        render(<ProsunaAttribution placement="form"/>);
        const link = screen.getByRole('link', {
            name: /Realisiert mit Prosuna – der quelloffenen Plattform für Ende-zu-Ende digitalisierte Verwaltungsprozesse\./,
        });
        const logo = link.querySelector('svg');
        const logoPaths = logo?.querySelectorAll('path') ?? [];

        expect(link).toHaveAttribute(
            'href',
            'https://prosuna.de/?utm_source=prosuna_instance&utm_medium=referral' +
            '&utm_campaign=footer_attribution&utm_content=form_footer',
        );
        expect(link).toHaveAttribute('target', '_blank');
        expect(link).toHaveAttribute('rel', 'noopener noreferrer');
        expect(logo).toHaveAttribute('viewBox', '0 0 1000 165');
        expect(logoPaths).toHaveLength(8);
        logoPaths.forEach((path) => expect(path).toHaveAttribute('fill', 'currentColor'));
    });

    it('uses a monochrome logo in dark mode', () => {
        const theme = createTheme({palette: {mode: 'dark'}});
        render(
            <ThemeProvider theme={theme}>
                <ProsunaAttribution placement="listing"/>
            </ThemeProvider>,
        );
        const link = screen.getByRole('link', {
            name: /Realisiert mit Prosuna – der quelloffenen Plattform für Ende-zu-Ende digitalisierte Verwaltungsprozesse\./,
        });
        const logo = link.querySelector('svg');
        const logoPaths = logo?.querySelectorAll('path') ?? [];

        expect(logo).toHaveAttribute('viewBox', '0 0 1000 165');
        expect(logoPaths).toHaveLength(8);
        logoPaths.forEach((path) => expect(path).toHaveAttribute('fill', 'currentColor'));
        expect(link).toHaveAttribute('href', expect.stringContaining('utm_content=listing_footer'));
    });
});
