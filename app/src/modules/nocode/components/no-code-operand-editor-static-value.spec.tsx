import {render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {useState} from 'react';
import {type NoCodeStaticValue} from '../../../models/functions/no-code-expression';
import {NoCodeOperandEditorStaticValue} from './no-code-operand-editor-static-value';
import {NoCodeDataType} from '../../../data/no-code-data-type';

describe('NoCodeOperandEditorStaticValue', () => {
    it('uses an external label for suggestions and retains selection, free text and control actions', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        const onAddEnclosingExpression = vi.fn();
        function Editor() {
            const [value, setValue] = useState<NoCodeStaticValue>({type: 'NoCodeStaticValue', value: 'done'});
            return <NoCodeOperandEditorStaticValue
                label="Status"
                hint="Status auswählen oder einen eigenen Wert eingeben."
                desiredType={NoCodeDataType.String}
                value={value}
                options={[{label: 'Offen', value: 'open'}, {label: 'Erledigt', value: 'done'}]}
                onChange={(nextValue) => {
                    onChange(nextValue);
                    if (nextValue != null) setValue(nextValue);
                }}
                onAddEnclosingExpression={onAddEnclosingExpression}
            />;
        }
        render(<Editor/>);

        const input = screen.getByRole('combobox', {name: /^Status/});
        const field = input.closest('[data-form-field]')! as HTMLElement;
        const control = input.closest('.MuiInputBase-root')! as HTMLElement;
        expect(field.querySelector('label')).toHaveAttribute('for', input.id);
        expect(control).not.toContainElement(field.querySelector('label'));
        expect(input).toHaveAccessibleDescription('Status auswählen oder einen eigenen Wert eingeben.');
        // This is an editable literal: option labels describe suggestions, not the stored expression value.
        expect(input).toHaveValue('done');

        await user.clear(input);
        await user.click(input);
        await user.click(screen.getByRole('option', {name: /Offen/}));
        expect(onChange).toHaveBeenLastCalledWith({type: 'NoCodeStaticValue', value: 'open'});
        expect(input).toHaveValue('open');
        await user.clear(input);
        await user.type(input, 'custom');
        expect(onChange).toHaveBeenLastCalledWith({type: 'NoCodeStaticValue', value: 'custom'});
        expect(input).toHaveValue('custom');
        await user.click(within(control).getByRole('button', {name: 'Diesen festen Wert mit einem Ausdruck verknüpfen'}));
        expect(onAddEnclosingExpression).toHaveBeenCalledOnce();
    });
});
