import {useState} from 'react';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {PhoneNumberFieldComponent} from './phone-number-field-component';

function renderControlledPhoneNumber(initialValue: string | null | undefined, emptyValue: string | null = null) {
    const onChange = vi.fn();
    const onBlur = vi.fn();

    function ControlledPhoneNumber() {
        const [value, setValue] = useState(initialValue);
        return (
            <>
                <PhoneNumberFieldComponent
                    label="Telefonnummer"
                    value={value}
                    onChange={(nextValue) => {
                        onChange(nextValue);
                        setValue(nextValue ?? emptyValue);
                    }}
                    onBlur={onBlur}
                />
                <button type="button">Outside</button>
            </>
        );
    }

    render(<ControlledPhoneNumber />);
    return {onChange, onBlur, input: screen.getByRole('textbox', {name: 'Telefonnummer – optional'})};
}

async function selectAustria(user: ReturnType<typeof userEvent.setup>) {
    await user.click(screen.getByRole('button', {name: /Open flags menu/}));
    await user.click(screen.getByRole('option', {name: 'Österreich Österreich +43'}));
}

describe('PhoneNumberFieldComponent', () => {
    it.each([null, '', undefined])('keeps the selected calling code for an empty value of %s', async (initialValue) => {
        const user = userEvent.setup();
        const {input, onChange, onBlur} = renderControlledPhoneNumber(initialValue, initialValue ?? null);

        await selectAustria(user);
        await user.click(screen.getByRole('button', {name: 'Outside'}));

        expect(screen.getByRole('button', {name: /Austria, Open flags menu/})).toBeInTheDocument();
        expect(input).toHaveValue('');
        expect(onChange).toHaveBeenLastCalledWith(null);
        expect(onBlur).toHaveBeenLastCalledWith(null);

        await user.type(input, '6641234567');
        await user.click(screen.getByRole('button', {name: 'Outside'}));
        expect(onChange).toHaveBeenLastCalledWith('+436641234567');
        expect(onBlur).toHaveBeenLastCalledWith('+436641234567');
    });

    it.each(['loaded', 'typed'])('preserves the national number and new calling code for a %s number', async (source) => {
        const user = userEvent.setup();
        const {input, onChange, onBlur} = renderControlledPhoneNumber(source === 'loaded' ? '+4930123456' : null);
        if (source === 'typed') {
            await user.type(input, '30123456');
        }

        await selectAustria(user);
        await user.click(screen.getByRole('button', {name: 'Outside'}));

        expect(screen.getByRole('button', {name: /Austria, Open flags menu/})).toBeInTheDocument();
        expect(input).toHaveValue('30123456');
        expect(onChange).toHaveBeenLastCalledWith('+4330123456');
        expect(onBlur).toHaveBeenLastCalledWith('+4330123456');
        expect(onChange).not.toHaveBeenCalledWith(null);
    });

    it.each([null, ''])('keeps the country when clearing a number and the parent returns %s', async (emptyValue) => {
        const user = userEvent.setup();
        const {input, onChange, onBlur} = renderControlledPhoneNumber('+436641234567', emptyValue);

        await user.clear(input);
        await user.click(screen.getByRole('button', {name: 'Outside'}));

        expect(screen.getByRole('button', {name: /Austria, Open flags menu/})).toBeInTheDocument();
        expect(input).toHaveValue('');
        expect(onChange).toHaveBeenLastCalledWith(null);
        expect(onBlur).toHaveBeenLastCalledWith(null);
    });

    it('applies externally replaced and reset values without stale blur data', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        const onBlur = vi.fn();
        const field = (value: string | null) => (
            <PhoneNumberFieldComponent label="Telefonnummer" value={value} onChange={onChange} onBlur={onBlur} />
        );
        const {rerender} = render(field('+4930123456'));
        const input = screen.getByRole('textbox', {name: 'Telefonnummer – optional'});
        await user.click(input);
        await user.tab();
        expect(onBlur).toHaveBeenLastCalledWith('+4930123456');

        rerender(field('+436641234567'));
        await user.click(input);
        await user.tab();
        expect(input).toHaveValue('6641234567');
        expect(onBlur).toHaveBeenLastCalledWith('+436641234567');

        rerender(field('+41'));
        await user.click(input);
        await user.tab();
        expect(screen.getByRole('button', {name: /Switzerland, Open flags menu/})).toBeInTheDocument();
        expect(input).toHaveValue('');
        expect(onBlur).toHaveBeenLastCalledWith(null);

        rerender(field(null));
        await user.click(input);
        await user.tab();
        expect(screen.getByRole('button', {name: /Germany, Open flags menu/})).toBeInTheDocument();
        expect(input).toHaveValue('');
        expect(onBlur).toHaveBeenLastCalledWith(null);
        expect(onChange).not.toHaveBeenCalled();
    });

    it('normalizes the current number on blur', async () => {
        const user = userEvent.setup();
        const {onChange, onBlur, input} = renderControlledPhoneNumber('+49 (0) 30 123456');
        await user.click(input);
        await user.click(screen.getByRole('button', {name: 'Outside'}));

        expect(onChange).toHaveBeenLastCalledWith('+4930123456');
        expect(onBlur).toHaveBeenLastCalledWith('+4930123456');
    });

    it('preserves incomplete input for form validation', async () => {
        const user = userEvent.setup();
        const {onChange, onBlur, input} = renderControlledPhoneNumber('+43');
        await user.type(input, '6');
        await user.click(screen.getByRole('button', {name: 'Outside'}));

        expect(input).toHaveValue('6');
        expect(onChange).toHaveBeenLastCalledWith('+436');
        expect(onBlur).toHaveBeenLastCalledWith('+436');
    });

    it('uses the external field label and helper for the native telephone input', () => {
        render(
            <PhoneNumberFieldComponent
                label="Telefonnummer"
                value="+4915112345678"
                onChange={vi.fn()}
                hint="Geben Sie eine international erreichbare Nummer an."
            />,
        );

        const input = screen.getByRole('textbox', {name: 'Telefonnummer – optional'});
        expect(input).toHaveAttribute('type', 'tel');
        expect(input).toHaveAccessibleDescription('Geben Sie eine international erreichbare Nummer an.');
    });

    it('exposes validation and busy state without a floating label', () => {
        render(
            <PhoneNumberFieldComponent
                label="Rückrufnummer"
                value="+4915112345678"
                onChange={vi.fn()}
                error="Die Telefonnummer ist ungültig."
                required
                busy
            />,
        );

        const input = screen.getByRole('textbox', {name: 'Rückrufnummer'});
        expect(input).toHaveAttribute('aria-invalid', 'true');
        expect(input).toHaveAttribute('aria-readonly', 'true');
        expect(input).toHaveAttribute('aria-busy', 'true');
        expect(input).toHaveAccessibleDescription('Die Telefonnummer ist ungültig.');
        expect(screen.getAllByRole('alert')).toHaveLength(1);
    });
});
