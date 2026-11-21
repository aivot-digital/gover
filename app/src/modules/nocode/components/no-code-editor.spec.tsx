import {act, fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import {useState} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {ElementType} from '../../../data/element-type/element-type';
import {NoCodeDryRunStatus} from '../../../models/dtos/no-code-dry-run-dto';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import {type NoCodeOperatorDetailsDTO} from '../../../models/dtos/no-code-operator-details-dto';
import {type NoCodeOperand} from '../../../models/functions/no-code-expression';
import {NoCodeEditor} from './no-code-editor';

vi.mock('../../../components/code-editor/monaco-editor', () => ({
    MonacoEditor: (props: import('@monaco-editor/react').EditorProps) => <textarea
        aria-label={props.options?.ariaLabel}
        value={props.value ?? ''}
        onChange={event => props.onChange?.(event.target.value, {} as never)}/>,
}));
vi.mock('../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));
const api = vi.hoisted(() => ({post: vi.fn()}));
vi.mock('../../../hooks/use-api', () => ({useApi: () => api}));

beforeEach(() => {
    api.post.mockReset();
});

const equals: NoCodeOperatorDetailsDTO = {
    identifier: 'equals',
    packageName: 'core',
    label: 'Ist Gleich',
    description: '',
    abstractDescription: '',
    humanReadableTemplate: null,
    tags: [],
    signatures: [{
        returnType: NoCodeDataType.Boolean,
        parameters: ['Wert 1', 'Wert 2'].map((label) => ({
            type: NoCodeDataType.Runtime,
            label,
            description: null,
            options: [],
        })),
    }],
};

const expression: NoCodeOperand = {
    type: 'NoCodeExpression',
    operatorIdentifier: 'equals',
    operands: [
        {
            type: 'NoCodeStaticValue',
            value: 'a',
        },
        {
            type: 'NoCodeStaticValue',
            value: 'b',
        },
    ],
};

const not: NoCodeOperatorDetailsDTO = {
    ...equals,
    identifier: 'not',
    label: 'Nicht',
    signatures: [{
        returnType: NoCodeDataType.Boolean,
        parameters: [{
            type: NoCodeDataType.Boolean,
            label: 'Wert',
            description: null,
            options: [],
        }],
    }],
};

const add: NoCodeOperatorDetailsDTO = {
    ...equals,
    identifier: 'add',
    label: 'Addiere',
    signatures: [{
        returnType: NoCodeDataType.Number,
        parameters: ['Summand 1', 'Summand 2'].map((label) => ({
            type: NoCodeDataType.Number,
            label,
            description: null,
            options: [],
        })),
    }],
};

// Has no parameter that could take a wrapped node.
const alwaysTrue: NoCodeOperatorDetailsDTO = {
    ...equals,
    identifier: 'always-true',
    label: 'Immer wahr',
    signatures: [{
        returnType: NoCodeDataType.Boolean,
        parameters: [],
    }],
};

const a: NoCodeOperand = {
    type: 'NoCodeStaticValue',
    value: 'a',
};
const amount = {
    ...generateElementWithDefaultValues(ElementType.Number),
    id: 'amount',
    label: 'Betrag',
};

// equals(a, not(empty)): a leaf, an operator to swap with and a socket to move into.
const nested: NoCodeOperand = {
    type: 'NoCodeExpression',
    operatorIdentifier: 'equals',
    operands: [a, {
        type: 'NoCodeExpression',
        operatorIdentifier: 'not',
        operands: [null],
    }],
};

function Harness(props: {
    onChange: (operand: NoCodeOperand | null | undefined) => void;
    initial?: NoCodeOperand | null;
    readOnly?: boolean;
    busy?: boolean;
    operators?: NoCodeOperatorDetailsDTO[];
    returnType?: NoCodeDataType;
}) {
    // Clones like a parent that copies its state, which must not rebuild the tree.
    const [operand, setOperand] = useState<NoCodeOperand | null | undefined>(props.initial === undefined ? expression : props.initial);
    return (
        <NoCodeEditor
            readOnly={props.readOnly}
            busy={props.busy}
            parameter={{
                type: props.returnType ?? NoCodeDataType.Boolean,
                label: 'Bedingung',
                description: null,
                options: [],
            }}
            operand={operand}
            onChange={(next) => {
                props.onChange(next);
                setOperand(structuredClone(next));
            }}
            allOperators={props.operators ?? [equals, not, add, alwaysTrue]}
            allElements={[{
                element: amount,
                parents: [],
            }]}
        />
    );
}

describe('NoCodeEditor', () => {
    it.each(['readOnly', 'busy'] as const)('blocks mouse and keyboard mutations in %s mode while retaining descriptions', async (mode) => {
        const onChange = vi.fn();
        render(<Harness onChange={onChange} {...{[mode]: true}}/>);
        expect(screen.getByRole('tree')).toHaveAttribute('aria-readonly', 'true');
        expect(screen.queryByRole('button', {name: 'Festen Wert „a“ löschen'})).not.toBeInTheDocument();
        const first = screen.getByRole('treeitem', {name: 'Wert 1: a, Beliebiger Wert'});
        first.focus();
        for (const key of ['Delete', 'Backspace', ' ']) fireEvent.keyDown(first, {key});
        fireEvent.keyDown(first, {
            key: 'F10',
            shiftKey: true,
        });
        expect(await screen.findByRole('menuitem', {name: 'Übergeordneten Operator hinzufügen'})).toHaveAttribute('aria-disabled', 'true');
        fireEvent.click(screen.getByRole('menuitem', {name: 'An andere Stelle verschieben'}));
        expect(onChange).not.toHaveBeenCalled();
        fireEvent.click(screen.getByRole('menuitem', {name: 'Beschreibung anzeigen'}));
        expect(await screen.findByRole('dialog')).toHaveTextContent('Fester Wert');
        expect(onChange).not.toHaveBeenCalled();
    });

    it('closes an open value editor when editing is disabled', async () => {
        const onChange = vi.fn();
        const {rerender} = render(<Harness onChange={onChange}/>);
        fireEvent.click(screen.getByRole('button', {name: 'a, Beliebiger Wert'}));
        await screen.findByRole('textbox', {name: /^Wert 1/});
        rerender(<Harness onChange={onChange}
                          readOnly/>);
        await waitFor(() => expect(screen.queryByRole('textbox', {name: /^Wert 1/})).not.toBeInTheDocument());
        expect(onChange).not.toHaveBeenCalled();
    });

    it('undoes and redoes deletion of the complete expression using the history buttons', async () => {
        const onChange = vi.fn();
        render(<Harness onChange={onChange}/>);
        const root = screen.getByRole('treeitem', {name: 'Bedingung: Operator Ist Gleich, Wahrheitswert'});
        root.focus();
        fireEvent.keyDown(root, {key: 'Delete'});
        expect(onChange).not.toHaveBeenCalled();
        fireEvent.click(screen.getByRole('button', {name: 'Alles löschen'}));
        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        expect(onChange).toHaveBeenLastCalledWith(null);
        fireEvent.click(screen.getByRole('button', {name: 'Rückgängig'}));
        expect(onChange).toHaveBeenLastCalledWith(expression);
        fireEvent.click(screen.getByRole('button', {name: 'Wiederholen'}));
        expect(onChange).toHaveBeenLastCalledWith(null);
        fireEvent.click(screen.getByRole('button', {name: 'Rückgängig'}));
        expect(onChange).toHaveBeenLastCalledWith(expression);
    });

    it('starts an empty expression through the shared choice without inserting a default value', async () => {
        const onChange = vi.fn();
        render(<Harness initial={null}
                        onChange={onChange}/>);
        expect(onChange).not.toHaveBeenCalled();
        expect(screen.getByText('Noch kein Ausdruck festgelegt')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Wert oder Variable'})).not.toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', {name: 'Ausdruck hinzufügen'}));
        fireEvent.click(screen.getByRole('button', {name: 'Operator'}));
        const dialog = await screen.findByRole('dialog', {name: 'Operator auswählen'});
        fireEvent.click(within(dialog).getByRole('button', {name: 'Ist Gleich: Hinzufügen'}));
        expect(onChange).toHaveBeenLastCalledWith({
            type: 'NoCodeExpression',
            operatorIdentifier: 'equals',
            operands: [null, null],
        });
    });

    it('opens the empty expression by keyboard and preserves it on cancellation', async () => {
        const onChange = vi.fn();
        render(<Harness initial={null}
                        onChange={onChange}/>);
        const empty = screen.getByRole('treeitem');
        expect(empty).toHaveAccessibleDescription(/Vergleichen oder berechnen Sie Werte/);
        empty.focus();
        fireEvent.keyDown(empty, {key: 'Enter'});
        const choice = screen.getByRole('button', {name: 'Fester Wert'});
        fireEvent.keyDown(choice, {key: 'Escape'});
        await waitFor(() => expect(screen.queryByRole('button', {name: 'Fester Wert'})).not.toBeInTheDocument());
        expect(onChange).not.toHaveBeenCalled();
        expect(screen.getByText('Noch kein Ausdruck festgelegt')).toBeInTheDocument();
        expect(empty).toHaveFocus();
    });

    it('does not offer creation for a read-only empty expression', () => {
        const onChange = vi.fn();
        render(<Harness initial={null}
                        readOnly
                        onChange={onChange}/>);
        fireEvent.keyDown(screen.getByRole('treeitem'), {key: 'Enter'});
        expect(screen.getByText('Noch kein Ausdruck festgelegt')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Ausdruck hinzufügen'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Operator'})).not.toBeInTheDocument();
        expect(onChange).not.toHaveBeenCalled();
    });

    it('associates each empty inline editor with its own validation message', () => {
        render(<>{['Sichtbarkeit', 'Bedingung'].map(label => <NoCodeEditor key={label}
                                                                           parameter={{
                                                                               type: NoCodeDataType.Boolean,
                                                                               label,
                                                                               description: null,
                                                                               options: [],
                                                                           }}
                                                                           operand={null}
                                                                           onChange={vi.fn()}
                                                                           allOperators={[]}
                                                                           allElements={[]}
                                                                           validationRequest={1}
        />)}</>);

        const items = screen.getAllByRole('treeitem');
        for (const item of items) {
            expect(item).toHaveAttribute('aria-invalid', 'true');
            const descriptions = item.getAttribute('aria-describedby')!.split(' ').map(id => document.getElementById(id));
            expect(descriptions.every(description => description != null && item.contains(description))).toBe(true);
        }
    });

    it('keeps every operand visible when stored data contains operands for a zero-parameter operator', () => {
        render(<Harness initial={{
            ...expression,
            operatorIdentifier: 'always-true',
        } as NoCodeOperand}
                        onChange={vi.fn()}/>);
        expect(screen.getByRole('treeitem', {name: 'Parameter 1: a, Beliebiger Wert'})).toBeInTheDocument();
        expect(screen.getByRole('treeitem', {name: 'Parameter 2: b, Beliebiger Wert'})).toBeInTheDocument();
        expect(screen.getByText(/Die Anzahl der Parameter passt nicht/)).toBeInTheDocument();
    });

    it('confirms removed operands, leaves cancellation unchanged and can undo a zero-parameter switch', async () => {
        const onChange = vi.fn();
        render(<Harness onChange={onChange}/>);
        const choose = async () => {
            fireEvent.click(screen.getByRole('button', {name: 'Ist Gleich, Wahrheitswert'}));
            const picker = await screen.findByRole('dialog', {name: 'Operator auswählen'});
            fireEvent.click(within(picker).getByRole('button', {name: 'Immer wahr: Ersetzen'}));
            return screen.findByRole('dialog', {name: 'Operator ändern'});
        };
        let dialog = await choose();
        expect(within(dialog).getByRole('alert')).toHaveTextContent('2 belegte Parameter werden');
        expect(onChange).not.toHaveBeenCalled();
        fireEvent.keyDown(dialog, {key: 'Escape'});
        await waitFor(() => expect(screen.queryByRole('dialog', {name: 'Operator ändern'})).not.toBeInTheDocument());
        expect(onChange).not.toHaveBeenCalled();
        dialog = await choose();
        fireEvent.click(within(dialog).getByRole('button', {name: 'Operator ändern'}));
        expect(onChange).toHaveBeenLastCalledWith({
            type: 'NoCodeExpression',
            operatorIdentifier: 'always-true',
            operands: [],
        });
        fireEvent.click(screen.getByRole('button', {name: 'Rückgängig'}));
        expect(onChange).toHaveBeenLastCalledWith(expression);
    });

    it('offers supported parameter counts and preserves values when extending a signature', async () => {
        const flexible: NoCodeOperatorDetailsDTO = {
            ...equals,
            identifier: 'flexible',
            label: 'Mehrere Varianten',
            signatures: [not.signatures[0], {
                ...equals.signatures[0],
                parameters: [
                    ...equals.signatures[0].parameters,
                    {
                        ...equals.signatures[0].parameters[1],
                        label: 'Wert 3',
                    },
                ],
            }],
        };
        const onChange = vi.fn();
        render(<Harness onChange={onChange}
                        operators={[equals, flexible]}/>);
        fireEvent.click(screen.getByRole('button', {name: 'Ist Gleich, Wahrheitswert'}));
        const picker = await screen.findByRole('dialog', {name: 'Operator auswählen'});
        fireEvent.click(within(picker).getByRole('button', {name: 'Mehrere Varianten: Ersetzen'}));
        const dialog = await screen.findByRole('dialog', {name: 'Operator ändern'});
        expect(within(dialog).getByRole('combobox', {name: /Anzahl der Parameter/})).toHaveTextContent('3');
        expect(onChange).not.toHaveBeenCalled();
        fireEvent.click(within(dialog).getByRole('button', {name: 'Operator ändern'}));
        expect(onChange).toHaveBeenLastCalledWith({
            type: 'NoCodeExpression',
            operatorIdentifier: 'flexible',
            operands: [{
                type: 'NoCodeStaticValue',
                value: 'a',
            }, {
                type: 'NoCodeStaticValue',
                value: 'b',
            }, null],
        });
        expect(screen.getByRole('treeitem', {name: 'Wert 3: leer, Beliebiger Wert'})).toBeInTheDocument();
    });

    it('invalidates test results immediately and ignores an older response after undo', async () => {
        let resolveOld: (value: unknown) => void = () => {
        };
        api.post.mockResolvedValueOnce({
            status: NoCodeDryRunStatus.Evaluated,
            value: true,
            operands: [],
        })
            .mockImplementationOnce(() => new Promise((resolve) => {
                resolveOld = resolve;
            }))
            .mockResolvedValue({
                status: NoCodeDryRunStatus.Evaluated,
                value: true,
                operands: [],
            });
        render(<Harness onChange={vi.fn()}/>);
        fireEvent.click(screen.getByRole('button', {name: 'Testlauf'}));
        fireEvent.click(screen.getByRole('button', {name: 'Test ausführen'}));
        await waitFor(() => expect(screen.getByRole('treeitem', {name: 'Bedingung: Operator Ist Gleich, Wahrheitswert'}))
            .toHaveAccessibleDescription('Testergebnis: Wahr'));
        fireEvent.click(screen.getByRole('button', {name: 'Testlauf'}));
        fireEvent.click(screen.getByRole('button', {name: 'a, Beliebiger Wert'}));
        const input = await screen.findByRole('textbox', {name: /^Wert 1/});
        fireEvent.change(input, {target: {value: 'changed'}});
        fireEvent.keyDown(input, {key: 'Escape'});
        await waitFor(() => expect(screen.queryByRole('textbox', {name: /^Wert 1/})).not.toBeInTheDocument());
        fireEvent.click(screen.getByRole('button', {name: 'Testlauf'}));
        expect(screen.getByRole('region', {name: 'Gesamtergebnis'})).toHaveTextContent('Nicht mehr aktuell');
        fireEvent.click(screen.getByRole('button', {name: 'Test ausführen'}));
        await waitFor(() => expect(api.post).toHaveBeenCalledTimes(2));
        fireEvent.click(screen.getByRole('button', {name: 'Rückgängig'}));
        fireEvent.click(screen.getByRole('button', {name: 'Test ausführen'}));
        await waitFor(() => expect(api.post).toHaveBeenCalledTimes(3));
        await act(async () => resolveOld({
            status: NoCodeDryRunStatus.Evaluated,
            value: false,
            operands: [],
        }));
        expect(screen.getByRole('treeitem', {name: 'Bedingung: Operator Ist Gleich, Wahrheitswert'}))
            .toHaveAccessibleDescription('Testergebnis: Wahr');
    });

    it('shows operator labels and emits operands without editor IDs', () => {
        const onChange = vi.fn();
        render(<Harness onChange={onChange}/>);

        expect(screen.getByRole('button', {name: 'Ist Gleich, Wahrheitswert'})).toHaveTextContent('Ist Gleich');
        const second = screen.getByRole('treeitem', {name: 'Wert 2: b, Beliebiger Wert'});

        fireEvent.click(screen.getByRole('button', {name: 'Festen Wert „a“ löschen'}));

        expect(onChange).toHaveBeenLastCalledWith({
            ...expression,
            operands: [null, {
                type: 'NoCodeStaticValue',
                value: 'b',
            }],
        });
        // The remaining item keeps its DOM node, so focus and future drag targets stay attached.
        expect(screen.getByRole('treeitem', {name: 'Wert 2: b, Beliebiger Wert'})).toBe(second);
    });

    it('keeps a single tab stop and navigates the tree with arrow keys', () => {
        render(<Harness onChange={vi.fn()}/>);

        const tree = screen.getByRole('tree', {name: 'Bedingung'});
        const root = screen.getByRole('treeitem', {name: 'Bedingung: Operator Ist Gleich, Wahrheitswert'});
        const first = screen.getByRole('treeitem', {name: 'Wert 1: a, Beliebiger Wert'});
        const second = screen.getByRole('treeitem', {name: 'Wert 2: b, Beliebiger Wert'});
        const tabStops = () => Array.from(tree.querySelectorAll<HTMLElement>('[tabindex]')).filter((el) => el.tabIndex >= 0);
        expect(tabStops()).toEqual([root]);

        root.focus();
        fireEvent.keyDown(root, {key: 'ArrowRight'});
        expect(first).toHaveFocus();
        fireEvent.keyDown(first, {key: 'ArrowDown'});
        expect(second).toHaveFocus();
        expect(tabStops()).toEqual([second]);
        fireEvent.keyDown(second, {key: 'ArrowLeft'});
        expect(root).toHaveFocus();
        fireEvent.keyDown(root, {key: 'End'});
        expect(second).toHaveFocus();
        fireEvent.keyDown(second, {key: 'Home'});
        expect(root).toHaveFocus();
        fireEvent.keyDown(root, {key: 'ArrowUp'});
        expect(root).toHaveFocus();
    });

    it('removes the focused node with the Delete key and focuses the socket it leaves', () => {
        const onChange = vi.fn();
        render(<Harness onChange={onChange}/>);

        const first = screen.getByRole('treeitem', {name: 'Wert 1: a, Beliebiger Wert'});
        first.focus();
        fireEvent.keyDown(first, {key: 'Delete'});

        expect(onChange).toHaveBeenLastCalledWith({
            ...expression,
            operands: [null, {
                type: 'NoCodeStaticValue',
                value: 'b',
            }],
        });
        const socket = screen.getByRole('treeitem', {name: 'Wert 1: leer, Beliebiger Wert'});
        expect(socket).toHaveFocus();
        expect(socket).toHaveAttribute('tabindex', '0');
    });

    it('fills a socket from the keyboard', async () => {
        const onChange = vi.fn();
        render(<Harness onChange={onChange}/>);
        const first = screen.getByRole('treeitem', {name: 'Wert 1: a, Beliebiger Wert'});
        first.focus();
        fireEvent.keyDown(first, {key: 'Delete'});

        fireEvent.keyDown(screen.getByRole('treeitem', {name: 'Wert 1: leer, Beliebiger Wert'}), {key: 'Enter'});
        fireEvent.click(await screen.findByRole('button', {name: 'Fester Wert'}));

        expect(onChange).toHaveBeenLastCalledWith({
            ...expression,
            operands: [{
                type: 'NoCodeStaticValue',
                value: '',
            }, {
                type: 'NoCodeStaticValue',
                value: 'b',
            }],
        });
        // The new value opens its editor right away; closing it returns focus to the new tree item.
        const valueInput = await screen.findByRole('textbox', {name: /^Wert 1/});
        fireEvent.keyDown(valueInput, {key: 'Escape'});
        await waitFor(() => expect(screen.getByRole('treeitem', {name: 'Wert 1: Leerer Text, Beliebiger Wert'})).toHaveFocus());
    });

    it('ignores tree shortcuts typed into a value editor', async () => {
        const onChange = vi.fn();
        render(<Harness onChange={onChange}/>);

        fireEvent.click(screen.getByRole('button', {name: 'a, Beliebiger Wert'}));
        const input = await screen.findByRole('textbox', {name: /^Wert 1/});
        input.focus();
        fireEvent.keyDown(input, {key: 'Backspace'});
        fireEvent.keyDown(input, {key: 'Delete'});
        fireEvent.keyDown(input, {key: 'ArrowDown'});

        expect(onChange).not.toHaveBeenCalled();
        expect(input).toHaveFocus();
    });

    it('opens the operator picker with the Enter key', async () => {
        render(<Harness onChange={vi.fn()}/>);
        const root = screen.getByRole('treeitem', {name: 'Bedingung: Operator Ist Gleich, Wahrheitswert'});
        root.focus();

        fireEvent.keyDown(root, {key: 'Enter'});

        expect(await screen.findByRole('dialog', {name: 'Operator auswählen'})).toBeInTheDocument();
    });

    describe('moving', () => {
        it('moves a node with Space, the arrow keys and Space and announces each step', () => {
            const onChange = vi.fn();
            render(<Harness onChange={onChange}
                            initial={nested}/>);
            const status = screen.getByRole('status');
            const leaf = screen.getByRole('treeitem', {name: 'Wert 1: a, Beliebiger Wert'});
            leaf.focus();

            fireEvent.keyDown(leaf, {key: ' '});
            expect(status).toHaveTextContent('„a“ aufgenommen. Ziel: Wert 2: Operator Nicht, Wahrheitswert, die Werte werden getauscht.');
            expect(leaf).toHaveFocus();

            fireEvent.keyDown(leaf, {key: 'ArrowDown'});
            expect(status).toHaveTextContent('Ziel: Wert: leer, Wahrheitswert.');
            // The last target stays selected.
            fireEvent.keyDown(leaf, {key: 'ArrowDown'});
            expect(status).toHaveTextContent('Ziel: Wert: leer, Wahrheitswert.');

            fireEvent.keyDown(leaf, {key: ' '});
            expect(onChange).toHaveBeenLastCalledWith({
                ...nested,
                operands: [null, {
                    type: 'NoCodeExpression',
                    operatorIdentifier: 'not',
                    operands: [a],
                }],
            });
            expect(status).toHaveTextContent('„a“ wurde verschoben.');
            expect(screen.getByRole('treeitem', {name: 'Wert: a, Wahrheitswert'})).toHaveFocus();
        });

        it('cancels with Escape without changing the expression', () => {
            const onChange = vi.fn();
            render(<Harness onChange={onChange}
                            initial={nested}/>);
            const leaf = screen.getByRole('treeitem', {name: 'Wert 1: a, Beliebiger Wert'});
            leaf.focus();

            fireEvent.keyDown(leaf, {key: ' '});
            fireEvent.keyDown(leaf, {key: 'Escape'});
            expect(screen.getByRole('status')).toHaveTextContent('Verschieben von „a“ abgebrochen.');
            expect(onChange).not.toHaveBeenCalled();
            expect(screen.queryByText(/wird verschoben/)).not.toBeInTheDocument();
        });

        it.each([undefined, {notes: 'Interne Notiz'}])('announces when a node has no target with metadata %j', (editorMetadata) => {
            render(<Harness initial={{
                ...expression,
                editorMetadata,
            }}
                            onChange={vi.fn()}/>);
            const root = screen.getByRole('treeitem', {name: 'Bedingung: Operator Ist Gleich, Wahrheitswert'});
            root.focus();

            fireEvent.keyDown(root, {key: ' '});

            expect(screen.getByRole('status')).toHaveTextContent('Für „Ist Gleich“ ist kein Ziel zum Verschieben verfügbar.');
        });

        it('moves a node down with the context menu and returns focus to it', async () => {
            const onChange = vi.fn();
            render(<Harness onChange={onChange}
                            initial={nested}/>);
            const leaf = screen.getByRole('treeitem', {name: 'Wert 1: a, Beliebiger Wert'});
            leaf.focus();

            fireEvent.keyDown(leaf, {
                key: 'F10',
                shiftKey: true,
            });
            expect(await screen.findByRole('menuitem', {name: 'Nach oben verschieben'})).toHaveAttribute('aria-disabled', 'true');
            fireEvent.click(screen.getByRole('menuitem', {name: 'Nach unten verschieben'}));

            expect(onChange).toHaveBeenLastCalledWith({
                ...nested,
                operands: [{
                    type: 'NoCodeExpression',
                    operatorIdentifier: 'not',
                    operands: [null],
                }, a],
            });
            await waitFor(() => expect(screen.getByRole('treeitem', {name: 'Wert 2: a, Beliebiger Wert'})).toHaveFocus());
            expect(screen.getByRole('status')).toHaveTextContent('„a“ wurde verschoben.');
        });

        it('moves a node by clicking a target, without dragging', async () => {
            const onChange = vi.fn();
            render(<Harness onChange={onChange}
                            initial={nested}/>);

            fireEvent.click(screen.getByRole('button', {name: 'Aktionen für „a“'}));
            fireEvent.click(await screen.findByRole('menuitem', {name: 'An andere Stelle verschieben'}));
            await waitFor(() => expect(screen.queryByRole('menu')).not.toBeInTheDocument());
            expect(screen.getByText('„a“ wird verschoben. Wählen Sie ein markiertes Ziel aus.')).toBeInTheDocument();

            fireEvent.click(screen.getByRole('button', {name: 'Wert festlegen'}));

            expect(onChange).toHaveBeenLastCalledWith({
                ...nested,
                operands: [null, {
                    type: 'NoCodeExpression',
                    operatorIdentifier: 'not',
                    operands: [a],
                }],
            });
            // The click picked the target instead of opening the socket's type selector.
            expect(screen.queryByRole('button', {name: 'Fester Wert'})).not.toBeInTheDocument();
        });

        it('moves a node by dragging it with a pointer onto a socket', async () => {
            const onChange = vi.fn();
            render(<Harness onChange={onChange}
                            initial={nested}/>);
            const row = (name: string) => screen.getByRole('treeitem', {name}).querySelector('[data-tree-row]')!;
            const dataTransfer = new FakeDataTransfer();
            const source = screen.getByRole('button', {name: 'a, Beliebiger Wert'});
            const target = row('Wert: leer, Wahrheitswert');

            fireEvent.dragStart(source, {dataTransfer});
            fireEvent.dragEnter(target, {dataTransfer});
            fireEvent.dragOver(target, {dataTransfer});
            expect(dataTransfer.dropEffect).toBe('move');
            fireEvent.drop(target, {dataTransfer});
            fireEvent.dragEnd(source, {dataTransfer});

            await waitFor(() => expect(onChange).toHaveBeenLastCalledWith({
                ...nested,
                operands: [null, {
                    type: 'NoCodeExpression',
                    operatorIdentifier: 'not',
                    operands: [a],
                }],
            }));
        });

        it('rejects dragging an operator into its own parameters', () => {
            render(<Harness onChange={vi.fn()}
                            initial={nested}/>);
            const row = (name: string) => screen.getByRole('treeitem', {name}).querySelector('[data-tree-row]')!;
            const dataTransfer = new FakeDataTransfer();

            fireEvent.dragStart(screen.getByRole('button', {name: 'Nicht, Wahrheitswert'}), {dataTransfer});
            fireEvent.dragEnter(row('Wert: leer, Wahrheitswert'), {dataTransfer});
            fireEvent.dragOver(row('Wert: leer, Wahrheitswert'), {dataTransfer});

            expect(dataTransfer.dropEffect).toBe('none');
        });
    });
});

