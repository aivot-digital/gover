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
        expect(input).toHaveAccessibleDescription(
            'Status auswählen oder einen eigenen Wert eingeben. Der Wert entspricht der Auswahloption „Erledigt“.',
        );
        // This is an editable literal: option labels describe suggestions, not the stored expression value.
        expect(input).toHaveValue('done');

        await user.clear(input);
        expect(screen.queryByText(/^Der Wert entspricht/)).not.toBeInTheDocument();
        await user.click(input);
        await user.click(screen.getByRole('option', {name: /Offen/}));
        expect(onChange).toHaveBeenLastCalledWith({type: 'NoCodeStaticValue', value: 'open'});
        expect(input).toHaveValue('open');
        expect(input).toHaveAccessibleDescription(/Der Wert entspricht der Auswahloption „Offen“\.$/);
        await user.clear(input);
        await user.type(input, 'custom');
        expect(onChange).toHaveBeenLastCalledWith({type: 'NoCodeStaticValue', value: 'custom'});
        expect(input).toHaveValue('custom');
        expect(input).toHaveAccessibleDescription(/Der Wert entspricht keiner Auswahloption\.$/);
        await user.click(within(control).getByRole('button', {name: 'Diesen Wert mit einer Funktion verknüpfen'}));
        expect(onAddEnclosingExpression).toHaveBeenCalledOnce();
        await user.click(within(control).getByRole('button', {name: 'Diesen Wert löschen'}));
        expect(onChange).toHaveBeenLastCalledWith(undefined);
    });

    it('connects number validation to the input and clears it when the count is corrected', async () => {
        const user = userEvent.setup();
        function Editor() {
            const [value, setValue] = useState<NoCodeStaticValue>({type: 'NoCodeStaticValue', value: '1.5'});
            return <NoCodeOperandEditorStaticValue
                label="Anzahl"
                hint="Die Anzahl der gewählten Einheiten. Geben Sie eine ganze Zahl ein."
                desiredType={NoCodeDataType.Number}
                numberConstraints={{integerOnly: true, minimum: -2147483648, maximum: 2147483647}}
                value={value}
                onChange={(nextValue) => { if (nextValue != null) setValue(nextValue); }}
                onAddEnclosingExpression={vi.fn()}
            />;
        }
        render(<Editor/>);
        const input = screen.getByRole('textbox', {name: 'Anzahl'});
        expect(input).toHaveAttribute('aria-invalid', 'true');
        expect(input).toHaveAccessibleDescription('Geben Sie für „Anzahl“ eine ganze Zahl ein.');
        await user.clear(input);
        await user.type(input, '2147483648');
        expect(input).toHaveAccessibleDescription('Geben Sie für „Anzahl“ eine Zahl zwischen −2.147.483.648 und 2.147.483.647 ein.');
        await user.clear(input);
        await user.type(input, '5.0');
        expect(input).not.toHaveAttribute('aria-invalid', 'true');
        expect(input).toHaveAccessibleDescription('Die Anzahl der gewählten Einheiten. Geben Sie eine ganze Zahl ein.');
    });

    it('retains evaluator errors as accessible field errors', () => {
        const value: NoCodeStaticValue = {type: 'NoCodeStaticValue', value: '5'};
        render(<NoCodeOperandEditorStaticValue
            label="Anzahl"
            desiredType={NoCodeDataType.Number}
            numberConstraints={{integerOnly: true}}
            value={value}
            operandError={{operand: value, error: 'Prüfen Sie die Anzahl.', isValid: false, subErrors: []}}
            onChange={vi.fn()}
            onAddEnclosingExpression={vi.fn()}
        />);
        const input = screen.getByRole('textbox', {name: 'Anzahl'});
        expect(input).toHaveAttribute('aria-invalid', 'true');
        expect(input).toHaveAccessibleDescription('Prüfen Sie die Anzahl.');
    });

    it('leaves unrestricted numeric literals editable without whole-number validation', () => {
        render(<NoCodeOperandEditorStaticValue
            label="Zahl"
            desiredType={NoCodeDataType.Number}
            value={{type: 'NoCodeStaticValue', value: '1.5'}}
            onChange={vi.fn()}
            onAddEnclosingExpression={vi.fn()}
        />);
        expect(screen.getByRole('textbox', {name: 'Zahl'})).not.toHaveAttribute('aria-invalid', 'true');
    });

    it('confirms a match without repeating a label that equals the value', () => {
        render(
            <NoCodeOperandEditorStaticValue
                label="Sprache"
                desiredType={NoCodeDataType.String}
                value={{type: 'NoCodeStaticValue', value: 'Deutsch'}}
                options={[{label: 'Deutsch', value: 'Deutsch'}]}
                onChange={vi.fn()}
                onAddEnclosingExpression={vi.fn()}
            />,
        );

        expect(screen.getByRole('combobox', {name: /^Sprache/}))
            .toHaveAccessibleDescription('Der Wert entspricht einer Auswahloption.');
    });
});
