import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {CheckboxFieldComponent} from './checkbox-field-component';

describe('CheckboxFieldComponent', () => {
    it('uses the directly clickable label as its accessible name', () => {
        const onChange = vi.fn();

        render(
            <CheckboxFieldComponent
                label="Einwilligung erteilt"
                value={false}
                onChange={onChange}
                hint="Die Einwilligung kann später widerrufen werden."
                margin="none"
            />,
        );

        const checkbox = screen.getByRole('checkbox', {name: 'Einwilligung erteilt'});
        expect(checkbox).toHaveAccessibleDescription('Die Einwilligung kann später widerrufen werden.');
        expect(screen.queryByText('optional')).not.toBeInTheDocument();

        fireEvent.click(screen.getByText('Einwilligung erteilt'));
        expect(onChange).toHaveBeenCalledWith(true);
    });

    it('keeps an optional label action separate from the input name', () => {
        render(
            <CheckboxFieldComponent
                label="Automatische Erinnerung"
                value
                onChange={vi.fn()}
                showOptionalIndicator
                labelAction={(field) => (
                    <button type="button" aria-controls={field.controlId}>
                        Eingabemodus wählen
                    </button>
                )}
                margin="none"
            />,
        );

        const checkbox = screen.getByRole('checkbox', {name: 'Automatische Erinnerung – optional'});
        const action = screen.getByRole('button', {name: 'Eingabemodus wählen'});
        expect(action).toHaveAttribute('aria-controls', checkbox.id);
        expect(action.closest('[data-form-field-label-action]')).not.toBeNull();
        expect(checkbox).not.toHaveAccessibleName(/Eingabemodus/);
        expect(Array.from(checkbox.closest('[data-form-field]')!.querySelectorAll('button, input')).slice(0, 2))
            .toEqual([action, checkbox]);
    });

    it('exposes required and error semantics on the native checkbox', () => {
        const {container} = render(
            <CheckboxFieldComponent
                label="Datenschutzbestätigung"
                value={false}
                onChange={vi.fn()}
                required
                error="Die Bestätigung ist erforderlich."
                margin="none"
            />,
        );

        const checkbox = screen.getByRole('checkbox', {name: 'Datenschutzbestätigung'});
        expect(checkbox).toBeRequired();
        expect(checkbox).toHaveAttribute('aria-required', 'true');
        expect(checkbox).toHaveAttribute('aria-invalid', 'true');
        expect(checkbox).toHaveAccessibleDescription('Die Bestätigung ist erforderlich.');
        expect(screen.getByRole('alert')).toHaveTextContent('Die Bestätigung ist erforderlich.');
        expect(container.querySelector('.MuiFormControlLabel-asterisk')).not.toBeInTheDocument();
    });

    it('blocks a busy switch and exposes its state', () => {
        const onChange = vi.fn();

        render(
            <CheckboxFieldComponent
                label="Benachrichtigungen"
                value
                onChange={onChange}
                variant="switch"
                busy
                margin="none"
            />,
        );

        const control = screen.getByRole('switch', {name: 'Benachrichtigungen'});
        expect(control).toBeDisabled();
        expect(control).toHaveAttribute('aria-busy', 'true');

        fireEvent.click(control);
        expect(onChange).not.toHaveBeenCalled();
    });
});