describe('adding a parent operator', () => {
    async function wrap(item: HTMLElement) {
        item.focus();
        fireEvent.keyDown(item, {
            key: 'F10',
            shiftKey: true,
        });
        fireEvent.click(await screen.findByRole('menuitem', {name: 'Übergeordneten Operator hinzufügen'}));
        return screen.findByRole('dialog', {name: 'Operator auswählen'});
    }

    it('makes a value the first parameter of the chosen operator and focuses the new operator', async () => {
        const onChange = vi.fn();
        render(<Harness onChange={onChange}
                        initial={nested}/>);

        const dialog = await wrap(screen.getByRole('treeitem', {name: 'Wert 1: a, Beliebiger Wert'}));
        fireEvent.click(within(dialog).getByRole('button', {name: 'Nicht: Hinzufügen'}));

        expect(onChange).toHaveBeenLastCalledWith({
            ...nested,
            operands: [{
                type: 'NoCodeExpression',
                operatorIdentifier: 'not',
                operands: [a],
            }, nested.operands![1]],
        });
        await waitFor(() => expect(screen.getByRole('treeitem', {name: 'Wert 1: Operator Nicht, Wahrheitswert'})).toHaveFocus());
        // Announced once the dialog no longer hides the page from screen readers.
        expect(screen.getByRole('status')).toHaveTextContent('Operator „Nicht“ hinzugefügt. „a“ ist jetzt sein erster Parameter.');
        expect(screen.getByRole('treeitem', {name: 'Wert: a, Wahrheitswert'})).toBeInTheDocument();
    });

    it('wraps the root operator and offers only operators that fit the slot and take a parameter', async () => {
        const onChange = vi.fn();
        render(<Harness onChange={onChange}
                        initial={nested}/>);

        const dialog = await wrap(screen.getByRole('treeitem', {name: 'Bedingung: Operator Ist Gleich, Wahrheitswert'}));
        expect(within(dialog).queryByRole('button', {name: 'Addiere: Hinzufügen'})).not.toBeInTheDocument();
        expect(within(dialog).queryByRole('button', {name: 'Immer wahr: Hinzufügen'})).not.toBeInTheDocument();
        fireEvent.click(within(dialog).getByRole('button', {name: 'Nicht: Hinzufügen'}));

        expect(onChange).toHaveBeenLastCalledWith({
            type: 'NoCodeExpression',
            operatorIdentifier: 'not',
            operands: [nested],
        });
        await waitFor(() => expect(screen.getByRole('treeitem', {name: 'Bedingung: Operator Nicht, Wahrheitswert'})).toHaveFocus());
    });

    it.each(['Escape', 'Schließen'])('returns focus and preserves the expression when cancelling with %s', async (method) => {
        const onChange = vi.fn();
        render(<Harness onChange={onChange}
                        initial={nested}/>);
        const leaf = screen.getByRole('treeitem', {name: 'Wert 1: a, Beliebiger Wert'});

        const dialog = await wrap(leaf);
        if (method === 'Escape') fireEvent.keyDown(dialog, {key: 'Escape'});
        else fireEvent.click(within(dialog).getByRole('button', {name: 'Schließen'}));

        await waitFor(() => expect(leaf).toHaveFocus());
        expect(onChange).not.toHaveBeenCalled();
    });
});

