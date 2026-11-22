import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {useState} from 'react';
import {describe, expect, it, vi} from 'vitest';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import {type NoCodeParameter} from '../../../models/dtos/no-code-operator-details-dto';
import {type NoCodeOperand} from '../../../models/functions/no-code-expression';
import {NoCodeOperandEditor} from './no-code-operand-editor';

vi.mock('../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));

describe('NoCodeOperandEditor', () => {
    it('opens an empty element-data input and supports entering, clearing and reentering a reference', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        function Editor() {
            const [operand, setOperand] = useState<NoCodeOperand | null | undefined>(null);
            return <NoCodeOperandEditor
                parameter={{type: NoCodeDataType.String, label: 'Ergebnis', description: null, options: []}}
                operand={operand}
                onChange={(value) => {
                    onChange(value);
                    setOperand(value);
                }}
                allOperators={[]}
                allElements={[]}
                contextType="PROCESS"
            />;
        }
        render(<Editor/>);

        await user.click(screen.getByRole('button', {name: 'Elementdaten'}));
        const input = screen.getByRole('textbox', {name: /^Ergebnis/});
        expect(input).toHaveValue('');
        expect(input.parentElement).toHaveTextContent('_.');
        expect(onChange).toHaveBeenCalledExactlyOnceWith({
            type: 'NoCodeNodeDataReference',
            nodeDataKey: '',
            path: '',
        });

        await user.type(input, 'formular.');
        expect(input).toHaveValue('formular.');
        await user.type(input, 'person.name');
        expect(input).toHaveValue('formular.person.name');
        expect(onChange).toHaveBeenLastCalledWith({
            type: 'NoCodeNodeDataReference',
            nodeDataKey: 'formular',
            path: 'person.name',
        });

        await user.clear(input);
        expect(input).toHaveValue('');
        expect(onChange).toHaveBeenLastCalledWith({
            type: 'NoCodeNodeDataReference',
            nodeDataKey: '',
            path: undefined,
        });
        await user.type(input, 'pruefung.ergebnis');
        expect(input).toHaveValue('pruefung.ergebnis');
        expect(onChange).toHaveBeenLastCalledWith({
            type: 'NoCodeNodeDataReference',
            nodeDataKey: 'pruefung',
            path: 'ergebnis',
        });
    });

    it.each(['textbox', 'combobox'] as const)('retains authored whitespace after leaving the %s', async role => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        function Editor() {
            const [operand, setOperand] = useState<NoCodeOperand | null | undefined>({type: 'NoCodeStaticValue', value: ' '});
            return <NoCodeOperandEditor
                allElements={[]} allOperators={[]}
                parameter={{type: NoCodeDataType.String, label: 'Text', description: null, options: [], preserveWhitespace: true}}
                suggestions={role === 'combobox' ? [{label: 'Gruß', value: 'Hallo '}] : undefined}
                operand={operand}
                onChange={next => {
                    onChange(next);
                    setOperand(next);
                }}
            />;
        }
        render(<Editor/>);
        const input = screen.getByRole(role, {name: 'Text'});
        await user.click(input);
        await user.tab();
        expect(input).toHaveValue(' ');
        expect(onChange).not.toHaveBeenCalled();

        for (const value of ['  Hallo  ', ' ', '\t\u00a0']) {
            await user.clear(input);
            await user.paste(value);
            await user.tab();
            expect(input).toHaveValue(value);
            expect(onChange).toHaveBeenLastCalledWith({type: 'NoCodeStaticValue', value});
        }

        await user.clear(input);
        await user.tab();
        expect(input).toHaveValue('');
        expect(onChange).toHaveBeenLastCalledWith({type: 'NoCodeStaticValue', value: undefined});
        if (role === 'combobox') {
            await user.click(input);
            await user.click(screen.getByRole('option', {name: /Gruß/}));
            await user.tab();
            expect(input).toHaveValue('Hallo ');
            expect(onChange).toHaveBeenLastCalledWith({type: 'NoCodeStaticValue', value: 'Hallo '});
        }
    });

    it('stores the same technical value from direct options and the existing fixed-value editor', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        const parameter: NoCodeParameter = {
            type: NoCodeDataType.String,
            label: 'Vergleich',
            description: 'Die auszuführende Vergleichsoperation.',
            options: [
                {label: 'ist davor', value: 'vor'},
                {label: 'ist davor oder gleich', value: 'vor-oder-gleich'},
            ],
        };
        const props = {
            parameter,
            allOperators: [],
            allElements: [],
            onChange,
            contextType: 'FORM' as const,
        };
        const {rerender} = render(<NoCodeOperandEditor {...props} operand={null}/>);

        expect(screen.getByRole('list', {name: 'Vergleich auswählen'})).toBeVisible();
        expect(screen.queryByRole('button', {name: 'Wert selbst festlegen'})).not.toBeInTheDocument();
        await user.click(screen.getByRole('button', {name: 'ist davor oder gleich'}));
        const expectedOperand = {type: 'NoCodeStaticValue', value: 'vor-oder-gleich'};
        expect(onChange).toHaveBeenCalledExactlyOnceWith(expectedOperand);

        rerender(<NoCodeOperandEditor {...props} operand={{type: 'NoCodeStaticValue', value: 'vor'}}/>);
        await user.click(screen.getByRole('combobox', {name: /^Vergleich/}));
        await user.click(screen.getByRole('option', {name: 'ist davor oder gleich'}));
        expect(onChange).toHaveBeenCalledTimes(2);
        expect(onChange).toHaveBeenLastCalledWith(expectedOperand);
    });

    it('keeps suggestions behind source selection and permits custom values after choosing a suggestion', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        const parameter: NoCodeParameter = {
            type: NoCodeDataType.String,
            label: 'Status',
            description: null,
            options: [],
        };
        function Editor() {
            const [operand, setOperand] = useState<NoCodeOperand | null | undefined>(null);
            return <NoCodeOperandEditor
                parameter={parameter}
                suggestions={[{label: 'Offen', value: 'open'}]}
                operand={operand}
                onChange={(value) => {
                    onChange(value);
                    setOperand(value);
                }}
                allOperators={[]}
                allElements={[]}
                contextType="FORM"
            />;
        }
        render(<Editor/>);

        expect(screen.getByRole('list', {name: 'Status'})).toBeVisible();
        expect(screen.queryByRole('button', {name: 'Offen'})).not.toBeInTheDocument();
        await user.click(screen.getByRole('button', {name: 'Wert selbst festlegen'}));
        expect(onChange).toHaveBeenCalledExactlyOnceWith({type: 'NoCodeStaticValue', value: ''});

        const input = screen.getByRole('combobox', {name: /^Status/});
        await user.click(input);
        await user.click(screen.getByRole('option', {name: /Offen/}));
        expect(onChange).toHaveBeenLastCalledWith({type: 'NoCodeStaticValue', value: 'open'});
        await user.clear(input);
        await user.type(input, 'custom');
        expect(input).toHaveValue('custom');
        expect(onChange).toHaveBeenLastCalledWith({type: 'NoCodeStaticValue', value: 'custom'});
        expect(input).toHaveAccessibleDescription('Der Wert entspricht keiner Auswahloption.');
        expect(input).not.toHaveAttribute('aria-invalid', 'true');
    });
});
