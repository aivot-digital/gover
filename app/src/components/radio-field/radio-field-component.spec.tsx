import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {RadioFieldComponent} from './radio-field-component';

describe('RadioFieldComponent', () => {
    it('exposes one labelled and described fieldset without a redundant radio group', () => {
        const onChange = vi.fn();

        render(
            <RadioFieldComponent
                label="Kontaktweg"
                value="email"
                onChange={onChange}
                options={[
                    {value: 'email', label: 'E-Mail'},
                    {value: 'post', label: 'Post'},
                ]}
                hint="Wählen Sie den bevorzugten Kontaktweg."
                margin="none"
            />,
        );

        const group = screen.getByRole('group', {name: 'Kontaktweg – optional'});
        expect(group).toHaveAccessibleDescription('Wählen Sie den bevorzugten Kontaktweg.');
        expect(screen.queryByRole('radiogroup')).not.toBeInTheDocument();
        expect(screen.getByRole('radio', {name: 'E-Mail'})).toBeChecked();

        fireEvent.click(screen.getByRole('radio', {name: 'Post'}));
        expect(onChange).toHaveBeenCalledWith('post');
    });

    it('blocks changes and exposes the busy group state', () => {
        const onChange = vi.fn();

        render(
            <RadioFieldComponent
                label="Freigabe"
                value="open"
                onChange={onChange}
                options={[
                    {value: 'open', label: 'Offen'},
                    {value: 'approved', label: 'Freigegeben'},
                ]}
                required
                busy
            />,
        );

        const group = screen.getByRole('group', {name: 'Freigabe'});
        expect(group).toHaveAttribute('aria-busy', 'true');
        expect(group).toHaveAttribute('aria-disabled', 'true');
        expect(screen.getByRole('radio', {name: 'Freigegeben'})).toBeDisabled();

        fireEvent.click(screen.getByRole('radio', {name: 'Freigegeben'}));
        expect(onChange).not.toHaveBeenCalled();
    });
});
