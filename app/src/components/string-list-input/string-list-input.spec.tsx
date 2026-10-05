import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {StringListInput} from './string-list-input';

vi.mock('@mui/x-data-grid', () => ({
    DataGrid: (props: {
        'aria-labelledby'?: string;
        'aria-describedby'?: string;
        'aria-invalid'?: boolean;
    }) => (
        <div
            role="grid"
            aria-labelledby={props['aria-labelledby']}
            aria-describedby={props['aria-describedby']}
            aria-invalid={props['aria-invalid']}
        />
    ),
}));

describe('StringListInput', () => {
    it('uses the shared field group in table and text mode', () => {
        render(
            <StringListInput
                label="Berechtigungen"
                value={['lesen']}
                onChange={vi.fn()}
                addLabel="Berechtigung hinzufügen"
                noItemsHint="Keine Berechtigungen hinterlegt"
                hint="Jeder Eintrag bezeichnet eine benötigte Berechtigung."
                allowEmpty
                labelAction={<button type="button">Feldoptionen</button>}
            />,
        );

        expect(screen.getByRole('grid', {name: 'Berechtigungen – optional'}))
            .toHaveAccessibleDescription('Jeder Eintrag bezeichnet eine benötigte Berechtigung.');
        expect(screen.getByRole('button', {name: 'Berechtigung hinzufügen'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Feldoptionen'})).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'Berechtigungen: Einträge als Text bearbeiten'}));

        expect(screen.getByTitle('Berechtigungen').closest('fieldset'))
            .toHaveAccessibleName('Berechtigungen – optional');
        expect(screen.getByRole('textbox', {name: 'Einträge'}))
            .toHaveAccessibleDescription('Jeder Eintrag bezeichnet eine benötigte Berechtigung.');
        expect(screen.getByRole('button', {name: 'Tabellenansicht'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Feldoptionen'})).toBeInTheDocument();
    });
});