describe('NoCodeEditor types and dry run', () => {
    // equals(add(1, empty), not(empty))
    const typed: NoCodeOperand = {
        type: 'NoCodeExpression',
        operatorIdentifier: 'equals',
        operands: [
            {
                type: 'NoCodeExpression',
                operatorIdentifier: 'add',
                operands: [{
                    type: 'NoCodeStaticValue',
                    value: '1',
                }, null],
            },
            {
                type: 'NoCodeExpression',
                operatorIdentifier: 'not',
                operands: [null],
            },
        ],
    };

    it('warns about values the backend cannot convert', () => {
        render(<Harness onChange={vi.fn()}
                        initial={{
                            type: 'NoCodeExpression',
                            operatorIdentifier: 'add',
                            operands: [{
                                type: 'NoCodeExpression',
                                operatorIdentifier: 'not',
                                operands: [null],
                            }, {
                                type: 'NoCodeStaticValue',
                                value: 'zwei',
                            }],
                        }}/>);

        expect(screen.getByRole('treeitem', {name: 'Summand 1: Operator Nicht, Wahrheitswert'}))
            .toHaveAccessibleDescription('Typkonflikt: Erwartet wird „Zahlenwert“, geliefert wird „Wahrheitswert“.');
        expect(screen.getByRole('treeitem', {name: 'Summand 2: zwei, Zahlenwert'}))
            .toHaveAccessibleDescription('Typkonflikt: Erwartet wird „Zahlenwert“, „zwei“ lässt sich nicht umwandeln.');
    });

    it('offers only type-compatible targets when moving', () => {
        render(<Harness onChange={vi.fn()}
                        initial={typed}/>);
        const negation = screen.getByRole('treeitem', {name: 'Wert 2: Operator Nicht, Wahrheitswert'});
        negation.focus();

        fireEvent.keyDown(negation, {key: ' '});
        fireEvent.keyDown(negation, {key: 'ArrowDown'});
        fireEvent.keyDown(negation, {key: 'ArrowDown'});

        // The empty number slot of "Addiere" and the swap with "1" would put a truth value into a number.
        expect(screen.getByRole('status')).toHaveTextContent('Ziel: Wert 1: Operator Addiere, Zahlenwert, die Werte werden getauscht.');
        expect(screen.getByRole('treeitem', {name: 'Summand 2: leer, Zahlenwert'}).querySelector('[data-tree-row]'))
            .not.toHaveStyle({outlineStyle: 'dashed'});
    });

    it('rejects pointer drops of incompatible types', () => {
        render(<Harness onChange={vi.fn()}
                        initial={typed}/>);
        const row = (name: string) => screen.getByRole('treeitem', {name}).querySelector('[data-tree-row]')!;
        const dataTransfer = new FakeDataTransfer();

        fireEvent.dragStart(screen.getByRole('button', {name: 'Nicht, Wahrheitswert'}), {dataTransfer});
        fireEvent.dragEnter(row('Summand 2: leer, Zahlenwert'), {dataTransfer});
        fireEvent.dragOver(row('Summand 2: leer, Zahlenwert'), {dataTransfer});

        expect(dataTransfer.dropEffect).toBe('none');
    });

    it('runs the expression with test data and shows the results at the nodes', async () => {
        api.post.mockResolvedValue({
            status: NoCodeDryRunStatus.Skipped,
            value: null,
            message: 'Ein Parameter konnte nicht ausgewertet werden.',
            operands: [
                {
                    status: NoCodeDryRunStatus.Evaluated,
                    value: 43,
                    message: null,
                    operands: [
                        {
                            status: NoCodeDryRunStatus.Evaluated,
                            value: 42,
                            message: null,
                            operands: [],
                        },
                        {
                            status: NoCodeDryRunStatus.Evaluated,
                            value: '1',
                            message: null,
                            operands: [],
                        },
                    ],
                },
                {
                    status: NoCodeDryRunStatus.Failed,
                    value: null,
                    message: 'Division durch Null ist nicht erlaubt',
                    operands: [],
                },
            ],
        });
        render(<Harness onChange={vi.fn()}
                        initial={{
                            type: 'NoCodeExpression',
                            operatorIdentifier: 'equals',
                            operands: [
                                {
                                    type: 'NoCodeExpression',
                                    operatorIdentifier: 'add',
                                    operands: [{
                                        type: 'NoCodeReference',
                                        elementId: 'amount',
                                    }, {
                                        type: 'NoCodeStaticValue',
                                        value: '1',
                                    }],
                                },
                                {
                                    type: 'NoCodeExpression',
                                    operatorIdentifier: 'not',
                                    operands: [{
                                        type: 'NoCodeProcessDataReference',
                                        path: 'ok',
                                    }],
                                },
                            ],
                        }}/>);

        fireEvent.click(screen.getByRole('button', {name: 'Testlauf'}));
        fireEvent.change(screen.getByRole('textbox', {name: /^Testwert für „Betrag“/}), {target: {value: '42'}});
        fireEvent.click(within(screen.getByRole('group', {name: 'Eingabeformat für Vorgangsdaten'})).getByRole('button', {name: 'JSON'}));
        fireEvent.change(screen.getByRole('textbox', {name: /^Vorgangsdaten \(JSON\)/}), {target: {value: '{"ok": true}'}});
        fireEvent.click(screen.getByRole('button', {name: 'Testlauf'}));
        fireEvent.click(screen.getByRole('button', {name: 'Testlauf'}));
        expect(screen.getByRole('textbox', {name: /^Testwert für „Betrag“/})).toHaveValue('42');
        expect(screen.getByRole('textbox', {name: /^Vorgangsdaten \(JSON\)/})).toHaveValue('{"ok": true}');
        expect(api.post).not.toHaveBeenCalled();
        fireEvent.click(screen.getByRole('button', {name: 'Test ausführen'}));

        await waitFor(() => expect(api.post).toHaveBeenCalledWith('no-code/dry-run/', expect.objectContaining({
            elementData: {
                effectiveValues: {amount: 42},
                elementStates: {},
            },
            processData: {ok: true},
            instanceData: null,
            nodeData: null,
        }), {abortController: expect.any(AbortController)}));
        expect(api.post.mock.lastCall![1].expression).not.toHaveProperty('id');

        expect(await screen.findByRole('treeitem', {name: 'Wert 1: Operator Addiere, Zahlenwert'}))
            .toHaveAccessibleDescription('Testergebnis: 43');
        expect(screen.getByRole('treeitem', {name: 'Summand 1: Betrag, Zahlenwert'})).toHaveAccessibleDescription('Testergebnis: 42');
        expect(screen.getByText('Fehler: Division durch Null ist nicht erlaubt')).toBeInTheDocument();
        expect(screen.getByRole('treeitem', {name: 'Bedingung: Operator Ist Gleich, Wahrheitswert'}))
            .toHaveAccessibleDescription('Nicht ausgewertet: Ein Parameter konnte nicht ausgewertet werden.');
    });

    it('does not run while the JSON test data is invalid', async () => {
        render(<Harness onChange={vi.fn()}
                        initial={{
                            type: 'NoCodeProcessDataReference',
                            path: 'ok',
                        }}/>);

        fireEvent.click(screen.getByRole('button', {name: 'Testlauf'}));
        fireEvent.click(within(screen.getByRole('group', {name: 'Eingabeformat für Vorgangsdaten'})).getByRole('button', {name: 'JSON'}));
        fireEvent.change(screen.getByRole('textbox', {name: /^Vorgangsdaten \(JSON\)/}), {target: {value: '[1'}});
        fireEvent.click(screen.getByRole('button', {name: 'Test ausführen'}));

        expect(await screen.findByText('Geben Sie ein JSON-Objekt ein, zum Beispiel {"betrag": 120}.')).toBeInTheDocument();
        await new Promise((resolve) => setTimeout(resolve, 500));
        expect(api.post).not.toHaveBeenCalled();
    });
});

