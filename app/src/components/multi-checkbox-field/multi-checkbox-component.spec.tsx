import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {MultiCheckboxComponent} from './multi-checkbox-component';

describe('MultiCheckboxComponent', () => {
    it('associates its options and validation with one native fieldset', () => {
        const onChange = vi.fn();

        render(
            <MultiCheckboxComponent
                label="Benachrichtigungen"
                value={['email']}
                onChange={onChange}
                options={[
                    {value: 'email', label: 'E-Mail'},
                    {value: 'inbox', label: 'Postfach'},
                ]}
                error="Wählen Sie mindestens zwei Kanäle."
                required
                margin="none"
            />,
        );

        const group = screen.getByRole('group', {name: 'Benachrichtigungen'});
        expect(group).toHaveAccessibleDescription('Wählen Sie mindestens zwei Kanäle.');
        expect(group).toHaveAttribute('aria-invalid', 'true');
        expect(screen.getByRole('alert')).toHaveTextContent('Wählen Sie mindestens zwei Kanäle.');
        expect(screen.getByRole('checkbox', {name: 'E-Mail'})).toBeChecked();

        fireEvent.click(screen.getByRole('checkbox', {name: 'Postfach'}));
        expect(onChange).toHaveBeenCalledWith(['email', 'inbox']);
    });

    it('disables its options while the group is busy', () => {
        const onChange = vi.fn();

        render(
            <MultiCheckboxComponent
                label="Ausgaben"
                value={['report']}
                onChange={onChange}
                options={[
                    {value: 'report', label: 'Bericht'},
                    {value: 'archive', label: 'Archiv'},
                ]}
                busy
            />,
        );

        const group = screen.getByRole('group', {name: 'Ausgaben – optional'});
        expect(group).toHaveAttribute('aria-busy', 'true');
        expect(group).toHaveAttribute('aria-disabled', 'true');
        expect(screen.getByRole('checkbox', {name: 'Archiv'})).toBeDisabled();

        fireEvent.click(screen.getByRole('checkbox', {name: 'Archiv'}));
        expect(onChange).not.toHaveBeenCalled();
    });
});
