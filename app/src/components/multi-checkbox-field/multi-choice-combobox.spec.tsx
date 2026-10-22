import {fireEvent, render, screen} from '@testing-library/react';
import {useState} from 'react';
import {describe, expect, it, vi} from 'vitest';
import {MultiChoiceCombobox} from './multi-choice-combobox';

const options = [{value: 'a', label: 'Bildung'}, {value: 'b', label: 'Wohnen'}, {value: 'c', label: 'Gesundheit'}];

function Field({onChange = vi.fn()}) {
    const [value, setValue] = useState<string[] | null>(['a']);
    return <MultiChoiceCombobox label="Bereiche" options={options} value={value}
        onChange={(next) => {setValue(next); onChange(next);}}/>;
}

describe('fixed multiple choice', () => {
    it('retains selections while searching and stores option values', () => {
        const onChange = vi.fn();
        render(<Field onChange={onChange}/>);
        const input = screen.getByRole('combobox', {name: /Bereiche/});
        fireEvent.focus(input);
        fireEvent.change(input, {target: {value: 'woh'}});
        expect(screen.getByText('Bildung')).toBeVisible();
        fireEvent.click(screen.getByRole('option', {name: 'Wohnen'}));
        expect(onChange).toHaveBeenLastCalledWith(['a', 'b']);
        expect(screen.getByRole('listbox')).toBeVisible();
        expect(input).toHaveValue('');
    });

    it('does not create arbitrary options or emit changes on search', () => {
        const onChange = vi.fn();
        render(<Field onChange={onChange}/>);
        const input = screen.getByRole('combobox');
        fireEvent.focus(input);
        fireEvent.change(input, {target: {value: 'Unbekannt'}});
        expect(screen.getByText('Keine passenden Optionen gefunden')).toBeVisible();
        fireEvent.keyDown(input, {key: 'Enter'});
        fireEvent.blur(input);
        expect(onChange).not.toHaveBeenCalled();
    });

    it('keeps unresolved values visible and does not delete them on an options update', () => {
        const onChange = vi.fn();
        const {rerender} = render(<MultiChoiceCombobox label="Bereiche" value={['a']} options={options} onChange={onChange}/>);
        rerender(<MultiChoiceCombobox label="Bereiche" value={['a']} options={[]} onChange={onChange}/>);
        expect(screen.getByText('a')).toBeVisible();
        expect(onChange).not.toHaveBeenCalled();
    });

    it('supports keyboard selection and removal of the last selected value', () => {
        const onChange = vi.fn();
        render(<Field onChange={onChange}/>);
        const input = screen.getByRole('combobox');
        fireEvent.focus(input);
        fireEvent.change(input, {target: {value: 'woh'}});
        fireEvent.keyDown(input, {key: 'ArrowDown'});
        fireEvent.keyDown(input, {key: 'Enter'});
        expect(onChange).toHaveBeenLastCalledWith(['a', 'b']);
        fireEvent.keyDown(input, {key: 'Backspace'});
        expect(onChange).toHaveBeenLastCalledWith(['a']);
        fireEvent.keyDown(input, {key: 'Backspace'});
        expect(onChange).toHaveBeenLastCalledWith(null);
    });

    it('keeps a busy field focusable but prevents removing its selected values', () => {
        const onChange = vi.fn();
        render(<MultiChoiceCombobox label="Bereiche" value={['a']} options={options} onChange={onChange}
            busy required error="Bitte prüfen Sie die Auswahl."/>);
        const input = screen.getByRole('combobox', {name: /Bereiche/});
        expect(input).not.toBeDisabled();
        expect(input).toHaveAttribute('readonly');
        expect(input).toHaveAttribute('aria-required', 'true');
        expect(input).toHaveAccessibleDescription('Bitte prüfen Sie die Auswahl.');
        fireEvent.focus(input);
        fireEvent.keyDown(input, {key: 'Backspace'});
        expect(onChange).not.toHaveBeenCalled();
    });
});
