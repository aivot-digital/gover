import {useState} from 'react';
import {describe, expect, it, vi} from 'vitest';
import {fireEvent, render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ThemeProvider} from '@mui/material';
import {DurationFieldComponent, DurationFieldComponentProps} from './duration-field-component';
import {DurationLargestUnit, DurationPrecision} from '../../models/elements/form/input/duration-field-element';
import {BaseTheme} from '../../theming/base-theme';

function Field(props: Partial<DurationFieldComponentProps>) {
    const [value, setValue] = useState(props.value);
    return <DurationFieldComponent label="Aufwand" {...props} value={value} onChange={next => {
        setValue(next);
        props.onChange?.(next);
    }} />;
}

describe('DurationFieldComponent', () => {
    it.each([DurationPrecision.Minute, DurationPrecision.Second])('normalizes hours into days with %s precision', async precision => {
        const onChange = vi.fn();
        render(<><Field largestUnit={DurationLargestUnit.Day} precision={precision} onChange={onChange}/><button>Weiter</button></>);
        const user = userEvent.setup();
        await user.type(screen.getByRole('textbox', {name: 'Tage'}), '1');
        await user.type(screen.getByRole('textbox', {name: 'Stunden'}), '27');
        await user.type(screen.getByRole('textbox', {name: 'Minuten'}), '90');
        if (precision === DurationPrecision.Second) {
            await user.type(screen.getByRole('textbox', {name: 'Sekunden'}), '75');
        }
        expect(onChange).toHaveBeenLastCalledWith(189000 + (precision === DurationPrecision.Second ? 75 : 0));
        expect(screen.getByRole('textbox', {name: 'Stunden'})).toHaveValue('27');
        await user.click(screen.getByRole('button', {name: 'Weiter'}));
        expect(screen.getByRole('textbox', {name: 'Tage'})).toHaveValue('2');
        expect(screen.getByRole('textbox', {name: 'Stunden'})).toHaveValue('4');
        expect(screen.getByRole('textbox', {name: 'Minuten'})).toHaveValue(precision === DurationPrecision.Second ? '31' : '30');
        if (precision === DurationPrecision.Second) {
            expect(screen.getByRole('textbox', {name: 'Sekunden'})).toHaveValue('15');
        }
    });

    it('changes the largest unit without emitting or losing seconds', () => {
        const onChange = vi.fn();
        const props = {label: 'Aufwand', value: 99015, precision: DurationPrecision.Second, onChange};
        const {rerender} = render(<DurationFieldComponent {...props}/>);
        expect(screen.queryByRole('textbox', {name: 'Tage'})).not.toBeInTheDocument();
        expect(screen.getByRole('textbox', {name: 'Stunden'})).toHaveValue('27');
        rerender(<DurationFieldComponent {...props} largestUnit={DurationLargestUnit.Day}/>);
        expect(screen.getByRole('textbox', {name: 'Tage'})).toHaveValue('1');
        expect(screen.getByRole('textbox', {name: 'Stunden'})).toHaveValue('3');
        expect(screen.getByRole('textbox', {name: 'Sekunden'})).toHaveValue('15');
        rerender(<DurationFieldComponent {...props} largestUnit={DurationLargestUnit.Hour}/>);
        expect(screen.getByRole('textbox', {name: 'Stunden'})).toHaveValue('27');
        expect(onChange).not.toHaveBeenCalled();
    });

    it('distinguishes empty days from zero and rejects unsafe day counts', () => {
        const onChange = vi.fn();
        render(<Field largestUnit={DurationLargestUnit.Day} onChange={onChange}/>);
        const days = screen.getByRole('textbox', {name: 'Tage'});
        fireEvent.change(days, {target: {value: '0'}});
        expect(onChange).toHaveBeenLastCalledWith(0);
        fireEvent.change(days, {target: {value: ''}});
        expect(onChange).toHaveBeenLastCalledWith(null);
        onChange.mockClear();
        fireEvent.change(days, {target: {value: '999999999999'}});
        expect(days).toHaveValue('');
        expect(onChange).not.toHaveBeenCalled();
    });

    it('leaves label spacing to FormField when using the application theme', () => {
        render(
            <ThemeProvider theme={BaseTheme}>
                <Field precision={DurationPrecision.Second}/>
            </ThemeProvider>,
        );
        for (const name of ['Stunden', 'Minuten', 'Sekunden']) {
            const control = screen.getByRole('textbox', {name}).closest('.MuiFormControl-root');
            expect(control).toHaveStyle({marginTop: '0px', marginBottom: '0px'});
        }
    });

    it('stores total seconds and normalizes only when leaving the whole group', async () => {
        const onChange = vi.fn();
        const onBlur = vi.fn();
        render(<><Field onChange={onChange} onBlur={onBlur}/><button>Weiter</button></>);
        const user = userEvent.setup();
        const hours = screen.getByRole('textbox', {name: 'Stunden'});
        const minutes = screen.getByRole('textbox', {name: 'Minuten'});
        await user.click(minutes);
        await user.type(minutes, '90');
        expect(onChange).toHaveBeenLastCalledWith(5400);
        await user.click(hours);
        expect(minutes).toHaveValue('90');
        expect(onBlur).not.toHaveBeenCalled();
        await user.click(screen.getByRole('button', {name: 'Weiter'}));
        expect(hours).toHaveValue('1');
        expect(minutes).toHaveValue('30');
        expect(onBlur).toHaveBeenLastCalledWith(5400);
    });

    it('distinguishes an empty field from explicit zero', () => {
        const onChange = vi.fn();
        render(<Field onChange={onChange}/>);
        const minutes = screen.getByRole('textbox', {name: 'Minuten'});
        expect(minutes).toHaveValue('');
        fireEvent.change(minutes, {target: {value: '0'}});
        expect(onChange).toHaveBeenLastCalledWith(0);
        fireEvent.change(minutes, {target: {value: ''}});
        expect(onChange).toHaveBeenLastCalledWith(null);
    });

    it('supports more than 24 hours and optional seconds', () => {
        const onChange = vi.fn();
        render(<Field precision={DurationPrecision.Second} onChange={onChange}/>);
        fireEvent.change(screen.getByRole('textbox', {name: 'Stunden'}), {target: {value: '27'}});
        fireEvent.change(screen.getByRole('textbox', {name: 'Minuten'}), {target: {value: '30'}});
        fireEvent.change(screen.getByRole('textbox', {name: 'Sekunden'}), {target: {value: '15'}});
        expect(onChange).toHaveBeenLastCalledWith(99015);
    });

    it('does not round, coerce or emit invalid typed parts', () => {
        const onChange = vi.fn();
        render(<Field onChange={onChange}/>);
        for (const value of ['-1', '1.5', '1,5', 'abc', '99999999999999999999999999999']) {
            fireEvent.change(screen.getByRole('textbox', {name: 'Stunden'}), {target: {value}});
        }
        expect(onChange).not.toHaveBeenCalled();
    });

    it('exposes shared label, hint, required state and bound errors accessibly', () => {
        const {rerender} = render(<DurationFieldComponent label="Aufwand" value={60} onChange={() => {}}
            required hint="Mit Vorbereitung"/>);
        expect(screen.getByRole('group', {name: /Aufwand/})).toHaveAttribute('aria-required', 'true');
        expect(screen.getByRole('textbox', {name: 'Minuten'})).toHaveAccessibleDescription('Mit Vorbereitung');
        rerender(<DurationFieldComponent label="Aufwand" value={60} onChange={() => {}} minDuration={120}/>);
        expect(screen.getByRole('textbox', {name: 'Minuten'})).toHaveAttribute('aria-invalid', 'true');
        expect(screen.getByRole('textbox', {name: 'Minuten'})).toHaveAccessibleDescription('Die Zeitdauer muss mindestens 2 Minuten betragen.');
    });

    it.each(['disabled', 'readOnly', 'busy'] as const)('protects %s fields from edits', property => {
        const onChange = vi.fn();
        render(<DurationFieldComponent label="Aufwand" value={9000} onChange={onChange} {...{[property]: true}}/>);
        const hours = screen.getByRole('textbox', {name: 'Stunden'});
        if (property === 'disabled') expect(hours).toBeDisabled();
        else expect(hours).toHaveAttribute('readonly');
        if (property === 'busy') expect(screen.getByRole('group', {name: /Aufwand/})).toHaveAttribute('aria-busy', 'true');
        fireEvent.change(hours, {target: {value: '5'}});
        expect(onChange).not.toHaveBeenCalled();
    });

    it('hydrates external changes without silently rounding seconds in minute mode', () => {
        const onChange = vi.fn();
        const {rerender} = render(<DurationFieldComponent label="Aufwand" value={9000} onChange={onChange}/>);
        rerender(<DurationFieldComponent label="Aufwand" value={75} onChange={onChange}/>);
        expect(screen.getByRole('textbox', {name: 'Minuten'})).toHaveValue('1');
        expect(screen.getByRole('alert')).toHaveTextContent('ganzen Minuten');
        expect(onChange).not.toHaveBeenCalled();
        rerender(<DurationFieldComponent label="Aufwand" value={null} onChange={onChange}/>);
        expect(screen.getByRole('textbox', {name: 'Minuten'})).toHaveValue('');
    });
});
