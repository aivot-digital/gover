import {fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import {useState} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {ElementType} from '../../../data/element-type/element-type';
import {NoCodeDryRunStatus} from '../../../models/dtos/no-code-dry-run-dto';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import {type NoCodeOperatorDetailsDTO} from '../../../models/dtos/no-code-operator-details-dto';
import {type NoCodeOperand} from '../../../models/functions/no-code-expression';
import {PillNoCodeEditor} from './pill-no-code-editor';

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
        parameters: ['Wert 1', 'Wert 2'].map((label) => ({type: NoCodeDataType.Runtime, label, description: null, options: []})),
    }],
};

const expression: NoCodeOperand = {
    type: 'NoCodeExpression',
    operatorIdentifier: 'equals',
    operands: [
        {type: 'NoCodeStaticValue', value: 'a'},
        {type: 'NoCodeStaticValue', value: 'b'},
    ],
};

const not: NoCodeOperatorDetailsDTO = {
    ...equals,
    identifier: 'not',
    label: 'Nicht',
    signatures: [{
        returnType: NoCodeDataType.Boolean,
        parameters: [{type: NoCodeDataType.Boolean, label: 'Wert', description: null, options: []}],
    }],
};

const add: NoCodeOperatorDetailsDTO = {
    ...equals,
    identifier: 'add',
    label: 'Addiere',
    signatures: [{
        returnType: NoCodeDataType.Number,
        parameters: ['Summand 1', 'Summand 2'].map((label) => ({type: NoCodeDataType.Number, label, description: null, options: []})),
    }],
};

// Has no parameter that could take a wrapped node.
const alwaysTrue: NoCodeOperatorDetailsDTO = {
    ...equals,
    identifier: 'always-true',
    label: 'Immer wahr',
    signatures: [{returnType: NoCodeDataType.Boolean, parameters: []}],
};

const a: NoCodeOperand = {type: 'NoCodeStaticValue', value: 'a'};
const amount = {...generateElementWithDefaultValues(ElementType.Number), id: 'amount', label: 'Betrag'};

// equals(a, not(empty)): a leaf, an operator to swap with and a socket to move into.
const nested: NoCodeOperand = {
    type: 'NoCodeExpression',
    operatorIdentifier: 'equals',
    operands: [a, {type: 'NoCodeExpression', operatorIdentifier: 'not', operands: [null]}],
};

function Harness(props: {
    onChange: (operand: NoCodeOperand | null | undefined) => void;
    initial?: NoCodeOperand;
    operators?: NoCodeOperatorDetailsDTO[];
}) {
    // Clones like a parent that copies its state, which must not rebuild the tree.
    const [operand, setOperand] = useState<NoCodeOperand | null | undefined>(props.initial ?? expression);
    return (
        <PillNoCodeEditor
            parameter={{type: NoCodeDataType.Boolean, label: 'Bedingung', description: null, options: []}}
            operand={operand}
            onChange={(next) => {
                props.onChange(next);
                setOperand(structuredClone(next));
            }}
            allOperators={props.operators ?? [equals, not, add, alwaysTrue]}
            allElements={[{element: amount, parents: []}]}
        />
    );
}

