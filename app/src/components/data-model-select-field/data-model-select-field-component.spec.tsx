import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {DataModelSelectFieldComponent} from './data-model-select-field-component';

const options = [
    {
        key: 'persons',
        value: 'persons',
        label: 'Personen',
        subLabel: 'persons',
    },
    {
        key: 'organizations',
        value: 'organizations',
        label: 'Organisationen',
        subLabel: 'organizations',
    },
];

describe('DataModelSelectFieldComponent', () => {
    it('combines the external label with Autocomplete semantics', () => {
        render(
            <DataModelSelectFieldComponent
                label="Datenmodell"
                value="persons"
                onChange={vi.fn()}
                options={options}
                hint="Bestimmt die verfügbaren Datenobjekte."
            />,
        );

        const input = screen.getByRole('combobox', {name: 'Datenmodell – optional'});
        expect(input).toHaveAttribute('aria-autocomplete', 'list');
        expect(input).toHaveAttribute('aria-expanded', 'false');
        expect(input).toHaveAccessibleDescription('Bestimmt die verfügbaren Datenobjekte.');
    });

    it('reports one externally associated error message', () => {
        render(
            <DataModelSelectFieldComponent
                label="Datenmodell"
                value={null}
                onChange={vi.fn()}
                options={options}
                error="Wählen Sie ein Datenmodell."
                required
            />,
        );

        const input = screen.getByRole('combobox', {name: 'Datenmodell'});
        expect(input).toHaveAttribute('aria-invalid', 'true');
        expect(input).toHaveAccessibleDescription('Wählen Sie ein Datenmodell.');
        expect(screen.getAllByRole('alert')).toHaveLength(1);
    });

    it('keeps the field non-interactive while a value is derived', () => {
        const onChange = vi.fn();

        render(
            <DataModelSelectFieldComponent
                label="Datenmodell"
                value="persons"
                onChange={onChange}
                options={options}
                busy
            />,
        );

        const input = screen.getByRole('combobox', {name: 'Datenmodell – optional'});
        expect(input).toHaveAttribute('aria-busy', 'true');
        expect(input).toHaveAttribute('aria-readonly', 'true');
        expect(onChange).not.toHaveBeenCalled();
    });

    it('passes the selected option value to consumers', () => {
        const onChange = vi.fn();

        render(
            <DataModelSelectFieldComponent
                label="Datenmodell"
                value={null}
                onChange={onChange}
                options={options}
            />,
        );

        const input = screen.getByRole('combobox', {name: 'Datenmodell – optional'});
        fireEvent.mouseDown(input);
        fireEvent.click(screen.getByRole('option', {name: /Organisationen/}));

        expect(onChange).toHaveBeenCalledWith('organizations');
    });
});
