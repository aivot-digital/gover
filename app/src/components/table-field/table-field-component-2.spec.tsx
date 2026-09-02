import {describe, expect, it, vi} from 'vitest';
import {fireEvent, render, screen} from '@testing-library/react';
import {TableFieldComponent2} from './table-field-component-2';

vi.mock('@mui/x-data-grid', () => ({
    DataGrid: ({processRowUpdate, rows}: {
        processRowUpdate?: (newRow: Record<string, unknown>, oldRow: Record<string, unknown>) => void;
        rows: Array<Record<string, unknown>>;
    }) => (
        <button
            type="button"
            onClick={() => processRowUpdate?.(
                {...rows[0], name: 'Aktualisiert'},
                rows[0],
            )}
        >
            Zeile bearbeiten
        </button>
    ),
}));

describe('TableFieldComponent2', () => {
    it('does not leak DataGrid generated row IDs into authored values', () => {
        const onChange = vi.fn();

        render(
            <TableFieldComponent2
                label="Einträge"
                fields={[{key: 'name', label: 'Name', required: true}]}
                createDefaultRow={() => ({name: ''})}
                value={[{name: 'Ursprünglich'}]}
                onChange={onChange}
            />,
        );

        fireEvent.click(screen.getByRole('button', {name: 'Zeile bearbeiten'}));

        expect(onChange).toHaveBeenCalledWith([{name: 'Aktualisiert'}]);
    });

    it('updates domain rows by their ID without treating the ID as an array index', () => {
        const onChange = vi.fn();

        render(
            <TableFieldComponent2
                label="Einträge"
                fields={[{key: 'name', label: 'Name', required: true}]}
                createDefaultRow={() => ({id: '', name: ''})}
                value={[{id: 'record-42', name: 'Ursprünglich'}]}
                onChange={onChange}
                rowsHaveIds
            />,
        );

        fireEvent.click(screen.getByRole('button', {name: 'Zeile bearbeiten'}));

        expect(onChange).toHaveBeenCalledWith([{
            id: 'record-42',
            name: 'Aktualisiert',
        }]);
    });
});