/** jsdom has no DataTransfer; react-aria only needs typed string items. */
class FakeDataTransfer {
    private readonly data = new Map<string, string>();
    effectAllowed = 'all';
    dropEffect = 'none';
    readonly items = Object.assign([] as { kind: string; type: string }[], {
        add: (value: string, type: string) => {
            this.data.set(type, value);
            this.items.push({
                kind: 'string',
                type,
            });
        },
    });

    get types() {
        return [...this.data.keys()];
    }

    getData(type: string) {
        return this.data.get(type) ?? '';
    }

    setData(type: string, value: string) {
        this.data.set(type, value);
    }

    clearData() {
        this.data.clear();
        this.items.length = 0;
    }

    setDragImage() {
    }
}

describe('operator sentences and descriptions', () => {
    const sentenceEquals: NoCodeOperatorDetailsDTO = {
        ...equals,
        sentenceLead: 'ergibt wahr',
        abstractDescription: 'Prüft, ob zwei Werte gleich sind.',
        description: 'Vergleicht **beide Werte** miteinander.',
        signatures: [{
            returnType: NoCodeDataType.Boolean,
            parameters: [
                {
                    type: NoCodeDataType.Runtime,
                    label: 'Wert 1',
                    description: null,
                    sentencePhrase: 'wenn',
                    options: [],
                },
                {
                    type: NoCodeDataType.Runtime,
                    label: 'Wert 2',
                    description: null,
                    sentencePhrase: 'gleich',
                    sentenceSuffix: 'ist',
                    options: [],
                },
            ],
        }],
    };
    const operatorItemName = 'Bedingung: Operator Ist Gleich, Wahrheitswert';
    const described: NoCodeOperatorDetailsDTO = {
        ...sentenceEquals,
        signatures: [{
            returnType: NoCodeDataType.Boolean,
            parameters: [
                {
                    ...sentenceEquals.signatures[0].parameters[0],
                    description: 'Der erste Vergleichswert.',
                },
                {
                    ...sentenceEquals.signatures[0].parameters[1],
                    description: 'Der zweite Vergleichswert.',
                },
            ],
        }],
    };

    it('reads as a sentence while retaining complete accessible parameter names', () => {
        render(<Harness onChange={vi.fn()}
                        operators={[sentenceEquals]}/>);

        const operatorItem = screen.getByRole('treeitem', {name: operatorItemName});
        const second = screen.getByRole('treeitem', {name: 'Wert 2: b, Beliebiger Wert'});
        expect(operatorItem.firstElementChild).toHaveTextContent('Ist Gleich');
        expect(operatorItem.firstElementChild).toHaveTextContent(/Ist Gleich.*ergibt wahr/);
        expect(screen.getByRole('treeitem', {name: 'Wert 1: a, Beliebiger Wert'})).toHaveTextContent(/^wenn/);
        expect(second).toHaveTextContent(/^gleich.*b.*ist$/);
        expect(screen.queryByText('Ergebnistyp: Wahrheitswert')).not.toBeInTheDocument();
        expect(screen.queryByText('Wert 1')).not.toBeInTheDocument();
        expect(screen.queryByText('Vorschau')).not.toBeInTheDocument();
    });

    it('closes a sentence after the nested operands and preserves empty sentence phrases', () => {
        const concat: NoCodeOperatorDetailsDTO = {
            ...equals,
            identifier: 'concat',
            label: 'Text verketten',
            sentenceLead: 'ergibt',
            signatures: [{
                returnType: NoCodeDataType.String,
                parameters: [
                    {
                        type: NoCodeDataType.String,
                        label: 'Text 1',
                        description: null,
                        sentencePhrase: '',
                        options: [],
                    },
                    {
                        type: NoCodeDataType.String,
                        label: 'Text 2',
                        description: null,
                        sentencePhrase: 'gefolgt von',
                        options: [],
                    },
                ],
            }],
        };
        render(<Harness onChange={vi.fn()}
                        operators={[sentenceEquals, concat]}
                        initial={{
                            type: 'NoCodeExpression',
                            operatorIdentifier: 'equals',
                            operands: [a, {
                                type: 'NoCodeExpression',
                                operatorIdentifier: 'concat',
                                operands: [a, {
                                    type: 'NoCodeStaticValue',
                                    value: 'b',
                                }],
                            }],
                        }}/>);
        const nestedItem = screen.getByRole('treeitem', {name: 'Wert 2: Operator Text verketten, Textwert'});
        expect(nestedItem).toHaveTextContent(/^gleich.*Text verketten.*ergibt.*a.*gefolgt von.*b.*ist$/);
        expect(screen.queryByText('Text 1')).not.toBeInTheDocument();
        expect(screen.getByRole('treeitem', {name: 'Text 1: a, Textwert'})).toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', {name: 'Text verketten: zuklappen'}));
        expect(nestedItem).toHaveTextContent(/ist$/);
    });

    it('collapses into a sentence continuation without repeating the operator template', () => {
        render(<Harness onChange={vi.fn()}
                        operators={[{
                            ...sentenceEquals,
                            humanReadableTemplate: 'vergleiche #0 mit #1',
                        }]}/>);
        fireEvent.click(screen.getByRole('button', {name: 'Ist Gleich: zuklappen'}));
        const root = screen.getByRole('treeitem', {name: operatorItemName});
        expect(root).toHaveTextContent('ergibt wahr');
        expect(root).not.toHaveTextContent('vergleiche');
        expect(root).toHaveTextContent('wenn „a“ gleich „b“ ist');
        expect(screen.queryByRole('treeitem', {name: 'Wert 1: a, Beliebiger Wert'})).not.toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', {name: 'Ist Gleich: aufklappen'}));
        expect(screen.getByRole('treeitem', {name: 'Wert 1: a, Beliebiger Wert'})).toBeInTheDocument();
    });

    it('shows the parameter label when an operator has no sentence parts', () => {
        render(<Harness onChange={vi.fn()}/>);

        expect(screen.getByText('Wert 1')).toBeInTheDocument();
        expect(screen.queryByText('ergibt wahr')).not.toBeInTheDocument();
    });

    it('shows the summary on hover and the description on request', async () => {
        render(<Harness onChange={vi.fn()}
                        operators={[sentenceEquals]}/>);

        fireEvent.mouseOver(screen.getByRole('button', {name: 'Ist Gleich, Wahrheitswert'}));
        const tooltip = await screen.findByRole('tooltip');
        expect(tooltip).toHaveTextContent('Prüft, ob zwei Werte gleich sind.');
        fireEvent.click(within(tooltip).getByRole('button', {name: 'Mehr...'}));

        const dialog = await screen.findByRole('dialog', {name: 'Ist Gleich'});
        expect(within(dialog).getByText('beide Werte').tagName).toBe('STRONG');
    });

    it('opens the description from the action menu and returns focus to the operator', async () => {
        render(<Harness onChange={vi.fn()}
                        operators={[sentenceEquals]}/>);
        const operatorItem = screen.getByRole('treeitem', {name: operatorItemName});

        operatorItem.focus();
        fireEvent.keyDown(operatorItem, {
            key: 'F10',
            shiftKey: true,
        });
        fireEvent.click(await screen.findByRole('menuitem', {name: 'Beschreibung anzeigen'}));
        const dialog = await screen.findByRole('dialog', {name: 'Ist Gleich'});
        fireEvent.keyDown(dialog, {key: 'Escape'});

        await waitFor(() => expect(operatorItem).toHaveFocus());
    });

    it('retains all menu entries during exit before opening a description with subordinate headings', async () => {
        render(<Harness onChange={vi.fn()}
                        operators={[{
                            ...sentenceEquals,
                            description: '# Beschreibung\n\n## Beispiel',
                        }]}/>);
        const operatorItem = screen.getByRole('treeitem', {name: operatorItemName});
        fireEvent.keyDown(operatorItem, {
            key: 'F10',
            shiftKey: true,
        });
        const menu = await screen.findByRole('menu');
        const entries = within(menu).getAllByRole('menuitem').map(item => item.textContent);
        expect(within(menu).getByRole('separator')).toBeInTheDocument();

        fireEvent.click(within(menu).getByRole('menuitem', {name: 'Beschreibung anzeigen'}));

        expect(menu).toBeInTheDocument();
        expect(within(menu).getAllByRole('menuitem', {hidden: true}).map(item => item.textContent)).toEqual(entries);
        const dialog = await screen.findByRole('dialog', {name: 'Ist Gleich'});
        expect(menu).not.toBeInTheDocument();
        expect(within(dialog).getByRole('heading', {
            name: 'Ist Gleich',
            level: 2,
        })).toBeInTheDocument();
        expect(within(dialog).getByRole('heading', {
            name: 'Beschreibung',
            level: 3,
        })).toBeInTheDocument();
        expect(within(dialog).getByRole('heading', {
            name: 'Beispiel',
            level: 4,
        })).toBeInTheDocument();
    });

    it('describes a value and the parameter it fills', async () => {
        render(<Harness onChange={vi.fn()}
                        operators={[described]}/>);

        fireEvent.mouseOver(screen.getByRole('button', {name: 'a, Beliebiger Wert'}));
        const tooltip = await screen.findByRole('tooltip');
        expect(tooltip).toHaveTextContent('Ergibt den eingegebenen Wert „a“.');
        expect(tooltip).toHaveTextContent('Wert 1: Der erste Vergleichswert.');
        fireEvent.click(within(tooltip).getByRole('button', {name: 'Mehr...'}));

        const dialog = await screen.findByRole('dialog', {name: 'Fester Wert'});
        expect(dialog).toHaveTextContent('einen konstanten Wert direkt in den Ausdruck einzufügen');
    });

    it.each([[0, '0'], [false, 'Falsch']] as const)('does not describe a backend literal %s as empty', async (value, label) => {
        // Backend-authored literals (for example from XDF) are not restricted to the editor's text inputs.
        const initial = {
            type: 'NoCodeStaticValue',
            value,
        } as unknown as NoCodeOperand;
        render(<Harness initial={initial}
                        returnType={NoCodeDataType.Runtime}
                        onChange={vi.fn()}/>);
        fireEvent.mouseOver(screen.getByRole('button', {name: `${label}, Beliebiger Wert`}));
        expect(await screen.findByRole('tooltip')).toHaveTextContent(`Ergibt den eingegebenen Wert „${label}“.`);
    });

    it('names the referenced element', async () => {
        const reference: NoCodeOperand = {
            ...expression,
            operands: [{
                type: 'NoCodeReference',
                elementId: 'amount',
            }, a],
        };
        render(<Harness onChange={vi.fn()}
                        operators={[described]}
                        initial={reference}/>);

        fireEvent.mouseOver(screen.getByRole('button', {name: /^Betrag, /}));

        expect(await screen.findByRole('tooltip')).toHaveTextContent('Ergibt den aktuellen Wert des Elements „Betrag“.');
    });

    it('describes what belongs into an empty parameter', async () => {
        render(<Harness onChange={vi.fn()}
                        operators={[described]}
                        initial={{
                            ...expression,
                            operands: [a, null],
                        }}/>);

        fireEvent.mouseOver(screen.getByRole('button', {name: 'Wert 2 festlegen'}));
        const tooltip = await screen.findByRole('tooltip');

        expect(tooltip).toHaveTextContent('Wert 2: Der zweite Vergleichswert.');
        expect(within(tooltip).queryByRole('button', {name: 'Mehr...'})).not.toBeInTheDocument();
    });

    it('opens the description of a value from the action menu', async () => {
        render(<Harness onChange={vi.fn()}
                        operators={[described]}/>);
        const leaf = screen.getByRole('treeitem', {name: 'Wert 1: a, Beliebiger Wert'});

        leaf.focus();
        fireEvent.keyDown(leaf, {
            key: 'F10',
            shiftKey: true,
        });
        fireEvent.click(await screen.findByRole('menuitem', {name: 'Beschreibung anzeigen'}));
        const dialog = await screen.findByRole('dialog', {name: 'Fester Wert'});
        fireEvent.keyDown(dialog, {key: 'Escape'});

        await waitFor(() => expect(leaf).toHaveFocus());
    });
});


