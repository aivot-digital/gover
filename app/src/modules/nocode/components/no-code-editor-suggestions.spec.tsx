import {fireEvent, render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {useState} from 'react';
import {afterEach, describe, expect, it, vi} from 'vitest';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import {ElementType} from '../../../data/element-type/element-type';
import {NoCodeOperatorDetailsDTO} from '../../../models/dtos/no-code-operator-details-dto';
import {NoCodeExpression, NoCodeOperand} from '../../../models/functions/no-code-expression';
import {OptionsSourceType} from '../../../models/elements/form/input/options-source-type';
import {BaseApiService} from '../../../services/base-api-service';
import {ElementWithParents} from '../../../utils/flatten-elements';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {
    ProcessNodeEditorProvider,
    useProcessNodeEditorContext,
} from '../../process/pages/details/components/process-node-editor/process-node-editor-context';
import {NoCodeEditor} from './no-code-editor';

vi.mock('../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));
vi.mock('../../../hooks/use-api', () => ({useApi: () => ({post: vi.fn()})}));

const operator: NoCodeOperatorDetailsDTO = {
    identifier: 'equals',
    packageName: 'core',
    label: 'Vergleich',
    description: '',
    abstractDescription: '',
    humanReadableTemplate: null,
    tags: [],
    signatures: [{
        returnType: NoCodeDataType.Boolean,
        parameters: ['Auswahl', 'Vergleichswert'].map((label) => ({
            type: NoCodeDataType.String,
            label,
            description: null,
            options: [],
        })),
    }],
};
const field = {
    ...generateElementWithDefaultValues(ElementType.Select),
    id: 'state',
    label: 'Bundesland',
    options: [{
        label: 'Berlin',
        value: 'BE',
    }, {
        label: 'Hamburg',
        value: 'HH',
    }],
};
const expression: NoCodeExpression = {
    type: 'NoCodeExpression',
    operatorIdentifier: 'equals',
    operands: [
        {
            type: 'NoCodeReference',
            elementId: 'state',
        }, {
            type: 'NoCodeStaticValue',
            value: 'BE',
        },
    ],
};

function Editor({
                    initial = expression,
                    elements = [{
                        element: field,
                        parents: [],
                    }],
                    operators = [operator],
                    onChange = vi.fn(),
                }: {
    initial?: NoCodeExpression;
    elements?: ElementWithParents[];
    operators?: NoCodeOperatorDetailsDTO[];
    onChange?: (value: unknown) => void;
}) {
    const [value, setValue] = useState<NoCodeOperand | null | undefined>(initial);
    return <NoCodeEditor parameter={{
        type: NoCodeDataType.Boolean,
        label: 'Bedingung',
        description: null,
        options: [],
    }}
                         operand={value}
                         allElements={elements}
                         allOperators={operators}
                         onChange={(next) => {
                             onChange(next);
                             setValue(next);
                         }}/>;
}

async function editComparison() {
    const item = await screen.findByRole('treeitem', {name: /^Vergleichswert:/});
    fireEvent.keyDown(item, {key: 'Enter'});
}

describe('NoCodeEditor suggestions', () => {
    afterEach(() => vi.restoreAllMocks());

    it('accepts a typed comparison immediately after choosing a fixed value with suggestions', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        render(<Editor onChange={onChange} initial={{...expression, operands: [expression.operands![0], null]}}/>);
        await user.click(screen.getByRole('button', {name: 'Vergleichswert festlegen'}));
        await user.click(screen.getByRole('button', {name: 'Wert selbst festlegen'}));

        const input = screen.getByRole('combobox', {name: 'Vergleichswert'});
        expect(input).toHaveFocus();
        await user.keyboard('BW');
        expect(input).toHaveValue('BW');
        expect(onChange).toHaveBeenLastCalledWith({
            ...expression,
            operands: [expression.operands![0], {type: 'NoCodeStaticValue', value: 'BW'}],
        });
    });

    it('labels a configured option, stores its technical value and still allows free text', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        render(<Editor onChange={onChange}/>);
        expect(screen.getByRole('treeitem', {name: /^Vergleichswert: Berlin,/})).toBeInTheDocument();
        await editComparison();
        const input = screen.getByRole('combobox', {name: /^Vergleichswert/});
        await user.clear(input);
        await user.click(input);
        await user.click(await screen.findByRole('option', {name: /Hamburg/}));
        expect(onChange).toHaveBeenLastCalledWith({
            ...expression,
            operands: [expression.operands![0], {
                type: 'NoCodeStaticValue',
                value: 'HH',
            }],
        });
        await user.clear(input);
        await user.type(input, 'Eigenwert');
        expect(onChange).toHaveBeenLastCalledWith({
            ...expression,
            operands: [expression.operands![0], {
                type: 'NoCodeStaticValue',
                value: 'Eigenwert',
            }],
        });
    });

    it('loads codelist labels and drops stale manual options', async () => {
        vi.spyOn(BaseApiService.prototype, 'get').mockResolvedValue([{
            label: 'Bayern',
            value: 'BY',
        }]);
        render(<Editor elements={[{
            element: {
                ...field,
                optionsSource: OptionsSourceType.CodeList,
                codeListKey: 'states',
            },
            parents: [],
        }]}/>);
        await editComparison();
        const input = await screen.findByRole('combobox', {name: /^Vergleichswert/});
        fireEvent.change(input, {target: {value: ''}});
        fireEvent.mouseDown(input);
        expect(await screen.findByRole('option', {name: /Bayern/})).toBeInTheDocument();
        expect(screen.queryByRole('option', {name: /Berlin/})).not.toBeInTheDocument();
    });

    it('keeps a saved literal editable if loading its codelist fails', async () => {
        vi.spyOn(BaseApiService.prototype, 'get').mockRejectedValue(new Error('unavailable'));
        const onChange = vi.fn();
        render(<Editor onChange={onChange}
                       elements={[{
                           element: {
                               ...field,
                               optionsSource: OptionsSourceType.CodeList,
                               codeListKey: 'missing',
                           },
                           parents: [],
                       }]}/>);
        await editComparison();
        const input = await screen.findByRole('textbox', {name: /^Vergleichswert/});
        expect(input).toHaveValue('BE');
        expect(onChange).not.toHaveBeenCalled();
        fireEvent.change(input, {target: {value: 'HH'}});
        expect(onChange).toHaveBeenLastCalledWith({
            ...expression,
            operands: [expression.operands![0], {
                type: 'NoCodeStaticValue',
                value: 'HH',
            }],
        });
    });

    it('offers matching process data options for an indexed row', async () => {
        const context = {
            incomingMetadata: {
                forwardedProcessDataKeys: [{
                    processDataKey: 'people[*].state',
                    label: 'Bundesland',
                    subLabel: null,
                    origin: {name: 'Formular'},
                    valueOptions: field.options,
                }],
            },
        } as ReturnType<typeof useProcessNodeEditorContext>;
        render(<ProcessNodeEditorProvider value={context}><Editor initial={{
            ...expression,
            operands: [
                {
                    type: 'NoCodeProcessDataReference',
                    path: 'people[2].state',
                }, expression.operands![1],
            ],
        }}
                                                                  elements={[]}/></ProcessNodeEditorProvider>);
        expect(await screen.findByRole('treeitem', {name: /^Vergleichswert: Berlin,/})).toBeInTheDocument();
        await editComparison();
        const input = screen.getByRole('combobox', {name: /^Vergleichswert/});
        fireEvent.change(input, {target: {value: ''}});
        fireEvent.mouseDown(input);
        expect(await screen.findByRole('option', {name: /Hamburg/})).toBeInTheDocument();
    });

    it('does not suggest options for an incompatible parameter type', async () => {
        const typed = {
            ...operator,
            signatures: [{
                ...operator.signatures[0],
                parameters: [
                    operator.signatures[0].parameters[0], {
                        ...operator.signatures[0].parameters[1],
                        type: NoCodeDataType.Number,
                    },
                ],
            }],
        };
        render(<Editor operators={[typed]}/>);
        await editComparison();
        expect(screen.getByRole('textbox', {name: /^Vergleichswert/})).toHaveValue('BE');
        expect(screen.queryByRole('combobox', {name: /^Vergleichswert/})).not.toBeInTheDocument();
    });

    it('updates suggestions when the referenced field changes without rewriting the literal', async () => {
        const onChange = vi.fn();
        const nextField = {
            ...field,
            id: 'other',
            label: 'Anderes Feld',
            options: [{
                label: 'Bayern',
                value: 'BY',
            }],
        };
        const elements = [{
            element: field,
            parents: [],
        }, {
            element: nextField,
            parents: [],
        }];
        const editor = (operand: NoCodeOperand) => <NoCodeEditor
            parameter={{
                type: NoCodeDataType.Boolean,
                label: 'Bedingung',
                description: null,
                options: [],
            }}
            operand={operand}
            allElements={elements}
            allOperators={[operator]}
            onChange={onChange}/>;
        const {rerender} = render(editor(expression));
        expect(screen.getByRole('treeitem', {name: /^Vergleichswert: Berlin,/})).toBeInTheDocument();
        rerender(editor({
            ...expression,
            operands: [{
                type: 'NoCodeReference',
                elementId: 'other',
            }, expression.operands![1]],
        }));
        expect(await screen.findByRole('treeitem', {name: /^Vergleichswert: BE,/})).toBeInTheDocument();
        expect(onChange).not.toHaveBeenCalled();
        await editComparison();
        const input = screen.getByRole('combobox', {name: /^Vergleichswert/});
        expect(input).toHaveValue('BE');
        fireEvent.change(input, {target: {value: ''}});
        fireEvent.mouseDown(input);
        expect(await screen.findByRole('option', {name: /Bayern/})).toBeInTheDocument();
        expect(screen.queryByRole('option', {name: /Berlin/})).not.toBeInTheDocument();
    });

    it('does not collect suggestions from references in an unrelated nested expression', async () => {
        render(<Editor initial={{
            ...expression,
            operands: [expression, expression.operands![1]],
        }}/>);
        const comparisons = screen.getAllByRole('treeitem', {name: /^Vergleichswert:/});
        const outer = comparisons.find((item) => item.getAttribute('aria-label')?.includes('BE,'))!;
        fireEvent.keyDown(outer, {key: 'Enter'});
        expect(screen.getByRole('textbox', {name: /^Vergleichswert/})).toHaveValue('BE');
        expect(screen.queryByRole('combobox', {name: /^Vergleichswert/})).not.toBeInTheDocument();
    });

    it('restricts an operator option to its own choices instead of mixing in reference suggestions', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        const restricted = {
            ...operator,
            signatures: [{
                ...operator.signatures[0],
                parameters: [
                    operator.signatures[0].parameters[0], {
                        ...operator.signatures[0].parameters[1],
                        options: [{
                            value: 'days',
                            label: 'Tage',
                        }, {
                            value: 'weeks',
                            label: 'Wochen',
                        }],
                    },
                ],
            }],
        };
        render(<Editor operators={[restricted]}
                       onChange={onChange}
                       initial={{
                           ...expression,
                           operands: [expression.operands![0], {
                               type: 'NoCodeStaticValue',
                               value: 'days',
                           }],
                       }}/>);
        await editComparison();
        await user.click(screen.getByRole('combobox', {name: /^Vergleichswert/}));
        expect(screen.queryByRole('option', {name: /Berlin/})).not.toBeInTheDocument();
        await user.click(await screen.findByRole('option', {name: 'Wochen'}));
        expect(onChange).toHaveBeenLastCalledWith({
            ...expression,
            operands: [expression.operands![0], {
                type: 'NoCodeStaticValue',
                value: 'weeks',
            }],
        });
    });
});
