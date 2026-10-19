import {type ComponentProps, useState} from 'react';
import {render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {ElementType} from '../../data/element-type/element-type';
import {AutocompleteSelect} from './autocomplete-select';
import {ThemeProvider} from '@mui/material';
import {BaseTheme} from '../../theming/base-theme';

describe('AutocompleteSelect', () => {
    it('associates the external label and hint with the combobox', async () => {
        const user = userEvent.setup();
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

        await user.click(input);
        expect(screen.getAllByRole('option')).toHaveLength(26);
    });

    it('groups text options in a stable order with common names and company details first', async () => {
        const user = userEvent.setup();
        renderField();
        await user.click(screen.getByRole('combobox'));

        const listbox = screen.getByRole('listbox');
        expect(within(listbox).getAllByRole('group').map(group => group.getAttribute('aria-label'))).toEqual([
            'Name',
            'Kontakt',
            'Unternehmen und Beruf',
            'Anschrift',
            'Weitere Angaben',
        ]);
        expect(within(listbox).getAllByRole('option')).toHaveLength(26);
        expect(within(screen.getByRole('group', {name: 'Name'})).getAllByRole('option').slice(0, 3)).toEqual([
            screen.getByRole('option', {name: /^Vorname/}),
            screen.getByRole('option', {name: /^Nachname/}),
            screen.getByRole('option', {name: /^Vollständiger Name/}),
        ]);
        expect(within(screen.getByRole('group', {name: 'Unternehmen und Beruf'})).getAllByRole('option')).toEqual([
            screen.getByRole('option', {name: /^Unternehmensname/}),
            screen.getByRole('option', {name: /^Berufsbezeichnung/}),
        ]);
        expect(screen.getByRole('option', {name: /^Weitere Verwaltungsebene \(4\)/})).toHaveTextContent(
            'Vierte und feinste Verwaltungsebene',
        );
    });

    it('offers only compatible select options and their groups in the same semantic order', async () => {
        const user = userEvent.setup();
        renderField({type: ElementType.Select});
        await user.click(screen.getByRole('combobox'));

        const listbox = screen.getByRole('listbox');
        expect(within(listbox).getAllByRole('group').map(group => group.getAttribute('aria-label'))).toEqual([
            'Name',
            'Anschrift',
            'Weitere Angaben',
        ]);
        expect(within(listbox).getAllByRole('option')).toEqual([
            screen.getByRole('option', {name: /^Anrede/}),
            screen.getByRole('option', {name: /^Nachgestellter Titel/}),
            screen.getByRole('option', {name: /^Ort \/ Stadt/}),
            screen.getByRole('option', {name: /^Land country-name/}),
            screen.getByRole('option', {name: /^Bundesland \/ Region/}),
            screen.getByRole('option', {name: /^Ländercode/}),
            screen.getByRole('option', {name: /^Bevorzugte Sprache/}),
            screen.getByRole('option', {name: /^Geschlechtsidentität/}),
        ]);
    });

    it('keeps the two birth date options ungrouped', async () => {
        const user = userEvent.setup();
        renderField({type: ElementType.Date});
        await user.click(screen.getByRole('combobox'));

        const listbox = screen.getByRole('listbox');
        expect(within(listbox).queryByRole('group')).not.toBeInTheDocument();
        expect(within(listbox).getAllByRole('option')).toEqual([
            screen.getByRole('option', {name: /^Geburtsdatum/}),
            screen.getByRole('option', {name: /^Geburtsjahr/}),
        ]);
    });

    it.each([
        {query: 'Unternehmensname', expectedOption: /^Unternehmensname/, count: 1},
        {query: 'organization', expectedOption: /^Unternehmensname/, count: 2},
        {query: 'vorangestellter', expectedOption: /^Anrede/, count: 1},
        {query: 'Kontakt', expectedOption: /^E-Mail-Adresse/, count: 3},
    ])('finds options when searching for "$query"', async ({query, expectedOption, count}) => {
        const user = userEvent.setup();
        renderField();
        await user.type(screen.getByRole('combobox'), query);

        expect(screen.getByRole('option', {name: expectedOption})).toBeInTheDocument();
        expect(screen.getAllByRole('option')).toHaveLength(count);
    });

    it('supports keyboard selection and clearing while storing the technical key', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();

        function Harness() {
            const [value, setValue] = useState<string>();
            return (
                <AutocompleteSelect
                    type={ElementType.Text}
                    value={value}
                    editable
                    onChange={(nextValue) => {
                        setValue(nextValue);
                        onChange(nextValue);
                    }}
                />
            );
        }

        render(<ThemeProvider theme={BaseTheme}><Harness/></ThemeProvider>);
        const input = screen.getByRole('combobox');
        await user.type(input, 'organization');
        await user.keyboard('{Enter}');

        expect(onChange).toHaveBeenLastCalledWith('organization');
        expect(input).toHaveValue('Unternehmensname (organization)');
        expect(screen.queryByRole('listbox')).not.toBeInTheDocument();

        await user.click(screen.getByRole('button', {name: 'Leeren'}));
        expect(onChange).toHaveBeenLastCalledWith(undefined);
        expect(input).toHaveValue('');

        await user.click(input);
        await user.keyboard('{End}{Enter}');
        expect(onChange).toHaveBeenLastCalledWith('nickname');
        expect(input).toHaveValue('Spitzname (nickname)');
    });

    it('keeps a configured value visible when editing is disabled', () => {
        const onChange = vi.fn();
        renderField({value: 'organization', editable: false, onChange});

        const input = screen.getByRole('combobox');
        expect(input).toBeDisabled();
        expect(input).toHaveValue('Unternehmensname (organization)');
        expect(onChange).not.toHaveBeenCalled();
    });
});

function renderField(props: Partial<ComponentProps<typeof AutocompleteSelect>> = {}) {
    return render(
        <ThemeProvider theme={BaseTheme}>
            <AutocompleteSelect
                type={ElementType.Text}
                value={null}
                onChange={vi.fn()}
                editable
                {...props}
            />
        </ThemeProvider>,
    );
}