describe('PillNoCodeEditor', () => {
    it('shows operator labels and emits operands without editor IDs', () => {
        const onChange = vi.fn();
        render(<Harness onChange={onChange}/>);

        expect(screen.getByRole('button', {name: 'Ist Gleich, Wahrheitswert'})).toHaveTextContent('Ist Gleich');
        const second = screen.getByRole('treeitem', {name: 'Wert 2: b, Beliebiger Wert'});

        fireEvent.click(screen.getByRole('button', {name: 'Festen Wert „a“ löschen'}));

        expect(onChange).toHaveBeenLastCalledWith({
            ...expression,
            operands: [null, {type: 'NoCodeStaticValue', value: 'b'}],
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
            operands: [null, {type: 'NoCodeStaticValue', value: 'b'}],
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
            operands: [{type: 'NoCodeStaticValue', value: ''}, {type: 'NoCodeStaticValue', value: 'b'}],
        });
        // The new value opens its editor right away; closing it returns focus to the new tree item.
        const valueInput = await screen.findByRole('textbox', {name: /^Wert 1/});
        fireEvent.keyDown(valueInput, {key: 'Escape'});
        await waitFor(() => expect(screen.getByRole('treeitem', {name: 'Wert 1: Leerer Wert, Beliebiger Wert'})).toHaveFocus());
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
            render(<Harness onChange={onChange} initial={nested}/>);
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
                operands: [null, {type: 'NoCodeExpression', operatorIdentifier: 'not', operands: [a]}],
            });
            expect(status).toHaveTextContent('„a“ wurde verschoben.');
            expect(screen.getByRole('treeitem', {name: 'Wert: a, Wahrheitswert'})).toHaveFocus();
        });

        it('cancels with Escape without changing the expression', () => {
            const onChange = vi.fn();
            render(<Harness onChange={onChange} initial={nested}/>);
            const leaf = screen.getByRole('treeitem', {name: 'Wert 1: a, Beliebiger Wert'});
            leaf.focus();

            fireEvent.keyDown(leaf, {key: ' '});
            fireEvent.keyDown(leaf, {key: 'Escape'});
            expect(screen.getByRole('status')).toHaveTextContent('Verschieben von „a“ abgebrochen.');
            expect(onChange).not.toHaveBeenCalled();
            expect(screen.queryByText(/wird verschoben/)).not.toBeInTheDocument();
        });

        it('announces when a node has no target', () => {
            render(<Harness onChange={vi.fn()}/>);
            const root = screen.getByRole('treeitem', {name: 'Bedingung: Operator Ist Gleich, Wahrheitswert'});
            root.focus();

            fireEvent.keyDown(root, {key: ' '});

            expect(screen.getByRole('status')).toHaveTextContent('Für „Ist Gleich“ ist kein Ziel zum Verschieben verfügbar.');
        });

        it('moves a node down with the context menu and returns focus to it', async () => {
            const onChange = vi.fn();
            render(<Harness onChange={onChange} initial={nested}/>);
            const leaf = screen.getByRole('treeitem', {name: 'Wert 1: a, Beliebiger Wert'});
            leaf.focus();

            fireEvent.keyDown(leaf, {key: 'F10', shiftKey: true});
            expect(await screen.findByRole('menuitem', {name: 'Nach oben verschieben'})).toHaveAttribute('aria-disabled', 'true');
            fireEvent.click(screen.getByRole('menuitem', {name: 'Nach unten verschieben'}));

            expect(onChange).toHaveBeenLastCalledWith({
                ...nested,
                operands: [{type: 'NoCodeExpression', operatorIdentifier: 'not', operands: [null]}, a],
            });
            await waitFor(() => expect(screen.getByRole('treeitem', {name: 'Wert 2: a, Beliebiger Wert'})).toHaveFocus());
            expect(screen.getByRole('status')).toHaveTextContent('„a“ wurde verschoben.');
        });

        it('moves a node by clicking a target, without dragging', async () => {
            const onChange = vi.fn();
            render(<Harness onChange={onChange} initial={nested}/>);

            fireEvent.click(screen.getByRole('button', {name: 'Aktionen für „a“'}));
            fireEvent.click(await screen.findByRole('menuitem', {name: 'An andere Stelle verschieben'}));
            await waitFor(() => expect(screen.queryByRole('menu')).not.toBeInTheDocument());
            expect(screen.getByText('„a“ wird verschoben. Wählen Sie ein markiertes Ziel aus.')).toBeInTheDocument();

            fireEvent.click(screen.getByRole('button', {name: 'Wert festlegen'}));

            expect(onChange).toHaveBeenLastCalledWith({
                ...nested,
                operands: [null, {type: 'NoCodeExpression', operatorIdentifier: 'not', operands: [a]}],
            });
            // The click picked the target instead of opening the socket's type selector.
            expect(screen.queryByRole('button', {name: 'Fester Wert'})).not.toBeInTheDocument();
        });

        it('moves a node by dragging it with a pointer onto a socket', async () => {
            const onChange = vi.fn();
            render(<Harness onChange={onChange} initial={nested}/>);
            const row = (name: string) => screen.getByRole('treeitem', {name}).querySelector('[data-tree-row]')!;
            const dataTransfer = new FakeDataTransfer();
            const source = row('Wert 1: a, Beliebiger Wert');
            const target = row('Wert: leer, Wahrheitswert');

            fireEvent.dragStart(source, {dataTransfer});
            fireEvent.dragEnter(target, {dataTransfer});
            fireEvent.dragOver(target, {dataTransfer});
            expect(dataTransfer.dropEffect).toBe('move');
            fireEvent.drop(target, {dataTransfer});
            fireEvent.dragEnd(source, {dataTransfer});

            await waitFor(() => expect(onChange).toHaveBeenLastCalledWith({
                ...nested,
                operands: [null, {type: 'NoCodeExpression', operatorIdentifier: 'not', operands: [a]}],
            }));
        });

        it('rejects dragging an operator into its own parameters', () => {
            render(<Harness onChange={vi.fn()} initial={nested}/>);
            const row = (name: string) => screen.getByRole('treeitem', {name}).querySelector('[data-tree-row]')!;
            const dataTransfer = new FakeDataTransfer();

            fireEvent.dragStart(row('Wert 2: Operator Nicht, Wahrheitswert'), {dataTransfer});
            fireEvent.dragEnter(row('Wert: leer, Wahrheitswert'), {dataTransfer});
            fireEvent.dragOver(row('Wert: leer, Wahrheitswert'), {dataTransfer});

            expect(dataTransfer.dropEffect).toBe('none');
        });
    });
});