it('renders and edits a deeply nested stored expression without changing its siblings', () => {
    let stored: NoCodeOperand = {
        type: 'NoCodeStaticValue',
        value: 'deep',
    };
    for (let i = 0; i < 31; i++) stored = {
        type: 'NoCodeExpression',
        operatorIdentifier: 'not',
        operands: [stored],
    };
    const onChange = vi.fn();
    const start = performance.now();
    render(<Harness initial={stored}
                    onChange={onChange}/>);
    expect(screen.getAllByRole('treeitem')).toHaveLength(32);
    expect(onChange).not.toHaveBeenCalled();
    fireEvent.keyDown(screen.getByRole('treeitem', {name: 'Wert: deep, Wahrheitswert'}), {key: 'Delete'});
    expect(onChange).toHaveBeenCalledOnce();
    fireEvent.click(screen.getByRole('button', {name: 'Rückgängig'}));
    expect(onChange).toHaveBeenLastCalledWith(stored);
    expect(performance.now() - start).toBeLessThan(10000);
});

it('renders a broad expression with 255 nodes and retains every stored operand', () => {
    const tree = (depth: number): NoCodeOperand => depth === 0 ? {
            type: 'NoCodeStaticValue',
            value: 'value',
        }
        : {
            type: 'NoCodeExpression',
            operatorIdentifier: 'equals',
            operands: [tree(depth - 1), tree(depth - 1)],
        };
    const onChange = vi.fn();
    const start = performance.now();
    render(<Harness initial={tree(7)}
                    onChange={onChange}/>);
    expect(screen.getAllByRole('treeitem')).toHaveLength(255);
    expect(onChange).not.toHaveBeenCalled();
    expect(performance.now() - start).toBeLessThan(10000);
});

