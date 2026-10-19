import {describe, expect, it, vi} from 'vitest';
import {fireEvent, render, screen} from '@testing-library/react';
import {TableFieldComponent2} from './table-field-component-2';

vi.mock('@mui/x-data-grid', () => ({
    DataGrid: ({onPaginationModelChange, paginationModel, processRowUpdate, rows}: {
        onPaginationModelChange: (model: {page: number; pageSize: number}) => void;
        paginationModel: {page: number; pageSize: number};
        processRowUpdate?: (newRow: Record<string, unknown>, oldRow: Record<string, unknown>) => void;
        rows: Array<Record<string, unknown>>;
    }) => {
        const pageStart = paginationModel.page * paginationModel.pageSize;
        const visibleRows = rows.slice(pageStart, pageStart + paginationModel.pageSize);

        return (
            <div>
                {visibleRows.map((row) => <span key={String(row.id)}>{String(row.name)}</span>)}
                <button
                    type="button"
                    disabled={processRowUpdate == null}
                    onClick={() => processRowUpdate?.(
                        {...rows[0], name: 'Aktualisiert'},
                        rows[0],
                    )}
                >
                    Zeile bearbeiten
                </button>
                <button
                    type="button"
                    disabled={pageStart + paginationModel.pageSize >= rows.length}
                    onClick={() => onPaginationModelChange({
                        ...paginationModel,
                        page: paginationModel.page + 1,
                    })}
                >
                    Nächste Seite
                </button>
            </div>
        );
    },
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

    it('keeps pagination available while preventing changes in read-only mode', () => {
        const onChange = vi.fn();
        const values = Array.from({length: 9}, (_, index) => ({name: `Eintrag ${index + 1}`}));

        render(
            <TableFieldComponent2
                label="Einträge"
                fields={[{key: 'name', label: 'Name', required: true}]}
                createDefaultRow={() => ({name: ''})}
                value={values}
                onChange={onChange}
                readOnly
            />,
        );

        const group = screen.getByRole('group', {name: /Einträge/});
        const nextPageButton = screen.getByRole('button', {name: 'Nächste Seite'});

        expect(group).toHaveAttribute('aria-readonly', 'true');
        expect(group).not.toBeDisabled();
        expect(screen.getByRole('button', {name: 'Eintrag hinzufügen'})).toBeDisabled();
        expect(screen.getByRole('button', {name: 'Ausgewählte Einträge löschen'})).toBeDisabled();
        expect(screen.getByRole('button', {name: 'Zeile bearbeiten'})).toBeDisabled();
        expect(nextPageButton).not.toBeDisabled();
        expect(screen.getByText('Eintrag 1')).toBeInTheDocument();
        expect(screen.queryByText('Eintrag 9')).not.toBeInTheDocument();

        fireEvent.click(nextPageButton);

        expect(screen.getByText('Eintrag 9')).toBeInTheDocument();
        expect(onChange).not.toHaveBeenCalled();
    });
});
