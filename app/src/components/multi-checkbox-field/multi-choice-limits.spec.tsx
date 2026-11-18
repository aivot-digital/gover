import {useState} from 'react';
import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {MultiCheckboxComponent} from './multi-checkbox-component';
import {MultiChoiceCombobox} from './multi-choice-combobox';

const options = [{value: 'a', label: 'Bildung'}, {value: 'b', label: 'Wohnen'}, {value: 'c', label: 'Gesundheit'}];

describe.each([MultiCheckboxComponent, MultiChoiceCombobox])('selection limits ($name)', Component => {
    const searchable = Component === MultiChoiceCombobox;
    const choice = (name: string) => screen.getByRole(searchable ? 'option' : 'checkbox', {name});
    function Field({initial = ['a'], maxItems = 2, onChange = vi.fn()}) {
        const [value, setValue] = useState<string[] | null>(initial);
        return <Component label="Bereiche" options={options} value={value} maxItems={maxItems}
            onChange={next => {setValue(next); onChange(next);}}/>;
    }
    function open() {
        if (searchable) {
            const input = screen.getByRole('combobox');
            fireEvent.focus(input);
            fireEvent.keyDown(input, {key: 'ArrowDown'});
        }
    }
    function expectDisabled(name: string, disabled: boolean) {
        if (searchable) expect(choice(name)).toHaveAttribute('aria-disabled', String(disabled));
        else if (disabled) expect(choice(name)).toBeDisabled();
        else expect(choice(name)).not.toBeDisabled();
    }

    it('blocks additions at the maximum and enables them after removal', () => {
        const onChange = vi.fn();
        render(<Field onChange={onChange}/>);
        open();
        fireEvent.click(choice('Wohnen'));
        expect(onChange).toHaveBeenLastCalledWith(['a', 'b']);
        expectDisabled('Gesundheit', true);
        expectDisabled('Bildung', false);
        fireEvent.click(choice('Gesundheit'));
        expect(onChange).toHaveBeenCalledTimes(1);
        fireEvent.click(choice('Bildung'));
        expect(onChange).toHaveBeenLastCalledWith(['b']);
        expectDisabled('Gesundheit', false);
        fireEvent.click(choice('Gesundheit'));
        expect(onChange).toHaveBeenLastCalledWith(['b', 'c']);
    });

    it('preserves over-limit values and allows removing them one at a time', () => {
        const onChange = vi.fn();
        render(<Field initial={['a', 'b', 'c']} maxItems={1} onChange={onChange}/>);
        expect(onChange).not.toHaveBeenCalled();
        open();
        fireEvent.click(choice('Bildung'));
        expect(onChange).toHaveBeenLastCalledWith(['b', 'c']);
        fireEvent.click(choice('Wohnen'));
        expect(onChange).toHaveBeenLastCalledWith(['c']);
        fireEvent.click(choice('Gesundheit'));
        expect(onChange).toHaveBeenLastCalledWith(null);
    });

    it('treats zero as unlimited', () => {
        render(<Field initial={['a', 'b']} maxItems={0}/>);
        open();
        expectDisabled('Gesundheit', false);
    });
});

it('prevents keyboard selection of disabled search results at the limit', () => {
    const onChange = vi.fn();
    render(<MultiChoiceCombobox label="Bereiche" options={options} value={['a']} maxItems={1} onChange={onChange}/>);
    const input = screen.getByRole('combobox');
    fireEvent.focus(input);
    fireEvent.change(input, {target: {value: 'Wohnen'}});
    fireEvent.keyDown(input, {key: 'ArrowDown'});
    fireEvent.keyDown(input, {key: 'Enter'});
    expect(onChange).not.toHaveBeenCalled();
    expect(screen.getByRole('option', {name: 'Wohnen'})).toHaveAttribute('aria-disabled', 'true');
    fireEvent.change(input, {target: {value: ''}});
    fireEvent.keyDown(input, {key: 'Backspace'});
    expect(onChange).toHaveBeenLastCalledWith(null);
});