it('keeps an entire selected direct subtree, restores it with undo and cancels without changes', async () => {
    const onChange = vi.fn();
    render(<Harness initial={{
        type: 'NoCodeExpression',
        operatorIdentifier: 'equals',
        operands: [nested, a],
    }}
                    onChange={onChange}/>);
    const root = screen.getByRole('treeitem', {name: 'Bedingung: Operator Ist Gleich, Wahrheitswert'});
    root.focus();
    fireEvent.keyDown(root, {key: 'Delete'});
    const dialog = screen.getByRole('dialog', {name: 'Operator entfernen?'});
    expect(onChange).not.toHaveBeenCalled();
    expect(within(dialog).getByRole('button', {name: 'Durch Teilausdruck ersetzen'})).toBeDisabled();
    fireEvent.keyDown(dialog, {key: 'Escape'});
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(root).toHaveFocus();
    expect(onChange).not.toHaveBeenCalled();
    fireEvent.keyDown(root, {key: 'Backspace'});
    fireEvent.mouseDown(screen.getByRole('combobox', {name: /^Teilausdruck erhalten/}));
    fireEvent.click(screen.getByRole('option', {name: /^Wert 1:/}));
    fireEvent.click(screen.getByRole('button', {name: 'Durch Teilausdruck ersetzen'}));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(onChange).toHaveBeenLastCalledWith(nested);
    expect(screen.getByRole('treeitem', {name: 'Bedingung: Operator Ist Gleich, Wahrheitswert'})).toHaveFocus();
    fireEvent.click(screen.getByRole('button', {name: 'Rückgängig'}));
    expect(onChange).toHaveBeenLastCalledWith({
        type: 'NoCodeExpression',
        operatorIdentifier: 'equals',
        operands: [nested, a],
    });
});

