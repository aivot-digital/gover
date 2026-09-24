import {describe, expect, it, vi} from 'vitest';
import {fireEvent, render, screen} from '@testing-library/react';
import {SelectFieldComponent} from './select-field-component';
import {SelectFieldPresentation} from '../../models/elements/form/input/select-field-presentation';
import {createTheme, ThemeProvider} from '@mui/material';

describe('SelectFieldComponent', () => {
    it.each(['light', 'dark'] as const)('renders a muted prompt without selecting an answer in %s mode', mode => {
        const onChange = vi.fn();
        const theme = createTheme({palette: {mode}});
        render(
            <ThemeProvider theme={theme}>
                <SelectFieldComponent label="Kategorie" value={null} onChange={onChange}
                    placeholder="Kategorie auswählen" options={[{value: 'request', label: 'Antrag'}]}/>
            </ThemeProvider>,
        );
        const control = screen.getByRole('combobox', {name: 'Kategorie – optional'});
        expect(control).toHaveTextContent('Kategorie auswählen');
        expect(screen.getByText('Kategorie auswählen')).toHaveStyle({color: theme.palette.text.secondary});
        expect(onChange).not.toHaveBeenCalled();
        fireEvent.mouseDown(control);
        expect(screen.queryByRole('option', {name: 'Kategorie auswählen'})).not.toBeInTheDocument();
        fireEvent.click(screen.getByRole('option', {name: 'Keine Auswahl'}));
        expect(onChange).not.toHaveBeenCalled();
    });

    it('shows an explicit empty filter value and preserves null when resetting it', () => {
        const onChange = vi.fn();
        const props = {
            label: 'Speichertyp', onChange, emptyOptionLabel: 'Alle Speichertypen',
            showOptionalIndicator: false, options: [{value: 's3', label: 'S3'}],
        };
        const {rerender} = render(<SelectFieldComponent {...props} value={null}/>);
        const control = screen.getByRole('combobox', {name: 'Speichertyp'});
        expect(control).toHaveTextContent('Alle Speichertypen');
        expect(screen.getByText('Alle Speichertypen')).toHaveStyle({color: getComputedStyle(control).color});
        rerender(<SelectFieldComponent {...props} value="s3"/>);
        expect(control).toHaveTextContent('S3');
        fireEvent.mouseDown(control);
        fireEvent.click(screen.getByRole('option', {name: 'Alle Speichertypen'}));
        expect(onChange).toHaveBeenCalledWith(null);
    });

    it.each([{required: true}, {includeEmptyOption: false}])('does not offer a reset for %j', flags => {
        render(<SelectFieldComponent label="Kategorie" value={null} onChange={vi.fn()}
            emptyOptionLabel="Alle Kategorien" options={[{value: 'request', label: 'Antrag'}]} {...flags}/>);
        const control = screen.getByRole('combobox');
        expect(control).toHaveTextContent('Bitte auswählen');
        fireEvent.mouseDown(control);
        expect(screen.queryByRole('option', {name: 'Alle Kategorien'})).not.toBeInTheDocument();
        expect(screen.queryByRole('option', {name: 'Keine Auswahl'})).not.toBeInTheDocument();
    });

    it('displays an empty options message without making it a selectable answer', () => {
        const onChange = vi.fn();
        render(<SelectFieldComponent label="Kategorie" value={null} onChange={onChange}
            options={[]} emptyStatePlaceholder="Keine Kategorien verfügbar"/>);
        const control = screen.getByRole('combobox');
        expect(control).toHaveTextContent('Keine Kategorien verfügbar');
        fireEvent.mouseDown(control);
        const status = screen.getByRole('option', {name: 'Keine Kategorien verfügbar'});
        expect(status).toHaveAttribute('aria-disabled', 'true');
        fireEvent.click(status);
        expect(onChange).not.toHaveBeenCalled();
    });

    it.each([SelectFieldPresentation.Dropdown, SelectFieldPresentation.Combobox])(
        'preserves an unavailable reference until its option arrives in %s', presentation => {
            const onChange = vi.fn();
            const props = {label: 'Kategorie', value: 'archived', onChange, presentation, emptyOptionLabel: 'Alle Kategorien'};
            const {rerender} = render(<SelectFieldComponent {...props} options={[]}/>);
            const control = screen.getByRole('combobox');
            if (presentation === SelectFieldPresentation.Dropdown) {
                expect(control).toHaveTextContent('Auswahl derzeit nicht verfügbar');
            } else {
                expect(control).toHaveAttribute('placeholder', 'Auswahl derzeit nicht verfügbar');
                expect(control).toHaveValue('');
            }
            expect(onChange).not.toHaveBeenCalled();
            rerender(<SelectFieldComponent {...props} options={[{value: 'archived', label: 'Archiviert'}]}/>);
            if (presentation === SelectFieldPresentation.Dropdown) {
                expect(control).toHaveTextContent('Archiviert');
            } else {
                expect(control).toHaveValue('Archiviert');
            }
            expect(onChange).not.toHaveBeenCalled();
        },
    );

    it('keeps combobox prompts separate from options and supports clearing a filter', () => {
        const onChange = vi.fn();
        const props = {
            label: 'Kategorie', onChange, presentation: SelectFieldPresentation.Combobox,
            options: [{value: 'request', label: 'Antrag'}],
        };
        const {rerender} = render(<SelectFieldComponent {...props} value={null}/>);
        const control = screen.getByRole('combobox');
        expect(control).toHaveAttribute('placeholder', 'Bitte auswählen');
        expect(control).toHaveValue('');
        rerender(<SelectFieldComponent {...props} value={null} placeholder="Kategorie suchen"/>);
        expect(control).toHaveAttribute('placeholder', 'Kategorie suchen');
        rerender(<SelectFieldComponent {...props} value={null} emptyOptionLabel="Alle Kategorien"/>);
        expect(control).toHaveAttribute('placeholder', 'Alle Kategorien');
        expect(control).toHaveValue('');
        fireEvent.change(control, {target: {value: 'Antrag'}});
        expect(screen.queryByRole('option', {name: 'Alle Kategorien'})).not.toBeInTheDocument();
        fireEvent.click(screen.getByRole('option', {name: 'Antrag'}));
        expect(onChange).toHaveBeenLastCalledWith('request');
        rerender(<SelectFieldComponent {...props} value="request" emptyOptionLabel="Alle Kategorien"/>);
        fireEvent.focus(control);
        fireEvent.click(screen.getByRole('button', {name: 'Clear'}));
        expect(onChange).toHaveBeenLastCalledWith(null);
    });

    it('allows explicitly clearing an unavailable dropdown reference without any remaining options', () => {
        const onChange = vi.fn();
        render(<SelectFieldComponent label="Kategorie" value="removed" onChange={onChange} options={[]}/>);
        fireEvent.mouseDown(screen.getByRole('combobox'));
        fireEvent.click(screen.getByRole('option', {name: 'Keine Auswahl'}));
        expect(onChange).toHaveBeenCalledExactlyOnceWith(null);
    });

    it('does not treat numeric zero as an empty selection', () => {
        render(<SelectFieldComponent label="Anzahl" value={0} onChange={vi.fn()}
            options={[{value: 0, label: 'Null'}]}/>);
        expect(screen.getByRole('combobox')).toHaveTextContent('Null');
    });

    it.each([{disabled: true}, {readOnly: true}, {busy: true}])('keeps an empty dropdown non-interactive for %j', flags => {
        render(<SelectFieldComponent label="Kategorie" value={null} onChange={vi.fn()}
            options={[{value: 'request', label: 'Antrag'}]} {...flags}/>);
        const control = screen.getByRole('combobox');
        expect(control).toHaveTextContent('Bitte auswählen');
        fireEvent.mouseDown(control);
        expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
    });

    it('associates its external label and hint with the combobox', () => {
        const {container} = render(
            <SelectFieldComponent
                label="Kategorie"
                value="request"
                onChange={vi.fn()}
                hint="Wählen Sie eine Kategorie."
                required
                options={[
                    {value: 'request', label: 'Antrag'},
                ]}
            />,
        );

        const select = container.querySelector('[role="combobox"]');
        expect(select).not.toBeNull();
        expect(select).toHaveAccessibleName('Kategorie');
        expect(select).toHaveAccessibleDescription('Wählen Sie eine Kategorie.');
        expect(select).toHaveAttribute('aria-required', 'true');
        expect(select?.closest('.MuiInputBase-root')).toHaveClass('MuiInputBase-sizeSmall');
        expect(screen.getByLabelText('Kategorie')).toBe(select);
    });

    it('exposes validation errors on the combobox', () => {
        const {container} = render(
            <SelectFieldComponent
                label="Kategorie"
                value={null}
                onChange={vi.fn()}
                error="Bitte wählen Sie eine Kategorie."
                options={[]}
            />,
        );

        const select = container.querySelector('[role="combobox"]');
        expect(select).not.toBeNull();
        expect(select).toHaveAccessibleName('Kategorie – optional');
        expect(select).toHaveAttribute('aria-invalid', 'true');
        expect(select).toHaveAccessibleDescription('Bitte wählen Sie eine Kategorie.');
    });

    it('uses the dropdown presentation by default', () => {
        const {container} = render(
            <SelectFieldComponent
                label="Kategorie"
                value="request"
                onChange={vi.fn()}
                options={[{value: 'request', label: 'Antrag'}]}
            />,
        );

        const control = container.querySelector('[role="combobox"]');
        expect(control).toHaveAccessibleName('Kategorie – optional');
        expect(control?.tagName).toBe('DIV');
        expect(control).toHaveTextContent('Antrag');
        expect(control).not.toHaveAttribute('aria-autocomplete');
    });

    it('preserves numeric option values when a dropdown selection changes', () => {
        const onChange = vi.fn();

        const {container} = render(
            <SelectFieldComponent
                label="Systemrolle"
                value={1}
                onChange={onChange}
                options={[
                    {value: 1, label: 'Administration'},
                    {value: 2, label: 'Sachbearbeitung'},
                ]}
            />,
        );

        const control = container.querySelector('[role="combobox"]');
        expect(control).toHaveAccessibleName('Systemrolle – optional');
        fireEvent.mouseDown(control!);
        const option = Array.from(document.querySelectorAll('[role="option"]'))
            .find((candidate) => candidate.textContent?.includes('Sachbearbeitung'));
        expect(option).toBeDefined();
        fireEvent.click(option!);

        expect(onChange).toHaveBeenCalledWith(2);
    });

    it('matches legacy numeric input against string-backed options', () => {
        const {container} = render(
            <SelectFieldComponent
                label="Systemrolle"
                value={4 as unknown as string}
                onChange={vi.fn()}
                options={[{value: '4', label: 'Sachbearbeitung'}]}
            />,
        );

        const control = container.querySelector('[role="combobox"]');
        expect(control).toHaveAccessibleName('Systemrolle – optional');
        expect(control).toHaveTextContent('Sachbearbeitung');
    });

    it('renders a searchable, constrained combobox and emits its option value', () => {
        const onChange = vi.fn();

        render(
            <SelectFieldComponent
                label="Kategorie"
                value={null}
                onChange={onChange}
                presentation={SelectFieldPresentation.Combobox}
                options={[
                    {value: 'request', label: 'Antrag', subLabel: 'Neuen Antrag erfassen'},
                    {value: 'approval', label: 'Freigabe', subLabel: 'Entscheidung einholen'},
                ]}
            />,
        );

        const input = screen.getByRole('combobox', {name: 'Kategorie – optional'});
        expect(input.tagName).toBe('INPUT');
        expect(input).toHaveAttribute('aria-autocomplete', 'list');

        fireEvent.change(input, {target: {value: 'Entscheidung'}});
        fireEvent.click(screen.getByRole('option', {name: /Freigabe/}));

        expect(onChange).toHaveBeenCalledWith('approval');
    });
});
