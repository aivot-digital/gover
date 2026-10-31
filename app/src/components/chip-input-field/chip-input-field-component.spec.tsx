import {fireEvent, render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {useState} from 'react';
import {describe, expect, it, vi} from 'vitest';
import {ChipInputFieldComponent, type ChipInputFieldComponentProps} from './chip-input-field-component';

vi.mock('../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => vi.fn(),
}));

function ControlledField(props: Partial<ChipInputFieldComponentProps>) {
    const [value, setValue] = useState<string[] | null>(props.value ?? null);
    return <ChipInputFieldComponent label="Tags" {...props} value={value} onChange={next => {
        setValue(next);
        props.onChange?.(next);
    }}/>;
}

function paste(input: HTMLElement, text: string) {
    return fireEvent.paste(input, {clipboardData: {getData: () => text}});
}

describe('ChipInputFieldComponent', () => {
    it('switches from the minimum indicator to the maximum counter as entries are added', () => {
        render(<ControlledField minItems={2} maxItems={5} required hint="Passende Begriffe ergänzen."/>);
        const input = screen.getByRole('combobox');
        expect(input).toHaveAccessibleDescription(/Passende Begriffe ergänzen.*Mindestens 2 Einträge/);
        fireEvent.change(input, {target: {value: 'A'}});
        fireEvent.keyDown(input, {key: 'Enter'});
        expect(input).toHaveAccessibleDescription(/Noch mindestens 1 Eintrag/);
        expect(screen.queryByText('1/5 Einträge')).not.toBeInTheDocument();
        fireEvent.change(input, {target: {value: 'B'}});
        fireEvent.keyDown(input, {key: 'Enter'});
        expect(screen.getByText('2/5 Einträge')).toBeVisible();
        expect(input).toHaveAccessibleDescription(/2 von 5 Einträgen verwendet/);
        expect(screen.queryByText(/Noch mindestens/)).not.toBeInTheDocument();
    });

    it('describes an optional minimum without marking the empty field invalid', () => {
        render(<ControlledField minItems={2} maxItems={5}/>);
        const input = screen.getByRole('combobox');
        expect(input).toHaveAccessibleDescription('Bei Eingabe mindestens 2 Einträge');
        expect(input).not.toHaveAttribute('aria-invalid', 'true');
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('hides the minimum-only indicator once the minimum is satisfied', () => {
        render(<ControlledField minItems={1} required/>);
        const input = screen.getByRole('combobox');
        expect(input).toHaveAccessibleDescription('Mindestens 1 Eintrag');
        fireEvent.change(input, {target: {value: 'A'}});
        fireEvent.blur(input);
        expect(screen.queryByText('Mindestens 1 Eintrag')).not.toBeInTheDocument();
        expect(input).not.toHaveAttribute('aria-describedby');
    });

    it('does not show indicators for unlimited counts and handles a maximum of one', () => {
        const props = {label: 'Tags', value: [], onChange: vi.fn()};
        const {rerender} = render(<ChipInputFieldComponent {...props} minItems={0} maxItems={0}/>);
        expect(screen.getByRole('combobox')).not.toHaveAttribute('aria-describedby');
        rerender(<ChipInputFieldComponent {...props} maxItems={1}/>);
        expect(screen.getByText('0/1 Eintrag')).toBeVisible();
        expect(screen.getByRole('combobox')).toHaveAccessibleDescription('0 von 1 Eintrag verwendet');
    });

    it('combines the external label with Autocomplete semantics', () => {
        render(
            <ChipInputFieldComponent
                label="Tags"
                value={['Antrag']}
                onChange={vi.fn()}
                hint="Ordnen Sie passende Begriffe zu."
                maxItems={3}
            />,
        );

        const input = screen.getByRole('combobox', {name: 'Tags – optional'});
        expect(input).toHaveAttribute('aria-autocomplete', 'list');
        expect(input).toHaveAttribute('aria-expanded', 'false');
        expect(input).toHaveAccessibleDescription(/Ordnen Sie passende Begriffe zu/);
        expect(input).toHaveAccessibleDescription(/1 von 3 Einträgen verwendet/);
        expect(screen.getByText('1 von 3 Einträgen verwendet')).toHaveClass('visually-hidden');
    });

    it('renders one externally associated error message', () => {
        render(
            <ChipInputFieldComponent
                label="Identitäten"
                value={null}
                onChange={vi.fn()}
                error="Wählen Sie mindestens eine Identität."
                required
            />,
        );

        const input = screen.getByRole('combobox', {name: 'Identitäten'});
        expect(input).toHaveAttribute('aria-invalid', 'true');
        expect(input).toHaveAccessibleDescription('Wählen Sie mindestens eine Identität.');
        expect(screen.getAllByRole('alert')).toHaveLength(1);
    });

    it('preserves free-solo keyboard entry through the Autocomplete slots', () => {
        const onChange = vi.fn();

        render(
            <ChipInputFieldComponent
                label="Tags"
                value={['Antrag']}
                onChange={onChange}
            />,
        );

        const input = screen.getByRole('combobox', {name: 'Tags – optional'});
        fireEvent.change(input, {target: {value: 'Frist'}});
        fireEvent.keyDown(input, {key: 'Enter'});

        expect(onChange).toHaveBeenCalledWith(['Antrag', 'Frist']);
        expect(onChange).toHaveBeenCalledTimes(1);
    });

    it('commits only the highlighted suggestion and does not recommit it on blur', () => {
        const onChange = vi.fn();
        render(<ControlledField suggestions={['Berlin']} onChange={onChange}/>);
        const input = screen.getByRole('combobox');
        fireEvent.change(input, {target: {value: 'Ber'}});
        fireEvent.keyDown(input, {key: 'ArrowDown'});
        fireEvent.keyDown(input, {key: 'Enter'});
        fireEvent.blur(input);
        expect(onChange.mock.calls).toEqual([[['Berlin']]]);
    });

    it('displays option labels and preserves their values when selecting suggestions', () => {
        const onChange = vi.fn();
        const labels: Record<string, string> = {create: 'Erstellt', update: 'Aktualisiert'};
        render(<ControlledField value={['create']} suggestions={['create', 'update']}
                                getOptionLabel={value => labels[value] ?? value} onChange={onChange}/>);
        expect(screen.getByText('Erstellt')).toBeVisible();

        fireEvent.change(screen.getByRole('combobox'), {target: {value: 'Aktu'}});
        fireEvent.click(screen.getByRole('option', {name: 'Aktualisiert'}));

        expect(onChange).toHaveBeenLastCalledWith(['create', 'update']);
        expect(screen.getByText('Aktualisiert')).toBeVisible();
    });

    it('resolves typed and pasted labels to their values while preserving unknown entries', () => {
        const onChange = vi.fn();
        const labels: Record<string, string> = {create: 'Erstellt', update: 'Aktualisiert'};
        render(<ControlledField suggestions={['create', 'update']}
                                getOptionLabel={value => labels[value] ?? value} onChange={onChange}/>);
        const input = screen.getByRole('combobox');

        fireEvent.change(input, {target: {value: 'Erstellt'}});
        fireEvent.keyDown(input, {key: 'Enter'});
        expect(onChange).toHaveBeenLastCalledWith(['create']);

        paste(input, 'Aktualisiert\nCustom');

        expect(onChange).toHaveBeenLastCalledWith(['create', 'update', 'Custom']);
    });

    it('commits the draft on Tab without preventing focus movement', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        render(<><ControlledField onChange={onChange}/><button>Weiter</button></>);
        const input = screen.getByRole('combobox');
        await user.click(input);
        await user.type(input, 'Frist');
        await user.tab();
        expect(screen.getByRole('button', {name: 'Weiter'})).toHaveFocus();
        expect(onChange.mock.calls).toEqual([[['Frist']]]);
    });

    it('commits before a following button handles its click', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        const onContinue = vi.fn();
        render(<><ControlledField onChange={onChange}/><button onClick={onContinue}>Weiter</button></>);
        await user.type(screen.getByRole('combobox'), 'Frist');
        await user.click(screen.getByRole('button', {name: 'Weiter'}));
        expect(onChange).toHaveBeenCalledWith(['Frist']);
        expect(onChange.mock.invocationCallOrder[0]).toBeLessThan(onContinue.mock.invocationCallOrder[0]);
    });

    it('waits until composition is complete before handling Enter', () => {
        const onChange = vi.fn();
        render(<ControlledField onChange={onChange}/>);
        const input = screen.getByRole('combobox');
        fireEvent.compositionStart(input);
        fireEvent.change(input, {target: {value: '東京'}});
        fireEvent.keyDown(input, {key: 'Enter', keyCode: 229, which: 229, isComposing: true});
        expect(onChange).not.toHaveBeenCalled();
        fireEvent.compositionEnd(input);
        fireEvent.keyDown(input, {key: 'Enter'});
        expect(onChange.mock.calls).toEqual([[['東京']]]);
    });

    it.each(['readOnly', 'disabled', 'busy'] as const)('does not commit a pending draft after becoming %s', (state) => {
        const onChange = vi.fn();
        const {rerender} = render(<ControlledField onChange={onChange}/>);
        const input = screen.getByRole('combobox');
        fireEvent.change(input, {target: {value: 'Entwurf'}});
        rerender(<ControlledField onChange={onChange} {...{[state]: true}}/>);
        fireEvent.keyDown(input, {key: 'Enter'});
        fireEvent.blur(input);
        paste(input, 'A\nB');
        expect(onChange).not.toHaveBeenCalled();
    });

    it('commits finished text when composition ends after blur', () => {
        const onChange = vi.fn();
        render(<ControlledField onChange={onChange}/>);
        const input = screen.getByRole('combobox');
        fireEvent.compositionStart(input);
        fireEvent.change(input, {target: {value: '東京'}});
        fireEvent.blur(input);
        expect(onChange).not.toHaveBeenCalled();
        fireEvent.compositionEnd(input);
        expect(onChange.mock.calls).toEqual([[['東京']]]);
    });

    it('allows manual duplicates', () => {
        const onChange = vi.fn();
        render(<ControlledField value={['Berlin']} suggestions={['Berlin']} allowDuplicates onChange={onChange}/>);
        const input = screen.getByRole('combobox');
        fireEvent.change(input, {target: {value: 'Berlin'}});
        fireEvent.keyDown(input, {key: 'Enter'});
        expect(onChange).toHaveBeenLastCalledWith(['Berlin', 'Berlin']);
    });

    it('still removes a selected suggestion when it is clicked again', () => {
        const onChange = vi.fn();
        render(<ControlledField value={['Berlin']} suggestions={['Berlin']} allowDuplicates onChange={onChange}/>);
        const input = screen.getByRole('combobox');
        fireEvent.change(input, {target: {value: 'Ber'}});
        fireEvent.click(screen.getByRole('option', {name: 'Berlin'}));
        fireEvent.blur(input);
        expect(onChange.mock.calls).toEqual([[null]]);
    });

    it('leaves unrelated over-limit values in place when removing a chip', () => {
        const onChange = vi.fn();
        render(<ControlledField value={['A', 'B', 'C', 'D']} maxItems={2} onChange={onChange}/>);
        expect(screen.getByRole('alert')).toHaveTextContent('Maximal 2 Einträge erlaubt.');
        fireEvent.click(screen.getAllByTestId('CancelIcon')[0]);
        expect(onChange).toHaveBeenCalledWith(['B', 'C', 'D']);
    });

    it('does not deduplicate existing entries on removal', () => {
        const onChange = vi.fn();
        render(<ControlledField value={['A', 'A', 'B']} onChange={onChange}/>);
        fireEvent.click(screen.getAllByTestId('CancelIcon')[2]);
        expect(onChange).toHaveBeenCalledWith(['A', 'A']);
        expect(screen.getByRole('alert')).toHaveTextContent('Mehrfach vorhandene Einträge sind nicht erlaubt.');
    });

    it('does not emit a change for a rejected duplicate or an empty draft', () => {
        const onChange = vi.fn();
        render(<ControlledField value={['Frist']} onChange={onChange}/>);
        const input = screen.getByRole('combobox');
        fireEvent.change(input, {target: {value: ' Frist '}});
        fireEvent.keyDown(input, {key: 'Enter'});
        fireEvent.blur(input);
        expect(onChange).not.toHaveBeenCalled();
        expect(input).toHaveValue('');
    });

    it('satisfies native required validation when at least one chip exists', () => {
        render(<ControlledField required/>);
        const input = screen.getByRole('combobox') as HTMLInputElement;
        expect(input.validity.valueMissing).toBe(true);
        fireEvent.change(input, {target: {value: 'Frist'}});
        fireEvent.keyDown(input, {key: 'Enter'});
        expect(input.validity.valueMissing).toBe(false);
        expect(input).toHaveAttribute('aria-required', 'true');
    });

    it('marks oversized entries without truncating or discarding them', () => {
        const onChange = vi.fn();
        render(<ControlledField maxItemLength={3} onChange={onChange}/>);
        const input = screen.getByRole('combobox');
        fireEvent.change(input, {target: {value: 'Frist'}});
        fireEvent.blur(input);
        expect(onChange).toHaveBeenCalledWith(['Frist']);
        expect(screen.getByRole('alert')).toHaveTextContent('Jeder Eintrag darf höchstens 3 Zeichen enthalten.');
        expect(input).toHaveAttribute('aria-invalid', 'true');
    });

    it('applies a minimum to optional non-empty lists and keeps empty lists valid', () => {
        const {rerender} = render(<ChipInputFieldComponent label="Tags" value={[]} onChange={vi.fn()} minItems={2}/>);
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
        rerender(<ChipInputFieldComponent label="Tags" value={['A']} onChange={vi.fn()} minItems={2}/>);
        expect(screen.getByRole('alert')).toHaveTextContent('Mindestens 2 Einträge erforderlich.');
    });

    it('validates entry length after trimming like the backend without changing existing values', () => {
        const onChange = vi.fn();
        render(<ControlledField value={[' ABC ']} maxItemLength={3} onChange={onChange}/>);
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
        expect(screen.getByRole('combobox')).not.toHaveAttribute('aria-invalid', 'true');
        expect(onChange).not.toHaveBeenCalled();
    });

    it('keeps hints visible when errors are an empty string', () => {
        render(<ControlledField error="" hint="Begriffe ergänzen"/>);
        expect(screen.getByRole('combobox')).toHaveAccessibleDescription('Begriffe ergänzen');
    });

    it('splits pasted lines, trims entries and ignores duplicates and empty lines', () => {
        const onChange = vi.fn();
        render(<ControlledField value={['A']} onChange={onChange}/>);
        const input = screen.getByRole('combobox');
        expect(paste(input, ' A\r\n B \rC\n\n')).toBe(false);
        fireEvent.blur(input);
        expect(onChange.mock.calls).toEqual([[['A', 'B', 'C']]]);
    });

    it('splits configured separators while preserving text around the replaced selection', () => {
        const onChange = vi.fn();
        render(<ControlledField pasteSeparators={[',', ';', '\t']} onChange={onChange}/>);
        const input = screen.getByRole('combobox') as HTMLInputElement;
        fireEvent.change(input, {target: {value: 'preXXpost'}});
        input.setSelectionRange(3, 5);
        paste(input, 'A,B;C\tD');
        expect(onChange).toHaveBeenCalledWith(['preA', 'B', 'C', 'Dpost']);
    });

    it.each([undefined, []])('does not split commas without an explicit separator setting: %j', (pasteSeparators) => {
        const onChange = vi.fn();
        render(<ControlledField pasteSeparators={pasteSeparators} onChange={onChange}/>);
        expect(paste(screen.getByRole('combobox'), 'A,B')).toBe(true);
        expect(onChange).not.toHaveBeenCalled();
    });

    it('allows disabling newline splitting', () => {
        render(<ControlledField pasteSeparators={[]}/>);
        expect(paste(screen.getByRole('combobox'), 'A\nB')).toBe(true);
    });

    it('rejects an oversized paste in full and retains the current draft and chips', () => {
        const onChange = vi.fn();
        render(<ControlledField value={['A']} maxItems={2} onChange={onChange}/>);
        const input = screen.getByRole('combobox');
        fireEvent.change(input, {target: {value: 'Draft'}});
        paste(input, 'B\nC');
        expect(onChange).not.toHaveBeenCalled();
        expect(input).toHaveValue('Draft');
        expect(screen.getByRole('status')).toHaveTextContent('Die Einträge wurden nicht übernommen. Maximal 2 Einträge möglich.');
    });

    it('explains a rejected paste even when the existing entries fail minimum validation', () => {
        const onChange = vi.fn();
        render(<ControlledField value={['A']} minItems={2} maxItems={3} onChange={onChange}/>);
        const input = screen.getByRole('combobox');
        paste(input, 'B\nC\nD');
        expect(onChange).not.toHaveBeenCalled();
        expect(screen.getByRole('alert')).toHaveTextContent('Mindestens 2 Einträge erforderlich.');
        expect(screen.getByRole('status')).toHaveTextContent('Die Einträge wurden nicht übernommen. Maximal 3 Einträge möglich.');
        expect(input).toHaveAccessibleDescription(/Die Einträge wurden nicht übernommen/);
    });
});