it('offers the only child directly and retains its value despite a type warning', async () => {
    const onChange = vi.fn();
    render(<Harness returnType={NoCodeDataType.Number}
                    initial={{
                        type: 'NoCodeExpression',
                        operatorIdentifier: 'not',
                        operands: [a],
                    }}
                    onChange={onChange}/>);
    fireEvent.click(screen.getByRole('button', {name: 'Operator „Nicht“ löschen'}));
    const dialog = screen.getByRole('dialog');
    expect(within(dialog).getByRole('alert')).toHaveTextContent(/Zahlenwert/);
    fireEvent.click(within(dialog).getByRole('button', {name: 'Durch Teilausdruck ersetzen'}));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(onChange).toHaveBeenLastCalledWith(a);
    expect(screen.getByRole('treeitem', {name: 'Bedingung: a, Zahlenwert'})).toHaveFocus();
});

it('uses the same deletion choice from the action menu', async () => {
    const onChange = vi.fn();
    render(<Harness onChange={onChange}/>);
    const root = screen.getByRole('treeitem', {name: 'Bedingung: Operator Ist Gleich, Wahrheitswert'});
    fireEvent.keyDown(root, {
        key: 'F10',
        shiftKey: true,
    });
    fireEvent.click(await screen.findByRole('menuitem', {name: 'Löschen'}));
    const dialog = await screen.findByRole('dialog', {name: 'Operator entfernen?'});
    expect(onChange).not.toHaveBeenCalled();
    fireEvent.click(within(dialog).getByRole('button', {name: 'Alles löschen'}));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(onChange).toHaveBeenLastCalledWith(null);
    expect(screen.getByRole('treeitem', {name: 'Bedingung: leer, Wahrheitswert'})).toHaveFocus();
});

