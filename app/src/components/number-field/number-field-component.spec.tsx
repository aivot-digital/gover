import {describe, expect, it, vi} from 'vitest';
import {render, screen} from '@testing-library/react';
import {NumberFieldComponent} from './number-field-component';

describe('NumberFieldComponent', () => {
    it('associates its external label, hint and required state with the input', () => {
        render(
            <NumberFieldComponent
                label="Betrag"
                value={12.5}
                onChange={vi.fn()}
                hint="Bitte in Euro angeben."
                required
            />,
        );

        const input = screen.getByRole('textbox', {name: 'Betrag'});
        expect(input).toHaveAccessibleDescription('Bitte in Euro angeben.');
        expect(input).toHaveAttribute('aria-required', 'true');
        expect(input).toHaveAttribute('inputmode', 'decimal');
        expect(input).not.toHaveAttribute('aria-labelledby');
        expect(input.closest('.MuiInputBase-root')).toHaveClass('MuiInputBase-sizeSmall');
    });

    it('exposes validation errors on the input', () => {
        render(
            <NumberFieldComponent
                label="Anzahl"
                value={2}
                onChange={vi.fn()}
                error="Der Wert ist zu klein."
            />,
        );

        const input = screen.getByRole('textbox', {name: 'Anzahl – optional'});
        expect(input).toHaveAttribute('aria-invalid', 'true');
        expect(input).toHaveAccessibleDescription('Der Wert ist zu klein.');
    });
});
