import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {ChipInputFieldComponent} from './chip-input-field-component';

vi.mock('../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => vi.fn(),
}));

describe('ChipInputFieldComponent', () => {
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
    });
});
