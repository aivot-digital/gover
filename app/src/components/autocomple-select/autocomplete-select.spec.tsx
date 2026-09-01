import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {ElementType} from '../../data/element-type/element-type';
import {AutocompleteSelect} from './autocomplete-select';
import {ThemeProvider} from '@mui/material';
import {BaseTheme} from '../../theming/base-theme';

describe('AutocompleteSelect', () => {
    it('associates the external label and hint with the combobox', () => {
        render(
            <ThemeProvider theme={BaseTheme}>
                <AutocompleteSelect
                    type={ElementType.Text}
                    value="name"
                    onChange={vi.fn()}
                    editable
                />
            </ThemeProvider>,
        );

        const input = screen.getByRole('combobox', {
            name: 'Automatisches Ausfüllen durch den Browser (Autocomplete) – optional',
        });

        expect(input).toHaveAccessibleDescription(
            'Legen Sie fest, welches Datenfeld der Browser zur Autovervollständigung vorschlagen soll (z. B. Name, E-Mail). Vorschläge sind browserabhängig.',
        );
        expect(input).toHaveValue('Vollständiger Name (name)');
        expect(input.closest('.MuiTextField-root')).not.toHaveClass('MuiFormControl-marginNormal');
        expect(getComputedStyle(input.closest('.MuiInputBase-root')!).minHeight).toBe('44px');
    });
});
