import {act, fireEvent, render, screen} from '@testing-library/react';
import {useState} from 'react';
import {afterEach, describe, expect, it, vi} from 'vitest';
import {AutocompleteTextField, type AutocompleteTextFieldProps} from './autocomplete-text-field';

function Field(props: Partial<AutocompleteTextFieldProps>) {
    const [value, setValue] = useState<string | null>(props.value ?? null);
    return <AutocompleteTextField label="Ort" suggestions={['Berlin', 'Hamburg']} {...props} value={value}
        onChange={(next) => { setValue(next); props.onChange?.(next); }}/>
}

describe('text suggestions', () => {
    afterEach(() => vi.useRealTimers());

    it('commits a selected suggestion and cancels an older pending draft', () => {
        vi.useFakeTimers();
        const onChange = vi.fn();
        render(<Field debounce={1000} onChange={onChange}/>);
        const input = screen.getByRole('combobox');
        fireEvent.change(input, {target: {value: 'Ber'}});
        fireEvent.click(screen.getByRole('option', {name: 'Berlin'}));
        act(() => vi.advanceTimersByTime(1500));
        fireEvent.blur(input);
        expect(input).toHaveValue('Berlin');
        expect(onChange).toHaveBeenLastCalledWith('Berlin');
        expect(onChange).not.toHaveBeenCalledWith('Ber');
    });

    it('preserves custom text on blur and clears explicitly', () => {
        const onChange = vi.fn();
        render(<Field onChange={onChange}/>);
        const input = screen.getByRole('combobox');
        fireEvent.change(input, {target: {value: 'Leipzig'}});
        fireEvent.blur(input);
        expect(onChange).toHaveBeenLastCalledWith('Leipzig');
        fireEvent.change(input, {target: {value: ''}});
        fireEvent.blur(input);
        expect(onChange).toHaveBeenLastCalledWith(null);
    });

    it('does not restore a pending draft after using the clear button', () => {
        vi.useFakeTimers();
        const onChange = vi.fn();
        render(<Field value="Hamburg" debounce={1000} onChange={onChange}/>);
        const input = screen.getByRole('combobox');
        fireEvent.focus(input);
        fireEvent.change(input, {target: {value: 'Ber'}});
        fireEvent.click(screen.getByRole('button', {name: 'Clear'}));
        act(() => vi.advanceTimersByTime(1500));
        fireEvent.blur(input);
        expect(input).toHaveValue('');
        expect(onChange).toHaveBeenCalledExactlyOnceWith(null);
    });

    it.each(['disabled', 'readonly', 'busy'] as const)('cancels pending changes when %s becomes active', (state) => {
        vi.useFakeTimers();
        const onChange = vi.fn();
        const props = {label: 'Ort', suggestions: ['Berlin'], value: 'Hamburg', debounce: 1000, onChange};
        const {rerender} = render(<AutocompleteTextField {...props}/>);
        fireEvent.change(screen.getByRole('combobox'), {target: {value: 'Ber'}});
        rerender(<AutocompleteTextField {...props} {...{[state]: true}}/>);
        act(() => vi.advanceTimersByTime(1500));
        fireEvent.blur(screen.getByRole('combobox'));
        expect(onChange).not.toHaveBeenCalled();
    });

    it('does not publish an unfinished draft after unmounting', () => {
        vi.useFakeTimers();
        const onChange = vi.fn();
        const {unmount} = render(<Field debounce={1000} onChange={onChange}/>);
        fireEvent.change(screen.getByRole('combobox'), {target: {value: 'Ber'}});
        unmount();
        act(() => vi.advanceTimersByTime(1500));
        expect(onChange).not.toHaveBeenCalled();
    });

    it('keeps technical ids distinct from labels and searches secondary descriptions', () => {
        const onChange = vi.fn();
        render(<Field onChange={onChange} suggestions={[{id: 'customer.city', label: 'Ort', subLabel: 'Anschrift'}]}/>);
        const input = screen.getByRole('combobox');
        fireEvent.change(input, {target: {value: 'anschrift'}});
        fireEvent.click(screen.getByRole('option', {name: /Ort Anschrift/}));
        fireEvent.blur(input);
        expect(input).toHaveValue('customer.city');
        expect(onChange).toHaveBeenLastCalledWith('customer.city');
    });

    it('does not replace a free input with a merely highlighted suggestion', () => {
        const onChange = vi.fn();
        render(<Field onChange={onChange}/>);
        const input = screen.getByRole('combobox');
        fireEvent.change(input, {target: {value: 'Ber'}});
        fireEvent.keyDown(input, {key: 'ArrowDown'});
        fireEvent.blur(input);
        expect(onChange).toHaveBeenLastCalledWith('Ber');
    });

    it('accepts an external value update instead of flushing a stale draft', () => {
        vi.useFakeTimers();
        const onChange = vi.fn();
        const {rerender} = render(<AutocompleteTextField label="Ort" suggestions={['Berlin']} value="" debounce={1000} onChange={onChange}/>);
        fireEvent.change(screen.getByRole('combobox'), {target: {value: 'Ber'}});
        rerender(<AutocompleteTextField label="Ort" suggestions={['Berlin']} value="Hamburg" debounce={1000} onChange={onChange}/>);
        act(() => vi.advanceTimersByTime(1500));
        expect(screen.getByRole('combobox')).toHaveValue('Hamburg');
        expect(onChange).not.toHaveBeenCalled();
    });

    it('buffers a selected suggestion until blur when configured', () => {
        const onChange = vi.fn();
        const onBlur = vi.fn();
        render(<Field bufferInputUntilBlur onChange={onChange} onBlur={onBlur}/>);
        const input = screen.getByRole('combobox');
        fireEvent.change(input, {target: {value: 'Ber'}});
        fireEvent.keyDown(input, {key: 'ArrowDown'});
        fireEvent.keyDown(input, {key: 'Enter'});
        expect(input).toHaveValue('Berlin');
        expect(onChange).not.toHaveBeenCalled();
        fireEvent.blur(input);
        expect(onChange).toHaveBeenCalledExactlyOnceWith('Berlin');
        expect(onBlur).toHaveBeenCalledWith('Berlin');
    });

    it('cancels pending normalized text while composition is active', () => {
        vi.useFakeTimers();
        const onChange = vi.fn();
        render(<Field debounce={1000} onChange={onChange}
            normalizeInput={(value) => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '')}/>);
        const input = screen.getByRole('combobox');
        fireEvent.change(input, {target: {value: 'caf'}});
        fireEvent.compositionStart(input);
        fireEvent.input(input, {target: {value: 'caf´'}, isComposing: true});
        act(() => vi.advanceTimersByTime(1500));
        expect(input).toHaveValue('caf´');
        expect(onChange).not.toHaveBeenCalled();
        fireEvent.compositionEnd(input, {target: {value: 'café'}});
        fireEvent.blur(input);
        expect(input).toHaveValue('cafe');
        expect(onChange).toHaveBeenCalledExactlyOnceWith('cafe');
    });
});
