import {render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {NoCodeOperandEditorProcessDataReference} from './no-code-operand-editor-process-data-reference';
import {type NoCodeOperand} from '../../../models/functions/no-code-expression';

describe('NoCodeOperandEditorProcessDataReference', () => {
    it.each([
        {type: 'NoCodeProcessDataReference', path: 'warenkorb.anzahl'},
        {type: 'NoCodeInstanceDataReference', path: 'caseNumber'},
        {type: 'NoCodeNodeDataReference', nodeDataKey: 'warenkorbLaden', path: 'anzahl'},
    ] satisfies NoCodeOperand[])('keeps $type actions inside the control rather than beside the field', async (value) => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        const onAddEnclosingExpression = vi.fn();
        render(<NoCodeOperandEditorProcessDataReference
            label="Summand 1"
            hint="Der erste Wert, der addiert werden soll."
            value={value}
            onChange={onChange}
            onAddEnclosingExpression={onAddEnclosingExpression}
        />);

        const input = screen.getByRole('textbox', {name: /^Summand 1/});
        const control = input.closest('.MuiInputBase-root')! as HTMLElement;
        expect(within(control).getByRole('button', {name: 'Diesen Vorgangsdaten-Verweis löschen'})).toBeInTheDocument();
        await user.click(within(control).getByRole('button', {name: 'Diesen Verweis mit einem Ausdruck verknüpfen'}));
        expect(onAddEnclosingExpression).toHaveBeenCalledOnce();
        await user.click(within(control).getByRole('button', {name: 'Diesen Vorgangsdaten-Verweis löschen'}));
        expect(onChange).toHaveBeenCalledWith(undefined);

        if (value.type === 'NoCodeProcessDataReference') {
            await user.click(within(control).getByRole('button', {name: 'Vorgangsdatenpfad leeren'}));
            expect(onChange).toHaveBeenLastCalledWith({...value, path: undefined});
        }
    });
});
