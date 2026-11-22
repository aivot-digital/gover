import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {OptionListInput} from './option-list-input';

const dataGrid = vi.hoisted(() => ({columns: [] as Array<{field: string; type?: string; valueOptions?: unknown}>}));

vi.mock('@mui/x-data-grid', () => ({
    DataGrid: (props: {
        'aria-labelledby'?: string;
        'aria-describedby'?: string;
        'aria-invalid'?: boolean;
        columns: typeof dataGrid.columns;
    }) => {
        dataGrid.columns = props.columns;
        return (
            <div
                role="grid"
                aria-labelledby={props['aria-labelledby']}
                aria-describedby={props['aria-describedby']}
                aria-invalid={props['aria-invalid']}
            />
        );
    },
}));

describe('OptionListInput', () => {
    it('uses the shared table layout and preserves text-mode parsing', () => {
        const onChange = vi.fn();
        render(
            <OptionListInput
                label="Auswahloptionen"
                value={[{label: 'Niedrig', value: 'low'}]}
                onChange={onChange}
                addLabel="Option hinzufügen"
                noItemsHint="Keine Optionen vorhanden"
                hint="Beschriftung und Wert müssen eindeutig sein."
                allowEmpty={false}
                labelAction={<button type="button">Feldoptionen</button>}
            />,
        );

        expect(screen.getByRole('grid', {name: 'Auswahloptionen'}))
            .toHaveAccessibleDescription('Beschriftung und Wert müssen eindeutig sein.');
        expect(screen.getByRole('button', {name: 'Option hinzufügen'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Feldoptionen'})).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'Auswahloptionen: Einträge als Text bearbeiten'}));
        const input = screen.getByRole('textbox', {name: 'Einträge'});
        fireEvent.change(input, {target: {value: 'Hoch|high'}});
        fireEvent.blur(input);

        expect(onChange).toHaveBeenLastCalledWith([{label: 'Hoch', value: 'high'}]);
        expect(screen.getByRole('button', {name: 'Tabellenansicht'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Feldoptionen'})).toBeInTheDocument();
    });

    it('offers the values of the referenced select as group values and keeps unknown ones', () => {
        const props = {
            label: 'Optionen',
            onChange: vi.fn(),
            addLabel: 'Option hinzufügen',
            noItemsHint: 'Keine Optionen vorhanden',
            hint: 'Hinweis',
            allowEmpty: false,
            showGroupField: true,
            value: [{label: 'Item A', value: 'a', group: 'stale'}],
        };
        const {rerender} = render(<OptionListInput {...props} groupOptions={[{value: '1', label: 'Group 1'}]}/>);

        expect(dataGrid.columns.find((column) => column.field === 'group')).toMatchObject({
            type: 'singleSelect',
            valueOptions: [
                {value: '', label: 'Ohne Gruppenwert (immer sichtbar)'},
                {value: '1', label: 'Group 1'},
                {value: 'stale', label: 'stale'},
            ],
        });

        // Without known values, for example while they are loading, the group stays free text.
        rerender(<OptionListInput {...props} groupOptions={[]}/>);
        expect(dataGrid.columns.find((column) => column.field === 'group')).toMatchObject({type: 'string'});
        expect(dataGrid.columns.find((column) => column.field === 'group')).not.toHaveProperty('valueOptions');
    });
});
