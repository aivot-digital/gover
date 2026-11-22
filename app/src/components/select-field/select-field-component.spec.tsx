import {afterEach, describe, expect, it, vi} from 'vitest';
import {act, fireEvent, render, screen} from '@testing-library/react';
import {SelectFieldComponent} from './select-field-component';
import {SelectFieldPresentation} from '../../models/elements/form/input/select-field-presentation';
import {createTheme, ThemeProvider} from '@mui/material';

function mockTextWidth(element: HTMLElement, scrollWidth: number, clientWidth: number) {
    Object.defineProperties(element, {
        scrollWidth: {configurable: true, value: scrollWidth},
        clientWidth: {configurable: true, value: clientWidth},
    });
}

function advanceTooltipTimers() {
    act(() => vi.advanceTimersByTime(300));
    act(() => vi.advanceTimersByTime(300));
}

describe('SelectFieldComponent', () => {
    afterEach(() => vi.useRealTimers());

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

    it('shows the full selected label on hover only while it is truncated', () => {
        vi.useFakeTimers();
        const label = 'Eine besonders lange ausgewählte Kategorie';
        render(<SelectFieldComponent label="Kategorie" value="request" onChange={vi.fn()}
            options={[{value: 'request', label}]}/>);
        const selectedLabel = screen.getByText(label);

        mockTextWidth(selectedLabel, 120, 120);
        fireEvent.mouseOver(selectedLabel);
        advanceTooltipTimers();
        expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
        expect(selectedLabel).not.toHaveAttribute('title');
        fireEvent.focus(screen.getByRole('combobox'));
        advanceTooltipTimers();
        expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
        fireEvent.blur(screen.getByRole('combobox'));

        fireEvent.mouseLeave(selectedLabel);
        advanceTooltipTimers();
        mockTextWidth(selectedLabel, 240, 120);
        fireEvent.mouseOver(selectedLabel);
        advanceTooltipTimers();
        expect(screen.getByRole('tooltip')).toHaveTextContent(label);

        fireEvent.mouseLeave(selectedLabel);
        advanceTooltipTimers();
        expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
    });

    it.each([{}, {readOnly: true}, {busy: true}])('reveals a truncated selection on combobox focus without changing its accessible name for %j', flags => {
        vi.useFakeTimers();
        const label = 'Eine besonders lange ausgewählte Kategorie';
        render(<SelectFieldComponent label="Kategorie" hint="Wählen Sie eine Kategorie."
            value="request" onChange={vi.fn()} options={[{value: 'request', label}]} {...flags}/>);
        const control = screen.getByRole('combobox', {name: 'Kategorie – optional'});
        const selectedLabel = screen.getByText(label);
        mockTextWidth(selectedLabel, 240, 120);

        fireEvent.focus(control);
        advanceTooltipTimers();
        expect(screen.getByRole('tooltip')).toHaveTextContent(label);
        expect(control).toHaveAccessibleName('Kategorie – optional');
        expect(control).toHaveAccessibleDescription('Wählen Sie eine Kategorie.');
        expect(selectedLabel).not.toHaveAttribute('tabindex');

        fireEvent.keyDown(document, {key: 'Escape'});
        advanceTooltipTimers();
        expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
        fireEvent.focus(control);
        advanceTooltipTimers();
        expect(screen.getByRole('tooltip')).toHaveTextContent(label);

        fireEvent.blur(control);
        advanceTooltipTimers();
        expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
    });

    it('closes the selected-label tooltip when the menu opens and preserves pass-through callbacks', () => {
        vi.useFakeTimers();
        const onFocus = vi.fn();
        const onBlur = vi.fn();
        const onOpen = vi.fn();
        const label = 'Eine besonders lange ausgewählte Kategorie';
        render(<SelectFieldComponent label="Kategorie" value="request" onChange={vi.fn()}
            options={[{value: 'request', label}]}
            muiPassTroughProps={{slotProps: {select: {onFocus, onBlur, onOpen}}}}/>);
        const control = screen.getByRole('combobox');
        mockTextWidth(screen.getByText(label), 240, 120);

        fireEvent.focus(control);
        advanceTooltipTimers();
        expect(screen.getByRole('tooltip')).toHaveTextContent(label);
        expect(onFocus).toHaveBeenCalledOnce();

        fireEvent.blur(control);
        advanceTooltipTimers();
        expect(onBlur).toHaveBeenCalledOnce();
        fireEvent.focus(control);
        advanceTooltipTimers();
        fireEvent.mouseDown(control);
        advanceTooltipTimers();
        expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
        expect(screen.getByRole('listbox')).toBeInTheDocument();
        expect(onOpen).toHaveBeenCalledOnce();
    });

    it('closes an old selected-label tooltip after the selected value changes', () => {
        vi.useFakeTimers();
        const label = 'Eine besonders lange ausgewählte Kategorie';
        const props = {
            label: 'Kategorie', onChange: vi.fn(),
            options: [{value: 'request', label}, {value: 'approval', label: 'Freigabe'}],
        };
        const {rerender} = render(<SelectFieldComponent {...props} value="request"/>);
        mockTextWidth(screen.getByText(label), 240, 120);
        fireEvent.focus(screen.getByRole('combobox'));
        advanceTooltipTimers();
        expect(screen.getByRole('tooltip')).toHaveTextContent(label);

        rerender(<SelectFieldComponent {...props} value="approval"/>);
        advanceTooltipTimers();
        expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
        expect(screen.getByRole('combobox')).toHaveTextContent('Freigabe');
    });

    it.each(['textfield', 'input', 'select'] as const)('preserves %s focus and blur callbacks with the existing slot precedence', layer => {
        const fieldOnFocus = vi.fn();
        const fieldOnBlur = vi.fn();
        const inputOnFocus = vi.fn();
        const inputOnBlur = vi.fn();
        const selectOnFocus = vi.fn();
        const selectOnBlur = vi.fn();
        render(<SelectFieldComponent label="Kategorie" value="request" onChange={vi.fn()}
            options={[{value: 'request', label: 'Antrag'}]}
            muiPassTroughProps={{
                onFocus: fieldOnFocus,
                onBlur: fieldOnBlur,
                slotProps: {
                    input: layer === 'textfield' ? undefined : {onFocus: inputOnFocus, onBlur: inputOnBlur},
                    select: layer === 'select' ? {onFocus: selectOnFocus, onBlur: selectOnBlur} : undefined,
                },
            }}/>
        );

        fireEvent.focus(screen.getByRole('combobox'));
        fireEvent.blur(screen.getByRole('combobox'));

        expect(fieldOnFocus).toHaveBeenCalledTimes(layer === 'textfield' ? 1 : 0);
        expect(fieldOnBlur).toHaveBeenCalledTimes(layer === 'textfield' ? 1 : 0);
        expect(inputOnFocus).toHaveBeenCalledTimes(layer === 'input' ? 1 : 0);
        expect(inputOnBlur).toHaveBeenCalledTimes(layer === 'input' ? 1 : 0);
        expect(selectOnFocus).toHaveBeenCalledTimes(layer === 'select' ? 1 : 0);
        expect(selectOnBlur).toHaveBeenCalledTimes(layer === 'select' ? 1 : 0);
    });

    it('keeps end actions and their tooltips independent from the selected label and option selection', () => {
        vi.useFakeTimers();
        const onChange = vi.fn();
        const onAction = vi.fn();
        const label = 'Eine besonders lange ausgewählte Kategorie';
        render(<SelectFieldComponent label="Kategorie" value="request" onChange={onChange}
            options={[{value: 'request', label}, {value: 'approval', label: 'Freigabe'}]}
            endAction={{icon: <span aria-hidden="true">+</span>, tooltip: 'Details anzeigen', onClick: onAction}}/>);
        mockTextWidth(screen.getByText(label), 240, 120);
        const action = screen.getByRole('button', {name: 'Details anzeigen'});

        fireEvent.mouseOver(action);
        advanceTooltipTimers();
        expect(screen.getByRole('tooltip')).toHaveTextContent('Details anzeigen');
        fireEvent.click(action);
        expect(onAction).toHaveBeenCalledOnce();
        expect(onChange).not.toHaveBeenCalled();
        expect(screen.queryByRole('listbox')).not.toBeInTheDocument();

        fireEvent.mouseLeave(action);
        advanceTooltipTimers();
        fireEvent.mouseDown(screen.getByRole('combobox'));
        fireEvent.click(screen.getByRole('option', {name: 'Freigabe'}));
        expect(onChange).toHaveBeenCalledExactlyOnceWith('approval');
    });

    it('does not reserve an end-adornment area for an empty action array', () => {
        const {container} = render(<SelectFieldComponent label="Kategorie" value="request" onChange={vi.fn()}
            options={[{value: 'request', label: 'Antrag'}]} endAction={[]}/>);
        expect(container.querySelector('.MuiInputAdornment-positionEnd')).not.toBeInTheDocument();
    });
});
