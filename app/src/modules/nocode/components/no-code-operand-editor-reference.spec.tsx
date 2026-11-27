import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import {ElementType} from '../../../data/element-type/element-type';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {NoCodeOperandEditorReference} from './no-code-operand-editor-reference';

describe('NoCodeOperandEditorReference', () => {
    it('keeps the configured label and names actions for the form element reference', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        const onAddEnclosingExpression = vi.fn();
        const element = {...generateElementWithDefaultValues(ElementType.Text), id: 'name', label: 'Name'};

        render(<NoCodeOperandEditorReference
            allElements={[{element, parents: []}]}
            label="Kundenfeld"
            value={{type: 'NoCodeReference', elementId: element.id}}
            onChange={onChange}
            desiredType={NoCodeDataType.String}
            onAddEnclosingExpression={onAddEnclosingExpression}
        />);

        expect(screen.getByRole('combobox', {name: 'Kundenfeld'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Formularelement für diesen Verweis auswählen'})).toBeInTheDocument();
        await user.click(screen.getByRole('button', {name: 'Diesen Verweis mit einer Funktion verknüpfen'}));
        expect(onAddEnclosingExpression).toHaveBeenCalledOnce();
        await user.click(screen.getByRole('button', {name: 'Diesen Verweis auf ein Formularelement löschen'}));
        expect(onChange).toHaveBeenCalledWith(undefined);
    });
});