describe('adding a parent operator', () => {
    async function wrap(item: HTMLElement) {
        item.focus();
        fireEvent.keyDown(item, {key: 'F10', shiftKey: true});
        fireEvent.click(await screen.findByRole('menuitem', {name: 'Übergeordneten Operator hinzufügen'}));
        return screen.findByRole('dialog', {name: 'Operator auswählen'});
    }

    it('makes a value the first parameter of the chosen operator and focuses the new operator', async () => {
        const onChange = vi.fn();
        render(<Harness onChange={onChange} initial={nested}/>);

        const dialog = await wrap(screen.getByRole('treeitem', {name: 'Wert 1: a, Beliebiger Wert'}));
        fireEvent.click(within(dialog).getByRole('button', {name: 'Nicht'}));

        expect(onChange).toHaveBeenLastCalledWith({
            ...nested,
            operands: [{type: 'NoCodeExpression', operatorIdentifier: 'not', operands: [a]}, nested.operands![1]],
        });
        await waitFor(() => expect(screen.getByRole('treeitem', {name: 'Wert 1: Operator Nicht, Wahrheitswert'})).toHaveFocus());
        // Announced once the dialog no longer hides the page from screen readers.
        expect(screen.getByRole('status')).toHaveTextContent('Operator „Nicht“ hinzugefügt. „a“ ist jetzt sein erster Parameter.');
        expect(screen.getByRole('treeitem', {name: 'Wert: a, Wahrheitswert'})).toBeInTheDocument();
    });

    it('wraps the root operator and offers only operators that fit the slot and take a parameter', async () => {
        const onChange = vi.fn();
        render(<Harness onChange={onChange} initial={nested}/>);

        const dialog = await wrap(screen.getByRole('treeitem', {name: 'Bedingung: Operator Ist Gleich, Wahrheitswert'}));
        expect(within(dialog).queryByRole('button', {name: 'Addiere'})).not.toBeInTheDocument();
        expect(within(dialog).queryByRole('button', {name: 'Immer wahr'})).not.toBeInTheDocument();
        fireEvent.click(within(dialog).getByRole('button', {name: 'Nicht'}));

        expect(onChange).toHaveBeenLastCalledWith({type: 'NoCodeExpression', operatorIdentifier: 'not', operands: [nested]});
        await waitFor(() => expect(screen.getByRole('treeitem', {name: 'Bedingung: Operator Nicht, Wahrheitswert'})).toHaveFocus());
    });

    it('returns focus to the node when no operator is chosen', async () => {
        const onChange = vi.fn();
        render(<Harness onChange={onChange} initial={nested}/>);
        const leaf = screen.getByRole('treeitem', {name: 'Wert 1: a, Beliebiger Wert'});

        const dialog = await wrap(leaf);
        fireEvent.keyDown(dialog, {key: 'Escape'});

        await waitFor(() => expect(leaf).toHaveFocus());
        expect(onChange).not.toHaveBeenCalled();
    });
});

