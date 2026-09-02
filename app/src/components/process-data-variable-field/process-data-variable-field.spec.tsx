import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {ProcessDataVariableField} from './process-data-variable-field';

const options = [
    {
        label: 'Zählerstand',
        origin: 'Zähler aktualisieren',
        path: 'zaehler.aktuellerStand',
    },
    {
        label: 'Vorname',
        origin: 'Antrag eingereicht',
        path: 'antragsteller.vorname',
    },
];

describe('ProcessDataVariableField', () => {
    it('can clear the selected destination', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        render(
            <ProcessDataVariableField
                label="Vorgangsdatenvariable"
                value="zaehler.aktuellerStand"
                options={options}
                onChange={onChange}
            />,
        );

        await user.click(screen.getByLabelText('Vorgangsdatenvariable leeren'));

        expect(onChange).toHaveBeenCalledWith(null);
    });

    it('selects an existing writable process-data variable', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        render(
            <ProcessDataVariableField
                label="Vorgangsdatenvariable"
                value="zaehler.aktuellerStand"
                options={options}
                onChange={onChange}
            />,
        );

        await user.click(screen.getByLabelText('Vorgangsdatenvariable auswählen'));
        expect(screen.getByLabelText('Beschreibbare Vorgangsdatenvariablen')).toBeInTheDocument();
        const option = screen.getByLabelText(/Vorname/);
        await user.click(option);
        expect(option).toBeChecked();
        await user.click(screen.getByText('Übernehmen'));

        expect(onChange).toHaveBeenCalledWith('antragsteller.vorname');
    });

    it('creates a new process-data path from the dialog', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        render(
            <ProcessDataVariableField
                label="Vorgangsdatenvariable"
                value="zaehler.aktuellerStand"
                options={options}
                onChange={onChange}
            />,
        );

        await user.click(screen.getByLabelText('Vorgangsdatenvariable auswählen'));
        await user.type(
            screen.getByRole('textbox', {name: /^Vorgangsdatenpfad durchsuchen oder neu anlegen/}),
            'protokoll.neuerZaehler',
        );
        expect(screen.getByText('Übernehmen').closest('button')).toBeDisabled();
        await user.click(screen.getByText('Neue Vorgangsdatenvariable'));
        await user.click(screen.getByText('Übernehmen'));

        expect(onChange).toHaveBeenCalledWith('protokoll.neuerZaehler');
    });
});