it('collapses without modifying the draft and expands the first invalid parameter on an explicit test', () => {
    const onChange = vi.fn();
    render(<Harness initial={nested}
                    onChange={onChange}/>);
    const child = screen.getByRole('treeitem', {name: 'Wert 2: Operator Nicht, Wahrheitswert'});
    expect(screen.queryByText('Legen Sie einen Ausdruck fest.')).not.toBeInTheDocument();
    fireEvent.keyDown(child, {key: 'ArrowLeft'});
    expect(child).toHaveAttribute('aria-expanded', 'false');
    expect(screen.queryByRole('treeitem', {name: 'Wert: leer, Wahrheitswert'})).not.toBeInTheDocument();
    fireEvent.keyDown(child, {key: 'ArrowRight'});
    expect(child).toHaveAttribute('aria-expanded', 'true');
    fireEvent.click(screen.getByRole('button', {name: 'Nicht: zuklappen'}));
    fireEvent.click(screen.getByRole('button', {name: 'Testlauf'}));
    expect(screen.queryByText('Legen Sie einen Ausdruck fest.')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', {name: 'Test ausführen'}));
    expect(child).toHaveAttribute('aria-expanded', 'true');
    const empty = screen.getByRole('treeitem', {name: 'Wert: leer, Wahrheitswert'});
    expect(empty).toHaveAttribute('aria-invalid', 'true');
    expect(empty).toHaveFocus();
    expect(api.post).not.toHaveBeenCalled();
    expect(onChange).not.toHaveBeenCalled();
});

it('shows an incomplete reference error only after leaving its value editor', async () => {
    render(<Harness initial={{
        type: 'NoCodeProcessDataReference',
        path: '',
    }}
                    onChange={vi.fn()}/>);
    const item = screen.getByRole('treeitem');
    expect(item).not.toHaveAttribute('aria-invalid', 'true');
    fireEvent.keyDown(item, {key: 'Enter'});
    const input = await screen.findByRole('textbox');
    expect(input).not.toHaveAttribute('aria-invalid', 'true');
    fireEvent.blur(input);
    expect(input).toHaveAttribute('aria-invalid', 'true');
    fireEvent.keyDown(input, {key: 'Escape'});
    await waitFor(() => expect(screen.queryByRole('textbox')).not.toBeInTheDocument());
    expect(item).toHaveAttribute('aria-invalid', 'true');
    expect(item).toHaveAccessibleDescription(/Geben Sie einen Datenpfad/);
});

it('does not mark newly inserted operator parameters as invalid after an earlier test attempt', async () => {
    render(<Harness initial={null}
                    onChange={vi.fn()}/>);
    fireEvent.click(screen.getByRole('button', {name: 'Testlauf'}));
    fireEvent.click(screen.getByRole('button', {name: 'Test ausführen'}));
    expect(screen.getByRole('treeitem')).toHaveAttribute('aria-invalid', 'true');
    fireEvent.click(screen.getByRole('button', {name: 'Ausdruck hinzufügen'}));
    fireEvent.click(screen.getByRole('button', {name: 'Operator'}));
    const picker = await screen.findByRole('dialog', {name: 'Operator auswählen'});
    fireEvent.click(within(picker).getByRole('button', {name: 'Nicht: Hinzufügen'}));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    const parameter = screen.getByRole('treeitem', {name: 'Wert: leer, Wahrheitswert'});
    expect(parameter).not.toHaveAttribute('aria-invalid', 'true');
    expect(screen.queryByText('Legen Sie einen Ausdruck fest.')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', {name: 'Test ausführen'}));
    expect(screen.getByRole('treeitem', {name: 'Wert: leer, Wahrheitswert'})).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByRole('treeitem', {name: 'Wert: leer, Wahrheitswert'})).toHaveFocus();
});


it('leaves Ctrl/Cmd+Z and Shift+Z unhandled while history buttons remain available', () => {
    const onChange = vi.fn();
    render(<Harness onChange={onChange}/>);
    fireEvent.click(screen.getByRole('button', {name: 'Festen Wert „a“ löschen'}));
    const item = screen.getByRole('treeitem', {name: 'Wert 1: leer, Beliebiger Wert'});
    const calls = onChange.mock.calls.length;
    for (const modifiers of [{ctrlKey: true}, {metaKey: true}, {
        ctrlKey: true,
        shiftKey: true,
    }, {
        metaKey: true,
        shiftKey: true,
    }]) {
        expect(fireEvent.keyDown(item, {key: 'z', ...modifiers})).toBe(true);
    }
    expect(onChange).toHaveBeenCalledTimes(calls);
    expect(screen.getByRole('tree')).not.toHaveAccessibleDescription(/Strg|Befehl/);
    fireEvent.click(screen.getByRole('button', {name: 'Rückgängig'}));
    expect(onChange).toHaveBeenLastCalledWith(expression);
});

it.each([false, true])('opens the test area with reduced motion %s and keeps the expression editable', (reducedMotion) => {
    const scrollTo = vi.fn();
    const original = HTMLElement.prototype.scrollTo;
    HTMLElement.prototype.scrollTo = scrollTo;
    vi.stubGlobal('matchMedia', () => ({matches: reducedMotion}));
    try {
        render(<Harness onChange={vi.fn()}/>);
        const toggle = screen.getByRole('button', {name: 'Testlauf'});
        expect(toggle).toHaveAttribute('aria-expanded', 'false');
        expect(toggle.parentElement).toContainElement(screen.getByRole('button', {name: 'Notizen'}));
        fireEvent.click(toggle);
        expect(toggle).toHaveAttribute('aria-expanded', 'true');
        expect(document.getElementById(toggle.getAttribute('aria-controls')!))
            .toContainElement(screen.getByRole('button', {name: 'Test ausführen'}));
        expect(screen.getByRole('tree')).toHaveAttribute('aria-readonly', 'false');
        expect(screen.getByRole('button', {name: 'Festen Wert „a“ löschen'})).toBeEnabled();
        expect(scrollTo).toHaveBeenCalledOnce();
        expect(scrollTo).toHaveBeenCalledWith({
            top: expect.any(Number),
            behavior: reducedMotion ? 'instant' : 'smooth',
        });
        expect(screen.queryByRole('tab')).not.toBeInTheDocument();
        expect(api.post).not.toHaveBeenCalled();
    } finally {
        HTMLElement.prototype.scrollTo = original;
        vi.unstubAllGlobals();
    }
});


it('starts pointer dragging only on pills, not on sentence text, row space or actions', () => {
    const onChange = vi.fn();
    render(<Harness initial={nested}
                    onChange={onChange}/>);
    const source = screen.getByRole('button', {name: 'a, Beliebiger Wert'});
    expect(source).toHaveAttribute('draggable', 'true');
    const row = screen.getByRole('treeitem', {name: 'Wert 1: a, Beliebiger Wert'}).querySelector('[data-tree-row]')!;
    const target = screen.getByRole('treeitem', {name: 'Wert: leer, Wahrheitswert'}).querySelector('[data-tree-row]')!;
    for (const surface of [row, screen.getByText('Wert 1'), screen.getByRole('button', {name: 'Festen Wert „a“ löschen'}), screen.getByRole('button', {name: 'Aktionen für „a“'})]) {
        expect(surface.closest('[draggable="true"]')).toBeNull();
        const dataTransfer = new FakeDataTransfer();
        fireEvent.dragStart(surface, {dataTransfer});
        fireEvent.dragEnter(target, {dataTransfer});
        fireEvent.dragOver(target, {dataTransfer});
        expect(dataTransfer.dropEffect).toBe('none');
        expect(dataTransfer.types).not.toContain('application/x-prosuna-no-code-node');
    }
    expect(onChange).not.toHaveBeenCalled();
});

it.each(['readOnly', 'busy'] as const)('disables pointer dragging on pills in %s mode', (mode) => {
    render(<Harness onChange={vi.fn()} {...{[mode]: true}}/>);
    expect(screen.getByRole('button', {name: 'a, Beliebiger Wert'})).toHaveAttribute('draggable', 'false');
    expect(screen.getByRole('button', {name: 'Ist Gleich, Wahrheitswert'})).toHaveAttribute('draggable', 'false');
});

it('retains partial results in the expression when the test area is closed', async () => {
    api.post.mockResolvedValue({
        status: NoCodeDryRunStatus.Evaluated,
        value: true,
        operands: [],
    });
    const onChange = vi.fn();
    render(<Harness onChange={onChange}/>);
    fireEvent.click(screen.getByRole('button', {name: 'Testlauf'}));
    fireEvent.click(screen.getByRole('button', {name: 'Test ausführen'}));
    const name = 'Bedingung: Operator Ist Gleich, Wahrheitswert';
    await waitFor(() => expect(screen.getByRole('treeitem', {name})).toHaveAccessibleDescription('Testergebnis: Wahr'));
    expect(screen.getAllByRole('tree')).toHaveLength(1);
    expect(screen.getByRole('tree')).toHaveAttribute('aria-readonly', 'false');
    fireEvent.click(screen.getByRole('button', {name: 'Testlauf'}));
    expect(screen.queryByRole('region', {name: 'Testlauf'})).not.toBeInTheDocument();
    expect(screen.getByRole('treeitem', {name})).toHaveAccessibleDescription('Testergebnis: Wahr');
    fireEvent.click(screen.getByRole('button', {name: 'Testlauf'}));
    expect(screen.getByRole('region', {name: 'Gesamtergebnis'})).not.toHaveTextContent('Nicht mehr aktuell');
    expect(api.post).toHaveBeenCalledOnce();
    expect(onChange).not.toHaveBeenCalled();
});
