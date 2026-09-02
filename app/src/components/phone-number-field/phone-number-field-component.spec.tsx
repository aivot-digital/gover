import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {PhoneNumberFieldComponent} from './phone-number-field-component';

describe('PhoneNumberFieldComponent', () => {
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
