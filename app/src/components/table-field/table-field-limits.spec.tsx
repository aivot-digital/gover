import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {ElementType} from '../../data/element-type/element-type';
import {createDerivedRuntimeElementData} from '../../models/element-data';
import {generateElementWithDefaultValues} from '../../utils/generate-element-with-default-values';
import {TableFieldComponent} from './table-field-component';
import {TableFieldComponent2} from './table-field-component-2';
import {TableFieldComponentView} from './table-field.component.view';

vi.mock('@mui/x-data-grid', () => ({DataGrid: () => null}));

const cases = [
    {
        name: 'form element',
        render: (maximumRows: number, onChange: () => void) => <TableFieldComponentView
            element={{...generateElementWithDefaultValues(ElementType.Table), id: 'rows', label: 'Einträge', fields: [], maximumRows}}
            value={[{}]}
            setValue={onChange}
            onBlur={vi.fn()}
            isBusy={false}
            isDeriving={false}
            errors={undefined}
            errorDetails={undefined}
            authoredElementValues={{}}
            onAuthoredElementValuesChange={vi.fn()}
            onElementBlur={vi.fn()}
            derivedData={createDerivedRuntimeElementData()}
            onDerive={vi.fn(async () => createDerivedRuntimeElementData())}
            onEvent={vi.fn(async () => undefined)}
            onResetErrors={vi.fn()}
            suppressErrors={false}
            derivationTriggerIdQueue={[]}
        />,
    },
    {
        name: 'shared component',
        render: (maximumRows: number, onChange: () => void) => <TableFieldComponent2
            label="Einträge"
            fields={[]}
            createDefaultRow={() => ({})}
            value={[{}]}
            maximumRows={maximumRows}
            onChange={onChange}
        />,
    },
    {
        name: 'legacy component',
        render: (maximumRows: number, onChange: () => void) => <TableFieldComponent
            label="Einträge"
            fields={[]}
            value={[{}]}
            maximumRows={maximumRows}
            onChange={onChange}
        />,
    },
];

describe.each(cases)('table row limits: $name', testCase => {
    it('allows adding rows with maximum zero and blocks adding at a positive maximum', () => {
        const onChange = vi.fn();
        const {rerender} = render(testCase.render(0, onChange));
        const addButton = screen.getByRole('button', {name: /hinzufügen/i});
        expect(addButton).toBeEnabled();
        fireEvent.click(addButton);
        expect(onChange).toHaveBeenCalledWith([{}, {}]);

        rerender(testCase.render(1, onChange));
        expect(screen.getByRole('button', {name: /hinzufügen/i})).toBeDisabled();
    });
});
