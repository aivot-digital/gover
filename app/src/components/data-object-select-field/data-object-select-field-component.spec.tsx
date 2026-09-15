import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {DataObjectSelectFieldComponent} from './data-object-select-field-component';

const options = [
    {
        key: 'person-1',
        value: 'person-1',
        label: 'Max Mustermann',
        subLabel: 'persons · person-1',
    },
    {
        key: 'person-2',
        value: 'person-2',
        label: 'Erika Musterfrau',
        subLabel: 'persons · person-2',
    },
];

describe('DataObjectSelectFieldComponent', () => {
    it('exposes its external label and helper through the native combobox', () => {
        render(
            <DataObjectSelectFieldComponent
                label="Datenobjekt"
                value="person-1"
                onChange={vi.fn()}
                dataModelKey="persons"
                options={options}
                hint="Referenziert einen Datensatz aus dem gewählten Modell."
                required
            />,
        );

        const input = screen.getByRole('combobox', {name: 'Datenobjekt'});
        expect(input).toHaveAttribute('aria-autocomplete', 'list');
        expect(input).toHaveAccessibleDescription('Referenziert einen Datensatz aus dem gewählten Modell.');
    });

    it('reports errors once and keeps the optional suffix out of required labels', () => {
        render(
            <DataObjectSelectFieldComponent
                label="Datenobjekt"
                value={null}
                onChange={vi.fn()}
                dataModelKey="persons"
                options={options}
                error="Wählen Sie ein Datenobjekt."
                required
            />,
        );

        const input = screen.getByRole('combobox', {name: 'Datenobjekt'});
        expect(input).toHaveAttribute('aria-invalid', 'true');
        expect(input).toHaveAccessibleDescription('Wählen Sie ein Datenobjekt.');
        expect(screen.getAllByRole('alert')).toHaveLength(1);
    });

    it('passes the selected data-object id to consumers', () => {
        const onChange = vi.fn();

        render(
            <DataObjectSelectFieldComponent
                label="Datenobjekt"
                value={null}
                onChange={onChange}
                dataModelKey="persons"
                options={options}
            />,
        );

        const input = screen.getByRole('combobox', {name: 'Datenobjekt – optional'});
        fireEvent.mouseDown(input);
        fireEvent.click(screen.getByRole('option', {name: /Erika Musterfrau/}));

        expect(onChange).toHaveBeenCalledWith('person-2');
    });
});