describe('PillNoCodeEditor types and dry run', () => {
    // equals(add(1, empty), not(empty))
    const typed: NoCodeOperand = {
        type: 'NoCodeExpression',
        operatorIdentifier: 'equals',
        operands: [
            {type: 'NoCodeExpression', operatorIdentifier: 'add', operands: [{type: 'NoCodeStaticValue', value: '1'}, null]},
            {type: 'NoCodeExpression', operatorIdentifier: 'not', operands: [null]},
        ],
    };

    it('warns about values the backend cannot convert', () => {
        render(<Harness onChange={vi.fn()} initial={{
            type: 'NoCodeExpression',
            operatorIdentifier: 'add',
            operands: [{type: 'NoCodeExpression', operatorIdentifier: 'not', operands: [null]}, {type: 'NoCodeStaticValue', value: 'zwei'}],
        }}/>);

        expect(screen.getByRole('treeitem', {name: 'Summand 1: Operator Nicht, Wahrheitswert'}))
            .toHaveAccessibleDescription('Typkonflikt: Erwartet wird „Zahlenwert“, geliefert wird „Wahrheitswert“.');
        expect(screen.getByRole('treeitem', {name: 'Summand 2: zwei, Zahlenwert'}))
            .toHaveAccessibleDescription('Typkonflikt: Erwartet wird „Zahlenwert“, „zwei“ lässt sich nicht umwandeln.');
    });

    it('offers only type-compatible targets when moving', () => {
        render(<Harness onChange={vi.fn()} initial={typed}/>);
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
        render(<Harness onChange={vi.fn()} initial={typed}/>);
        const row = (name: string) => screen.getByRole('treeitem', {name}).querySelector('[data-tree-row]')!;
        const dataTransfer = new FakeDataTransfer();

        fireEvent.dragStart(row('Wert 2: Operator Nicht, Wahrheitswert'), {dataTransfer});
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
                {status: NoCodeDryRunStatus.Evaluated, value: 43, message: null, operands: [
                    {status: NoCodeDryRunStatus.Evaluated, value: 42, message: null, operands: []},
                    {status: NoCodeDryRunStatus.Evaluated, value: '1', message: null, operands: []},
                ]},
                {status: NoCodeDryRunStatus.Failed, value: null, message: 'Division durch Null ist nicht erlaubt', operands: []},
            ],
        });
        render(<Harness onChange={vi.fn()} initial={{
            type: 'NoCodeExpression',
            operatorIdentifier: 'equals',
            operands: [
                {type: 'NoCodeExpression', operatorIdentifier: 'add', operands: [{type: 'NoCodeReference', elementId: 'amount'}, {type: 'NoCodeStaticValue', value: '1'}]},
                {type: 'NoCodeExpression', operatorIdentifier: 'not', operands: [{type: 'NoCodeProcessDataReference', path: 'ok'}]},
            ],
        }}/>);

        fireEvent.click(screen.getByRole('button', {name: 'Testlauf'}));
        fireEvent.change(screen.getByRole('textbox', {name: /^Testwert für „Betrag“/}), {target: {value: '42'}});
        fireEvent.change(screen.getByRole('textbox', {name: /^Vorgangsdaten \(JSON\)/}), {target: {value: '{"ok": true}'}});

        await waitFor(() => expect(api.post).toHaveBeenCalledWith('no-code/dry-run/', expect.objectContaining({
            elementData: {effectiveValues: {amount: 42}, elementStates: {}},
            processData: {ok: true},
            instanceData: null,
            nodeData: null,
        })));
        expect(api.post.mock.lastCall![1].expression).not.toHaveProperty('id');

        expect(await screen.findByRole('treeitem', {name: 'Wert 1: Operator Addiere, Zahlenwert'}))
            .toHaveAccessibleDescription('Testergebnis: 43');
        expect(screen.getByRole('treeitem', {name: 'Summand 1: Betrag, Zahlenwert'})).toHaveAccessibleDescription('Testergebnis: 42');
        expect(screen.getByText('Fehler: Division durch Null ist nicht erlaubt')).toBeInTheDocument();
        expect(screen.getByRole('treeitem', {name: 'Bedingung: Operator Ist Gleich, Wahrheitswert'}))
            .toHaveAccessibleDescription('Nicht ausgewertet: Ein Parameter konnte nicht ausgewertet werden.');
    });

    it('does not run while the JSON test data is invalid', async () => {
        render(<Harness onChange={vi.fn()} initial={{type: 'NoCodeProcessDataReference', path: 'ok'}}/>);

        fireEvent.click(screen.getByRole('button', {name: 'Testlauf'}));
        fireEvent.change(screen.getByRole('textbox', {name: /^Vorgangsdaten \(JSON\)/}), {target: {value: '[1'}});

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
    readonly items = Object.assign([] as {kind: string; type: string}[], {
        add: (value: string, type: string) => {
            this.data.set(type, value);
            this.items.push({kind: 'string', type});
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
                {type: NoCodeDataType.Runtime, label: 'Wert 1', description: null, sentencePhrase: 'wenn', options: []},
                {type: NoCodeDataType.Runtime, label: 'Wert 2', description: null, sentencePhrase: 'gleich', sentenceSuffix: 'ist', options: []},
            ],
        }],
    };
    const operatorItemName = 'Bedingung: Operator Ist Gleich, Wahrheitswert';

    it('reads the expression as a sentence and keeps the parameter labels as accessible names', () => {
        render(<Harness onChange={vi.fn()} operators={[sentenceEquals]}/>);

        const operatorItem = screen.getByRole('treeitem', {name: operatorItemName});
        const second = screen.getByRole('treeitem', {name: 'Wert 2: b, Beliebiger Wert'});
        expect(operatorItem.firstElementChild).toHaveTextContent('Ist Gleich');
        expect(operatorItem.firstElementChild).toHaveTextContent('ergibt wahr');
        expect(screen.getByRole('treeitem', {name: 'Wert 1: a, Beliebiger Wert'})).toHaveTextContent(/^wenn/);
        expect(second).toHaveTextContent(/^gleich/);
        expect(second).toHaveTextContent(/ist$/);
        expect(screen.queryByText('Wert 1')).not.toBeInTheDocument();
    });

    it('shows the parameter label when an operator has no sentence parts', () => {
        render(<Harness onChange={vi.fn()}/>);

        expect(screen.getByText('Wert 1')).toBeInTheDocument();
        expect(screen.queryByText('ergibt wahr')).not.toBeInTheDocument();
    });

    it('shows the summary on hover and the description on request', async () => {
        render(<Harness onChange={vi.fn()} operators={[sentenceEquals]}/>);

        fireEvent.mouseOver(screen.getByRole('button', {name: 'Ist Gleich, Wahrheitswert'}));
        const tooltip = await screen.findByRole('tooltip');
        expect(tooltip).toHaveTextContent('Prüft, ob zwei Werte gleich sind.');
        fireEvent.click(within(tooltip).getByRole('button', {name: 'Mehr...'}));

        const dialog = await screen.findByRole('dialog', {name: 'Ist Gleich'});
        expect(within(dialog).getByText('beide Werte').tagName).toBe('STRONG');
    });

    it('opens the description from the action menu and returns focus to the operator', async () => {
        render(<Harness onChange={vi.fn()} operators={[sentenceEquals]}/>);
        const operatorItem = screen.getByRole('treeitem', {name: operatorItemName});

        operatorItem.focus();
        fireEvent.keyDown(operatorItem, {key: 'F10', shiftKey: true});
        fireEvent.click(await screen.findByRole('menuitem', {name: 'Beschreibung anzeigen'}));
        const dialog = await screen.findByRole('dialog', {name: 'Ist Gleich'});
        fireEvent.keyDown(dialog, {key: 'Escape'});

        await waitFor(() => expect(operatorItem).toHaveFocus());
    });

    it('offers no description for values', async () => {
        render(<Harness onChange={vi.fn()} operators={[sentenceEquals]}/>);
        const leaf = screen.getByRole('treeitem', {name: 'Wert 1: a, Beliebiger Wert'});

        leaf.focus();
        fireEvent.keyDown(leaf, {key: 'F10', shiftKey: true});

        await screen.findByRole('menuitem', {name: 'An andere Stelle verschieben'});
        expect(screen.queryByRole('menuitem', {name: 'Beschreibung anzeigen'})).not.toBeInTheDocument();
    });
});
