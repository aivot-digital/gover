import {render, screen} from '@testing-library/react';
import {ThemeProvider} from '@mui/material';
import {describe, expect, it, vi} from 'vitest';
import {ElementWidthSelector} from './element-width-selector';
import {ElementType} from '../../data/element-type/element-type';
import {BaseTheme} from '../../theming/base-theme';

describe('ElementWidthSelector', () => {
    it('suppresses the application-wide FormControl margin inside FormField', () => {
        const {container} = render(
            <ThemeProvider theme={BaseTheme}>
                <ElementWidthSelector
                    label="Elementbreite"
                    elementType={ElementType.Text}
                    value={6}
                    onChange={vi.fn()}
                    hint="Bestimmt die Breite des Elements."
                    margin="none"
                />
            </ThemeProvider>,
        );

        const label = screen.getByTitle('Elementbreite');
        const select = container.querySelector('[role="combobox"]');
        const helperId = select?.getAttribute('aria-describedby');

        // Direct relationship assertions avoid a JSDOM CSS resolution bug in accessible-name queries.
        expect(select).toHaveAttribute('aria-labelledby', label.id);
        expect(document.getElementById(helperId!)).toHaveTextContent('Bestimmt die Breite des Elements.');
        expect(container.querySelector('.MuiTextField-root')).not.toHaveClass('MuiFormControl-marginNormal');
        expect(getComputedStyle(container.querySelector('.MuiInputBase-root')!).minHeight).toBe('44px');
    });
});
