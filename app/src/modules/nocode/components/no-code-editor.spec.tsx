import {act, fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {useState} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {ElementType} from '../../../data/element-type/element-type';
import {NoCodeDryRunStatus} from '../../../models/dtos/no-code-dry-run-dto';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {NoCodeDataType, NoCodeDataTypeLabels} from '../../../data/no-code-data-type';
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
    variableArgumentCount: true,
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

const currentToday: NoCodeOperatorDetailsDTO = {
    ...alwaysTrue,
    identifier: 'create-today',
    label: 'Heutiges Datum',
    abstractDescription: 'Liefert das heutige Datum.',
    humanReadableTemplate: 'heutiges Datum',
    signatures: [{returnType: NoCodeDataType.Date, parameters: []}],
};

const currentNow: NoCodeOperatorDetailsDTO = {
    ...currentToday,
    identifier: 'create-now',
    label: 'Aktueller Zeitpunkt',
    abstractDescription: 'Liefert das aktuelle Datum mit Uhrzeit.',
    humanReadableTemplate: 'aktueller Zeitpunkt',
    signatures: [{returnType: NoCodeDataType.DateTime, parameters: []}],
};

function temporalComparison(type: NoCodeDataType): NoCodeOperatorDetailsDTO {
    const [identifier, label, firstLabel, secondLabel] = type === NoCodeDataType.Date
        ? ['compare-date', 'Datumswerte vergleichen', 'Datum', 'Vergleichsdatum']
        : type === NoCodeDataType.Time
            ? ['compare-time', 'Uhrzeiten vergleichen', 'Uhrzeit', 'Vergleichsuhrzeit']
            : ['compare-datetime', 'Zeitpunkte vergleichen', 'Zeitpunkt', 'Vergleichszeitpunkt'];
    return {
        ...equals, identifier, label, humanReadableTemplate: '#0 ist #1 #2', sentenceLead: 'ergibt wahr',
        signatures: [{
            returnType: NoCodeDataType.Boolean,
            parameters: [{
                type, label: firstLabel, description: null, sentencePhrase: 'wenn', options: [],
            }, {
                type: NoCodeDataType.String,
                label: 'Vergleichsart',
                description: null,
                sentencePhrase: '',
                optionsAsSentencePart: true,
                options: [
                    {label: 'gleich', value: 'gleich'},
                    {label: 'nicht gleich', value: 'ungleich'},
                    {label: 'früher als', value: 'vor'},
                    {label: 'früher als oder gleich', value: 'vor-oder-gleich'},
                    {label: 'später als', value: 'nach'},
                    {label: 'später als oder gleich', value: 'nach-oder-gleich'},
                ],
            }, {
                type, label: secondLabel, description: null, sentencePhrase: '', sentenceSuffix: 'ist', options: [],
            }],
        }],
    };
}

const dateComparison = temporalComparison(NoCodeDataType.Date);

function dateArithmetic(identifier: string, label: string, direction: string, type = NoCodeDataType.Date): NoCodeOperatorDetailsDTO {
    const isDateTime = type === NoCodeDataType.DateTime;
    return {
        ...equals,
        identifier,
        label,
        sentenceLead: 'ergibt',
        humanReadableTemplate: `#0 ${direction} #1 #2`,
        signatures: [{
            returnType: type,
            parameters: [{
                type,
                label: isDateTime ? 'Ausgangszeitpunkt' : 'Ausgangsdatum',
                description: null,
                sentencePhrase: isDateTime ? 'den Zeitpunkt' : 'das Datum',
                options: [],
            }, {
                type: NoCodeDataType.Number,
                label: 'Anzahl',
                description: null,
                sentencePhrase: direction,
                numberConstraints: {integerOnly: true, minimum: -2147483648, maximum: 2147483647},
                options: [],
            }, {
                type: NoCodeDataType.String,
                label: 'Einheit',
                description: null,
                sentencePhrase: '',
                quantityParameterIndex: 1,
                options: [
                    {label: 'Tage', value: 'tage', singularLabel: 'Tag'},
                    {label: 'Wochen', value: 'wochen', singularLabel: 'Woche'},
                ],
            }],
        }],
    };
}

function dateArithmeticExpression(identifier: string, value = '2026-08-15'): NoCodeOperand {
    return {
        type: 'NoCodeExpression',
        operatorIdentifier: identifier,
        operands: [
            {type: 'NoCodeStaticValue', value},
            {type: 'NoCodeStaticValue', value: '1'},
            {type: 'NoCodeStaticValue', value: 'tage'},
        ],
    };
}

const incompleteComparison: NoCodeOperand = {
    type: 'NoCodeExpression',
    operatorIdentifier: dateComparison.identifier,
    operands: [
        {type: 'NoCodeStaticValue', value: '2000-01-01'},
        null,
        {type: 'NoCodeStaticValue', value: '2025-01-01'},
    ],
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
        expect(screen.queryByRole('button', {name: 'Wert „a“ löschen'})).not.toBeInTheDocument();
        const first = screen.getByRole('treeitem', {name: 'Wert 1: a, Beliebiger Wert'});
        first.focus();
        for (const key of ['Delete', 'Backspace', ' ']) fireEvent.keyDown(first, {key});
        fireEvent.keyDown(first, {
            key: 'F10',
            shiftKey: true,
        });
        expect(await screen.findByRole('menuitem', {name: 'Übergeordnete Funktion hinzufügen'})).toHaveAttribute('aria-disabled', 'true');
        expect(screen.getByRole('menuitem', {name: 'An andere Stelle verschieben'})).toHaveAttribute('aria-disabled', 'true');
        fireEvent.click(screen.getByRole('menuitem', {name: 'An andere Stelle verschieben'}));
        expect(onChange).not.toHaveBeenCalled();
        fireEvent.click(screen.getByRole('menuitem', {name: 'Beschreibung anzeigen'}));
        expect(await screen.findByRole('dialog')).toHaveTextContent('Wert selbst festlegen');
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
        const root = screen.getByRole('treeitem', {name: 'Bedingung: Funktion Ist Gleich, Wahrheitswert'});
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

    it.each([
        {
            name: 'a single value',
            initial: a,
            trigger: 'button',
            cancel: 'button',
        },
        {
            name: 'an operator without parameters',
            initial: {type: 'NoCodeExpression', operatorIdentifier: 'always-true', operands: []} as NoCodeOperand,
            trigger: 'keyboard',
            cancel: 'escape',
        },
        {
            name: 'an operator with empty parameters',
            initial: {type: 'NoCodeExpression', operatorIdentifier: 'equals', operands: [null, null]} as NoCodeOperand,
            trigger: 'menu',
            cancel: 'close',
        },
    ])('confirms losing notes when deleting $name and restores them with undo', async ({initial, trigger, cancel}) => {
        const stored: NoCodeOperand = {...initial, editorMetadata: {notes: 'Fachliche Ausnahme'}};
        const onChange = vi.fn();
        render(<Harness initial={stored}
                        onChange={onChange}/>);
        const root = screen.getByRole('treeitem', {name: /^Bedingung:/});
        root.focus();
        if (trigger === 'button') fireEvent.click(screen.getByRole('button', {name: 'Wert „a“ löschen'}));
        else if (trigger === 'keyboard') fireEvent.keyDown(root, {key: 'Delete'});
        else {
            fireEvent.keyDown(root, {key: 'F10', shiftKey: true});
            fireEvent.click(await screen.findByRole('menuitem', {name: 'Löschen'}));
        }

        const dialog = await screen.findByRole('dialog', {name: 'Ausdruck löschen?'});
        expect(within(dialog).getByRole('alert')).toHaveTextContent('Mit dem Ausdruck werden auch seine Notizen gelöscht.');
        expect(within(dialog).queryByRole('combobox')).not.toBeInTheDocument();
        expect(within(dialog).queryByRole('button', {name: 'Durch Teilausdruck ersetzen'})).not.toBeInTheDocument();
        const deleteButton = within(dialog).getByRole('button', {name: 'Ausdruck löschen'});
        expect(deleteButton).toHaveClass('MuiButton-contained', 'MuiButton-colorError');
        expect(deleteButton.querySelector('.MuiButton-startIcon svg')).not.toBeNull();
        expect(within(dialog).getByRole('button', {name: 'Abbrechen'})).toHaveClass('MuiButton-outlined');
        expect(onChange).not.toHaveBeenCalled();
        if (cancel === 'escape') fireEvent.keyDown(dialog, {key: 'Escape'});
        else fireEvent.click(within(dialog).getByRole('button', {name: cancel === 'close' ? 'Schließen' : 'Abbrechen'}));
        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        expect(root).toHaveFocus();
        expect(onChange).not.toHaveBeenCalled();

        fireEvent.keyDown(root, {key: 'Delete'});
        fireEvent.click(screen.getByRole('button', {name: 'Ausdruck löschen'}));
        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        expect(onChange).toHaveBeenLastCalledWith(null);
        expect(screen.getByRole('treeitem', {name: 'Bedingung: leer, Wahrheitswert'})).toHaveFocus();
        fireEvent.click(screen.getByRole('button', {name: 'Rückgängig'}));
        expect(onChange).toHaveBeenLastCalledWith(stored);
        expect(screen.getByRole('button', {name: 'Notizen anzeigen'})).toBeInTheDocument();
    });

    it.each(['delete', 'keep'] as const)('explains note loss for a root operator and handles %s', async (action) => {
        const stored: NoCodeOperand = {
            type: 'NoCodeExpression',
            operatorIdentifier: 'not',
            operands: [a],
            editorMetadata: {notes: 'Fachliche Ausnahme'},
        };
        const onChange = vi.fn();
        render(<Harness initial={stored}
                        onChange={onChange}/>);
        fireEvent.click(screen.getByRole('button', {name: 'Funktion „Nicht“ löschen'}));
        const dialog = screen.getByRole('dialog', {name: 'Funktion entfernen?'});
        expect(within(dialog).getByText('Bei „Alles löschen“ werden auch die Notizen zum Ausdruck gelöscht. Beim Ersetzen durch einen Teilausdruck bleiben sie erhalten.')).toBeInTheDocument();
        expect(onChange).not.toHaveBeenCalled();
        fireEvent.click(within(dialog).getByRole('button', {
            name: action === 'delete' ? 'Alles löschen' : 'Durch Teilausdruck ersetzen',
        }));
        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        expect(onChange).toHaveBeenLastCalledWith(action === 'delete' ? null : {
            ...a,
            editorMetadata: stored.editorMetadata,
        });
    });

    it('keeps root notes when deleting a child without warning about note loss', async () => {
        const stored: NoCodeOperand = {
            ...expression,
            operands: [{type: 'NoCodeExpression', operatorIdentifier: 'not', operands: [a]}, a],
            editorMetadata: {notes: 'Fachliche Ausnahme'},
        };
        const onChange = vi.fn();
        render(<Harness initial={stored}
                        onChange={onChange}/>);
        fireEvent.click(screen.getByRole('button', {name: 'Funktion „Nicht“ löschen'}));
        const dialog = screen.getByRole('dialog', {name: 'Funktion entfernen?'});
        expect(within(dialog).queryByText(/Notizen/)).not.toBeInTheDocument();
        fireEvent.click(within(dialog).getByRole('button', {name: 'Alles löschen'}));
        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        expect(onChange).toHaveBeenLastCalledWith({...stored, operands: [null, a]});
        fireEvent.click(screen.getByRole('button', {name: 'Wert „a“ löschen'}));
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        expect(onChange).toHaveBeenLastCalledWith({...stored, operands: [null, null]});
    });

    it.each([undefined, '', '   '])('deletes a single value immediately when notes are %j', (notes) => {
        const onChange = vi.fn();
        render(<Harness initial={{...a, editorMetadata: {notes}}}
                        onChange={onChange}/>);
        fireEvent.click(screen.getByRole('button', {name: 'Wert „a“ löschen'}));
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        expect(onChange).toHaveBeenLastCalledWith(null);
    });

    it('starts an empty expression through the shared choice without inserting a default value', async () => {
        const onChange = vi.fn();
        render(<Harness initial={null}
                        onChange={onChange}/>);
        expect(onChange).not.toHaveBeenCalled();
        expect(screen.getByText('Noch kein Ausdruck festgelegt')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Wert oder Variable'})).not.toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', {name: 'Ausdruck hinzufügen'}));
        fireEvent.click(screen.getByRole('button', {name: 'Mit Funktion ermitteln'}));
        const dialog = await screen.findByRole('dialog', {name: 'Funktion auswählen'});
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
        expect(empty).toHaveAccessibleDescription(/Legen Sie einen Wert selbst fest/);
        empty.focus();
        fireEvent.keyDown(empty, {key: 'Enter'});
        const choice = screen.getByRole('button', {name: 'Wert selbst festlegen'});
        fireEvent.keyDown(choice, {key: 'Escape'});
        await waitFor(() => expect(screen.queryByRole('button', {name: 'Wert selbst festlegen'})).not.toBeInTheDocument());
        expect(onChange).not.toHaveBeenCalled();
        expect(screen.getByText('Noch kein Ausdruck festgelegt')).toBeInTheDocument();
        expect(empty).toHaveFocus();
    });

    it('selects a declared option directly, focuses the result and keeps editing and history available', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        render(<Harness initial={incompleteComparison}
                        operators={[dateComparison]}
                        onChange={onChange}/>);
        const empty = screen.getByRole('treeitem', {name: 'Vergleichsart: leer, Textwert'});
        empty.focus();
        await user.keyboard('{Enter}');
        expect(screen.getByRole('button', {name: 'früher als oder gleich'})).toBeInTheDocument();
        expect(onChange).not.toHaveBeenCalled();
        await user.click(screen.getByRole('button', {name: 'früher als oder gleich'}));
        const selected = {
            ...incompleteComparison,
            operands: [
                incompleteComparison.operands![0],
                {type: 'NoCodeStaticValue', value: 'vor-oder-gleich'},
                incompleteComparison.operands![2],
            ],
        };
        expect(onChange).toHaveBeenCalledExactlyOnceWith(selected);
        const item = screen.getByRole('treeitem', {name: 'Vergleichsart: früher als oder gleich, Textwert'});
        await waitFor(() => expect(item).toHaveFocus());
        expect(screen.queryByRole('list', {name: 'Vergleichsart auswählen'})).not.toBeInTheDocument();
        expect(screen.queryByRole('combobox')).not.toBeInTheDocument();

        await user.keyboard('{Enter}');
        const input = await screen.findByRole('combobox', {name: 'Vergleichsart'});
        await user.click(input);
        await user.click(await screen.findByRole('option', {name: 'gleich'}));
        expect(onChange).toHaveBeenLastCalledWith({
            ...selected,
            operands: [selected.operands[0], {type: 'NoCodeStaticValue', value: 'gleich'}, selected.operands[2]],
        });
        await user.keyboard('{Escape}');
        await waitFor(() => expect(screen.queryByRole('combobox')).not.toBeInTheDocument());
        await user.click(screen.getByRole('button', {name: 'Rückgängig'}));
        expect(onChange).toHaveBeenLastCalledWith(selected);
        await user.click(screen.getByRole('button', {name: 'Rückgängig'}));
        expect(onChange).toHaveBeenLastCalledWith(incompleteComparison);
        await user.click(screen.getByRole('button', {name: 'Wiederholen'}));
        expect(onChange).toHaveBeenLastCalledWith(selected);
    });

    it('preserves an empty option slot when navigating sources and canceling nested dialogs', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        render(<Harness initial={incompleteComparison}
                        operators={[dateComparison, equals]}
                        onChange={onChange}/>);
        const empty = screen.getByRole('treeitem', {name: 'Vergleichsart: leer, Textwert'});
        empty.focus();
        await user.keyboard('{Enter}');
        await user.click(screen.getByRole('button', {name: 'Andere Wertquelle verwenden …'}));
        await user.click(screen.getByRole('button', {name: 'Mit Funktion ermitteln'}));
        await screen.findByRole('dialog', {name: 'Funktion auswählen'});
        await user.keyboard('{Escape}');
        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        expect(screen.getByRole('button', {name: 'Mit Funktion ermitteln'})).toHaveFocus();
        await user.click(screen.getByRole('button', {name: 'Zurück zur Auswahl'}));
        expect(screen.getByRole('button', {name: 'gleich'})).toHaveFocus();
        await user.keyboard('{Escape}');
        await waitFor(() => expect(screen.queryByRole('list', {name: 'Vergleichsart auswählen'})).not.toBeInTheDocument());
        expect(empty).toHaveFocus();
        expect(onChange).not.toHaveBeenCalled();
    });

    it('does not offer creation for a read-only empty expression', () => {
        const onChange = vi.fn();
        render(<Harness initial={null}
                        readOnly
                        onChange={onChange}/>);
        fireEvent.keyDown(screen.getByRole('treeitem'), {key: 'Enter'});
        expect(screen.getByText('Noch kein Ausdruck festgelegt')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Ausdruck hinzufügen'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Mit Funktion ermitteln'})).not.toBeInTheDocument();
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

    it('runs a stored variadic expression with every operand and retains them on replacement', async () => {
        const onChange = vi.fn();
        const stored: NoCodeOperand = {
            type: 'NoCodeExpression',
            operatorIdentifier: 'add',
            operands: ['1', '2', '3'].map(value => ({type: 'NoCodeStaticValue', value})),
        };
        const subtract = {...add, identifier: 'subtract', label: 'Subtrahiere'};
        api.post.mockResolvedValue({status: NoCodeDryRunStatus.Evaluated, value: 6, operands: []});
        render(<Harness initial={stored} operators={[add, subtract]} returnType={NoCodeDataType.Number} onChange={onChange}/>);
        expect(screen.queryByText(/Die Anzahl der Parameter passt nicht/)).not.toBeInTheDocument();
        expect(screen.getByRole('treeitem', {name: 'Summand 3: 3, Zahlenwert'})).toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', {name: 'Testlauf'}));
        fireEvent.click(screen.getByRole('button', {name: 'Test ausführen'}));
        await waitFor(() => expect(api.post).toHaveBeenCalledWith('no-code/dry-run/',
            expect.objectContaining({expression: stored}), expect.anything()));
        fireEvent.click(screen.getByRole('button', {name: 'Addiere, Zahlenwert'}));
        const dialog = await screen.findByRole('dialog', {name: 'Funktion auswählen'});
        fireEvent.click(within(dialog).getByRole('button', {name: 'Subtrahiere: Ersetzen'}));
        expect(onChange).toHaveBeenLastCalledWith({...stored, operatorIdentifier: 'subtract'});
    });

    describe('preserving authored spaces for text functions', () => {
        const literal = (value: string): NoCodeOperand => ({type: 'NoCodeStaticValue', value});

        it.each([
            ['split', 'Text aufteilen', NoCodeDataType.List, ['Max Mustermann'], ' ', ['Max', 'Mustermann']],
            ['concat', 'Werte zu Text zusammenfügen', NoCodeDataType.String, ['Guten', ' Tag'], ' ', 'Guten Tag '],
            ['text-contains', 'Text enthält', NoCodeDataType.Boolean, ['AZ Nr. 5'], 'AZ ', true],
            ['text-starts-with', 'Text beginnt mit', NoCodeDataType.Boolean, ['AZ123'], 'AZ ', false],
            ['text-ends-with', 'Text endet mit', NoCodeDataType.Boolean, ['AZ 123'], ' 123', true],
            ['text-equals-ignore-case', 'Text ist gleich (ohne Groß-/Kleinschreibung)', NoCodeDataType.Boolean, ['AZ'], 'az ', false],
        ] as const)('preserves the input for %s on blur, reopen and dry run', async (
            identifier, label, returnType, precedingValues, authoredValue, result,
        ) => {
            const user = userEvent.setup();
            const isSplit = identifier === 'split';
            const isConcat = identifier === 'concat';
            const inputType = isConcat ? NoCodeDataType.Runtime : NoCodeDataType.String;
            const secondLabel = isSplit ? 'Trennzeichen' : 'Wert 2';
            const targetLabel = isConcat ? 'Wert 3' : secondLabel;
            const operator: NoCodeOperatorDetailsDTO = {
                ...equals,
                identifier,
                label,
                variableArgumentCount: isConcat,
                humanReadableTemplate: isSplit ? '#0 aufgeteilt an #1' : null,
                signatures: [{returnType, parameters: ['Wert 1', secondLabel].map(parameterLabel => ({
                    type: inputType,
                    label: parameterLabel,
                    description: null,
                    preserveWhitespace: true,
                    options: [],
                }))}],
            };
            const onChange = vi.fn();
            const props = {onChange, operators: [operator], returnType};
            const initial: NoCodeOperand = {
                type: 'NoCodeExpression',
                operatorIdentifier: identifier,
                operands: [...precedingValues.map(literal), null],
            };
            const view = render(<Harness {...props} initial={initial}/>);
            fireEvent.click(screen.getByRole('button', {name: `${targetLabel} festlegen`}));
            fireEvent.click(await screen.findByRole('button', {name: 'Wert selbst festlegen'}));
            const input = await screen.findByRole('textbox', {name: targetLabel});
            await user.type(input, authoredValue);
            await user.tab();
            expect(input).toHaveValue(authoredValue);
            await user.keyboard('{Escape}');
            await waitFor(() => expect(screen.queryByRole('textbox', {name: targetLabel})).not.toBeInTheDocument());

            const saved = structuredClone(onChange.mock.lastCall![0]);
            const expected = {...initial, operands: [...precedingValues.map(literal), literal(authoredValue)]};
            expect(saved).toEqual(expected);
            view.unmount();
            onChange.mockClear();
            render(<Harness {...props} initial={saved}/>);

            if (isSplit) {
                expect(screen.getByRole('treeitem', {name: 'Trennzeichen: 1 Leerzeichen, Textwert'})).toBeVisible();
                fireEvent.click(screen.getByRole('button', {name: 'Text aufteilen: zuklappen'}));
                expect(screen.getByText('„Max Mustermann“ aufgeteilt an einem Leerzeichen')).toBeVisible();
            }
            expect(onChange).not.toHaveBeenCalled();

            api.post.mockResolvedValueOnce({status: NoCodeDryRunStatus.Evaluated, value: result, operands: []});
            fireEvent.click(screen.getByRole('button', {name: 'Testlauf'}));
            fireEvent.click(screen.getByRole('button', {name: 'Test ausführen'}));
            await waitFor(() => expect(api.post).toHaveBeenCalledWith('no-code/dry-run/',
                expect.objectContaining({expression: expected}), expect.anything()));
        });
    });

    describe('editing variadic inputs', () => {
        const removeInputName = /^Eingabe „.+“ entfernen$/;
        const literal = (value: string): NoCodeOperand => ({type: 'NoCodeStaticValue', value});

        it.each([
            ['concat', 'Werte zu Text zusammenfügen', 'Wert 1', 'Wert 2', ['AZ', '-', '42'], 'AZ-42'],
            ['add', 'Addieren', 'Zahl 1', 'Zahl 2', ['24', '3', '2'], 29],
            ['subtract', 'Subtrahieren', 'Ausgangszahl', 'Abzuziehende Zahl 1', ['24', '3', '2'], 19],
            ['multiply', 'Multiplizieren', 'Zahl 1', 'Zahl 2', ['24', '3', '2'], 144],
            ['divide', 'Dividieren', 'Ausgangszahl', 'Teiler 1', ['24', '3', '2'], 4],
        ] as const)('creates, reopens and tests all three authored inputs of %s in order', async (
            identifier, label, firstLabel, secondLabel, values, result,
        ) => {
            const isText = identifier === 'concat';
            const returnType = isText ? NoCodeDataType.String : NoCodeDataType.Number;
            const operator: NoCodeOperatorDetailsDTO = {
                ...add, identifier, label,
                signatures: [{returnType, parameters: [firstLabel, secondLabel].map(parameterLabel => ({
                    type: isText ? NoCodeDataType.Runtime : NoCodeDataType.Number,
                    label: parameterLabel, description: null, options: [],
                }))}],
            };
            const onChange = vi.fn();
            const editorProps = {onChange, operators: [operator], returnType};
            const view = render(<Harness {...editorProps} initial={null}/>);
            fireEvent.click(screen.getByRole('button', {name: 'Ausdruck hinzufügen'}));
            fireEvent.click(screen.getByRole('button', {name: 'Mit Funktion ermitteln'}));
            const picker = await screen.findByRole('dialog', {name: 'Funktion auswählen'});
            fireEvent.click(within(picker).getByRole('button', {name: `${label}: Hinzufügen`}));
            await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());

            const root = {type: 'NoCodeExpression', operatorIdentifier: identifier};
            expect(onChange).toHaveBeenLastCalledWith({...root, operands: [null, null]});
            expect(screen.queryAllByRole('button', {name: removeInputName})).toHaveLength(0);
            fireEvent.click(screen.getByRole('button', {name: isText ? 'Weiteren Wert hinzufügen' : 'Weitere Zahl hinzufügen'}));
            expect(onChange).toHaveBeenLastCalledWith({...root, operands: [null, null, null]});
            const sockets = screen.getAllByRole('treeitem', {name: /: leer,/});
            expect(sockets).toHaveLength(3);
            expect(sockets[2]).toHaveFocus();
            for (const [index, value] of values.entries()) {
                fireEvent.keyDown(sockets[index], {key: 'Enter'});
                fireEvent.click(await screen.findByRole('button', {name: 'Wert selbst festlegen'}));
                const input = await screen.findByRole('textbox');
                fireEvent.change(input, {target: {value}});
                fireEvent.keyDown(input, {key: 'Escape'});
                await waitFor(() => expect(screen.queryByRole('textbox')).not.toBeInTheDocument());
            }

            const saved: NoCodeOperand = JSON.parse(JSON.stringify(onChange.mock.lastCall![0]));
            expect(saved).toEqual({...root, operands: values.map(literal)});
            view.unmount();
            onChange.mockClear();
            render(<Harness {...editorProps} initial={saved}/>);
            for (const value of values) {
                const type = isText ? 'Beliebiger Wert' : 'Zahlenwert';
                expect(screen.getByRole('button', {name: `${value}, ${type}`})).toBeInTheDocument();
            }
            expect(onChange).not.toHaveBeenCalled();

            api.post.mockResolvedValueOnce({status: NoCodeDryRunStatus.Evaluated, value: result, operands: []});
            fireEvent.click(screen.getByRole('button', {name: 'Testlauf'}));
            fireEvent.click(screen.getByRole('button', {name: 'Test ausführen'}));
            await waitFor(() => expect(api.post).toHaveBeenCalledWith('no-code/dry-run/',
                expect.objectContaining({expression: saved}), {abortController: expect.any(AbortController)}));
        });

        it('confirms removal of an occupied extra position and preserves the following subtree and history', async () => {
            const onChange = vi.fn();
            const firstTwo = [literal('24'), literal('3')];
            const nestedValue: NoCodeOperand = {type: 'NoCodeExpression', operatorIdentifier: 'add', operands: [literal('1'), literal('2')]};
            const stored: NoCodeOperand = {
                type: 'NoCodeExpression', operatorIdentifier: 'add', operands: [...firstTwo, nestedValue, literal('8')],
            };
            render(<Harness initial={stored} operators={[add]} returnType={NoCodeDataType.Number} onChange={onChange}/>);
            const third = screen.getByRole('treeitem', {name: /^Summand 3: Funktion Addiere/});
            const fourth = screen.getByRole('treeitem', {name: 'Summand 4: 8, Zahlenwert'});
            const removeThird = within(third).getByRole('button', {name: 'Eingabe „Summand 3“ entfernen'});
            fireEvent.click(removeThird);
            let dialog = await screen.findByRole('dialog', {name: 'Eingabe entfernen?'});
            fireEvent.click(within(dialog).getByRole('button', {name: 'Abbrechen'}));
            await waitFor(() => expect(third).toHaveFocus());
            expect(onChange).not.toHaveBeenCalled();

            fireEvent.click(removeThird);
            dialog = await screen.findByRole('dialog', {name: 'Eingabe entfernen?'});
            fireEvent.click(within(dialog).getByRole('button', {name: 'Eingabe entfernen'}));
            const remaining = {...stored, operands: [...firstTwo, literal('8')]};
            expect(onChange).toHaveBeenCalledExactlyOnceWith(remaining);
            await waitFor(() => expect(fourth).toHaveFocus());
            expect(fourth).toHaveAccessibleName('Summand 3: 8, Zahlenwert');
            expect(third).not.toBeInTheDocument();
            fireEvent.click(screen.getByRole('button', {name: 'Rückgängig'}));
            expect(onChange).toHaveBeenLastCalledWith(stored);
            expect(screen.getByRole('treeitem', {name: /^Summand 3: Funktion Addiere/})).toBeInTheDocument();
            fireEvent.click(screen.getByRole('button', {name: 'Wiederholen'}));
            expect(onChange).toHaveBeenLastCalledWith(remaining);
        });

        it('removes an empty extra slot immediately and starts a new slot at the same index untouched', () => {
            const onChange = vi.fn();
            const stored: NoCodeOperand = {
                type: 'NoCodeExpression', operatorIdentifier: 'add', operands: [literal('1'), literal('2'), null],
            };
            render(<Harness initial={stored} returnType={NoCodeDataType.Number} onChange={onChange}/>);
            fireEvent.click(screen.getByRole('button', {name: 'Testlauf'}));
            fireEvent.click(screen.getByRole('button', {name: 'Test ausführen'}));
            const empty = screen.getByRole('treeitem', {name: 'Summand 3: leer, Zahlenwert'});
            expect(empty).toHaveAttribute('aria-invalid', 'true');
            expect(api.post).not.toHaveBeenCalled();
            fireEvent.click(within(empty).getByRole('button', {name: 'Eingabe „Summand 3“ entfernen'}));
            expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
            expect(onChange).toHaveBeenLastCalledWith({...stored, operands: [literal('1'), literal('2')]});
            expect(screen.getByRole('treeitem', {name: 'Summand 2: 2, Zahlenwert'})).toHaveFocus();
            expect(screen.queryAllByRole('button', {name: removeInputName})).toHaveLength(0);
            fireEvent.click(screen.getByRole('button', {name: 'Weitere Zahl hinzufügen'}));
            const added = screen.getByRole('treeitem', {name: 'Summand 3: leer, Zahlenwert'});
            expect(added).toHaveFocus();
            expect(added).not.toHaveAttribute('aria-invalid', 'true');
        });

        it('operates the extra slot by keyboard without treating resize buttons as move targets', async () => {
            const user = userEvent.setup();
            const onChange = vi.fn();
            const stored: NoCodeOperand = {
                type: 'NoCodeExpression', operatorIdentifier: 'add', operands: [literal('1'), literal('2')],
            };
            render(<Harness initial={stored} returnType={NoCodeDataType.Number} onChange={onChange}/>);
            screen.getByRole('button', {name: 'Weitere Zahl hinzufügen'}).focus();
            await user.keyboard('{Enter}');
            const empty = screen.getByRole('treeitem', {name: 'Summand 3: leer, Zahlenwert'});
            expect(empty).toHaveFocus();
            await user.tab();
            expect(within(empty).getByRole('button', {name: 'Eingabe „Summand 3“ entfernen'})).toHaveFocus();
            await user.keyboard('{Enter}');
            expect(onChange).toHaveBeenLastCalledWith(stored);

            fireEvent.click(screen.getByRole('button', {name: 'Weitere Zahl hinzufügen'}));
            screen.getByRole('treeitem', {name: 'Summand 2: 2, Zahlenwert'}).focus();
            await user.keyboard(' ');
            expect(screen.queryByRole('button', {name: 'Weitere Zahl hinzufügen'})).not.toBeInTheDocument();
            expect(screen.queryByRole('button', {name: removeInputName})).not.toBeInTheDocument();
            await user.keyboard('{End} ');
            expect(onChange).toHaveBeenLastCalledWith({...stored, operands: [literal('1'), null, literal('2')]});
            expect(screen.getByRole('treeitem', {name: 'Summand 3: 2, Zahlenwert'})).toHaveFocus();
        });

        it('offers the actions only on an expanded supported operator with its variadic signature', () => {
            const stored: NoCodeOperand = {
                type: 'NoCodeExpression', operatorIdentifier: 'add', operands: [literal('1'), literal('2'), literal('3')],
            };
            const onChange = vi.fn();
            const props = {initial: stored, returnType: NoCodeDataType.Number, onChange};
            const view = render(<Harness {...props} operators={[add]}/>);
            fireEvent.click(screen.getByRole('button', {name: 'Addiere: zuklappen'}));
            expect(screen.queryByRole('button', {name: 'Weitere Zahl hinzufügen'})).not.toBeInTheDocument();
            expect(screen.queryByRole('button', {name: removeInputName})).not.toBeInTheDocument();
            fireEvent.click(screen.getByRole('button', {name: 'Addiere: aufklappen'}));
            expect(screen.getByRole('button', {name: 'Weitere Zahl hinzufügen'})).toBeVisible();

            view.rerender(<Harness {...props} operators={[{...add, variableArgumentCount: false}]}/>);
            expect(screen.queryByRole('button', {name: 'Weitere Zahl hinzufügen'})).not.toBeInTheDocument();
            expect(screen.queryByRole('button', {name: removeInputName})).not.toBeInTheDocument();
            expect(screen.getByRole('treeitem', {name: 'Summand 3: 3, Zahlenwert'})).toBeVisible();
            expect(onChange).not.toHaveBeenCalled();
        });

        it('closes a pending removal and restores focus when the editor becomes busy', async () => {
            const onChange = vi.fn();
            const props = {
                initial: {type: 'NoCodeExpression', operatorIdentifier: 'add', operands: [literal('1'), literal('2'), literal('3')]} satisfies NoCodeOperand,
                returnType: NoCodeDataType.Number,
                onChange,
            };
            const view = render(<Harness {...props}/>);
            fireEvent.click(screen.getByRole('button', {name: 'Eingabe „Summand 3“ entfernen'}));
            expect(await screen.findByRole('dialog', {name: 'Eingabe entfernen?'})).toBeVisible();

            view.rerender(<Harness {...props} busy/>);
            await waitFor(() => expect(screen.getByRole('treeitem', {name: 'Summand 3: 3, Zahlenwert'})).toHaveFocus());
            expect(onChange).not.toHaveBeenCalled();
        });

        it.each(['readOnly', 'busy'] as const)('hides input-count actions for a %s variadic expression', mode => {
            const onChange = vi.fn();
            render(<Harness
                initial={{type: 'NoCodeExpression', operatorIdentifier: 'add', operands: [literal('1'), literal('2'), literal('3')]}}
                returnType={NoCodeDataType.Number} onChange={onChange} {...{[mode]: true}}
            />);
            expect(screen.queryByRole('button', {name: 'Weitere Zahl hinzufügen'})).not.toBeInTheDocument();
            expect(screen.queryByRole('button', {name: removeInputName})).not.toBeInTheDocument();
            expect(screen.getByRole('treeitem', {name: 'Summand 3: 3, Zahlenwert'})).toBeVisible();
            expect(onChange).not.toHaveBeenCalled();
        });
    });

    it('confirms removed operands, leaves cancellation unchanged and can undo a zero-parameter switch', async () => {
        const onChange = vi.fn();
        render(<Harness onChange={onChange}/>);
        const choose = async () => {
            fireEvent.click(screen.getByRole('button', {name: 'Ist Gleich, Wahrheitswert'}));
            const picker = await screen.findByRole('dialog', {name: 'Funktion auswählen'});
            fireEvent.click(within(picker).getByRole('button', {name: 'Immer wahr: Ersetzen'}));
            return screen.findByRole('dialog', {name: 'Funktion ändern'});
        };
        let dialog = await choose();
        expect(within(dialog).getByRole('alert')).toHaveTextContent('2 belegte Parameter werden');
        expect(within(dialog).getByText('Neue Funktion: Immer wahr')).toBeInTheDocument();
        const confirmButton = within(dialog).getByRole('button', {name: 'Funktion ändern'});
        expect(confirmButton).toHaveClass('MuiButton-contained', 'MuiButton-colorPrimary');
        expect(confirmButton.querySelector('.MuiButton-startIcon')).toBeNull();
        expect(within(dialog).getByRole('button', {name: 'Abbrechen'})).toHaveClass('MuiButton-outlined');
        expect(onChange).not.toHaveBeenCalled();
        fireEvent.keyDown(dialog, {key: 'Escape'});
        await waitFor(() => expect(screen.queryByRole('dialog', {name: 'Funktion ändern'})).not.toBeInTheDocument());
        expect(onChange).not.toHaveBeenCalled();
        dialog = await choose();
        fireEvent.click(within(dialog).getByRole('button', {name: 'Funktion ändern'}));
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
        const picker = await screen.findByRole('dialog', {name: 'Funktion auswählen'});
        fireEvent.click(within(picker).getByRole('button', {name: 'Mehrere Varianten: Ersetzen'}));
        const dialog = await screen.findByRole('dialog', {name: 'Funktion ändern'});
        expect(within(dialog).getByRole('combobox', {name: /Anzahl der Parameter/})).toHaveTextContent('3');
        expect(onChange).not.toHaveBeenCalled();
        fireEvent.click(within(dialog).getByRole('button', {name: 'Funktion ändern'}));
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
        await waitFor(() => expect(screen.getByRole('treeitem', {name: 'Bedingung: Funktion Ist Gleich, Wahrheitswert'}))
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
        expect(screen.getByRole('treeitem', {name: 'Bedingung: Funktion Ist Gleich, Wahrheitswert'}))
            .toHaveAccessibleDescription('Testergebnis: Wahr');
    });

    it('shows operator labels and emits operands without editor IDs', () => {
        const onChange = vi.fn();
        render(<Harness onChange={onChange}/>);

        expect(screen.getByRole('button', {name: 'Ist Gleich, Wahrheitswert'})).toHaveTextContent('Ist Gleich');
        const second = screen.getByRole('treeitem', {name: 'Wert 2: b, Beliebiger Wert'});

        fireEvent.click(screen.getByRole('button', {name: 'Wert „a“ löschen'}));

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
        const root = screen.getByRole('treeitem', {name: 'Bedingung: Funktion Ist Gleich, Wahrheitswert'});
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
        fireEvent.click(await screen.findByRole('button', {name: 'Wert selbst festlegen'}));

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
        const root = screen.getByRole('treeitem', {name: 'Bedingung: Funktion Ist Gleich, Wahrheitswert'});
        root.focus();

        fireEvent.keyDown(root, {key: 'Enter'});

        expect(await screen.findByRole('dialog', {name: 'Funktion auswählen'})).toBeInTheDocument();
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
            expect(status).toHaveTextContent('„a“ aufgenommen. Ziel: Wert 2: Funktion Nicht, Wahrheitswert, die Werte werden getauscht.');
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

        it.each([undefined, {notes: 'Interne Notiz'}])('disables the move menu and announces keyboard attempts without targets with metadata %j', async (editorMetadata) => {
            render(<Harness initial={{
                ...expression,
                editorMetadata,
            }}
                            onChange={vi.fn()}/>);
            const root = screen.getByRole('treeitem', {name: 'Bedingung: Funktion Ist Gleich, Wahrheitswert'});
            root.focus();

            fireEvent.keyDown(root, {key: ' '});

            expect(screen.getByRole('status')).toHaveTextContent('Für „Ist Gleich“ ist kein Ziel zum Verschieben verfügbar.');
            fireEvent.keyDown(root, {key: 'F10', shiftKey: true});
            expect(await screen.findByRole('menuitem', {name: 'An andere Stelle verschieben'})).toHaveAttribute('aria-disabled', 'true');
        });

        it.each([null, expression.operands![1]])('enables the move menu for a valid empty or occupied target %j', async (target) => {
            render(<Harness initial={{...expression, operands: [a, target]}}
                            onChange={vi.fn()}/>);

            fireEvent.click(screen.getByRole('button', {name: 'Aktionen für „a“'}));

            expect(await screen.findByRole('menuitem', {name: 'An andere Stelle verschieben'})).not.toHaveAttribute('aria-disabled', 'true');
        });

        it.each([
            {
                name: 'the moved value does not fit any target',
                types: [NoCodeDataType.Boolean, NoCodeDataType.Number],
                source: 'Immer wahr',
                operands: [{type: 'NoCodeExpression', operatorIdentifier: 'always-true', operands: []}, null],
            },
            {
                name: 'the swapped occupant does not fit the source',
                types: [NoCodeDataType.Number, NoCodeDataType.Runtime],
                source: '1',
                operands: [{type: 'NoCodeStaticValue', value: '1'}, {type: 'NoCodeExpression', operatorIdentifier: 'always-true', operands: []}],
            },
        ])('disables the move menu when $name', async ({types, source, operands}) => {
            const typedEquals: NoCodeOperatorDetailsDTO = {
                ...equals,
                signatures: [{
                    returnType: NoCodeDataType.Boolean,
                    parameters: equals.signatures[0].parameters.map((parameter, index) => ({...parameter, type: types[index]})),
                }],
            };
            render(<Harness initial={{...expression, operands: operands as (NoCodeOperand | null)[]}}
                            operators={[typedEquals, alwaysTrue]}
                            onChange={vi.fn()}/>);

            fireEvent.click(screen.getByRole('button', {name: `Aktionen für „${source}“`}));

            expect(await screen.findByRole('menuitem', {name: 'An andere Stelle verschieben'})).toHaveAttribute('aria-disabled', 'true');
        });

        it('enables the move menu after expanding its only valid target', async () => {
            const typedEquals: NoCodeOperatorDetailsDTO = {
                ...equals,
                signatures: [{
                    returnType: NoCodeDataType.Boolean,
                    parameters: equals.signatures[0].parameters.map((parameter, index) => ({
                        ...parameter,
                        type: index === 0 ? NoCodeDataType.Number : NoCodeDataType.Runtime,
                    })),
                }],
            };
            render(<Harness initial={{...nested, operands: [{type: 'NoCodeStaticValue', value: '1'}, nested.operands![1]]}}
                            operators={[typedEquals, not]}
                            onChange={vi.fn()}/>);
            fireEvent.click(screen.getByRole('button', {name: 'Nicht: zuklappen'}));
            fireEvent.click(screen.getByRole('button', {name: 'Aktionen für „1“'}));
            expect(await screen.findByRole('menuitem', {name: 'An andere Stelle verschieben'})).toHaveAttribute('aria-disabled', 'true');

            fireEvent.keyDown(screen.getByRole('menu'), {key: 'Escape'});
            await waitFor(() => expect(screen.queryByRole('menu')).not.toBeInTheDocument());
            fireEvent.click(screen.getByRole('button', {name: 'Nicht: aufklappen'}));
            fireEvent.click(screen.getByRole('button', {name: 'Aktionen für „1“'}));

            expect(await screen.findByRole('menuitem', {name: 'An andere Stelle verschieben'})).not.toHaveAttribute('aria-disabled', 'true');
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
            expect(screen.queryByRole('button', {name: 'Wert selbst festlegen'})).not.toBeInTheDocument();
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
        fireEvent.click(await screen.findByRole('menuitem', {name: 'Übergeordnete Funktion hinzufügen'}));
        return screen.findByRole('dialog', {name: 'Funktion auswählen'});
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
        await waitFor(() => expect(screen.getByRole('treeitem', {name: 'Wert 1: Funktion Nicht, Wahrheitswert'})).toHaveFocus());
        // Announced once the dialog no longer hides the page from screen readers.
        expect(screen.getByRole('status')).toHaveTextContent('Funktion „Nicht“ hinzugefügt. „a“ ist jetzt ihr erster Parameter.');
        expect(screen.getByRole('treeitem', {name: 'Wert: a, Wahrheitswert'})).toBeInTheDocument();
    });

    it('wraps the root operator and offers only operators that fit the slot and take a parameter', async () => {
        const onChange = vi.fn();
        render(<Harness onChange={onChange}
                        initial={nested}/>);

        const dialog = await wrap(screen.getByRole('treeitem', {name: 'Bedingung: Funktion Ist Gleich, Wahrheitswert'}));
        expect(within(dialog).queryByRole('button', {name: 'Addiere: Hinzufügen'})).not.toBeInTheDocument();
        expect(within(dialog).queryByRole('button', {name: 'Immer wahr: Hinzufügen'})).not.toBeInTheDocument();
        fireEvent.click(within(dialog).getByRole('button', {name: 'Nicht: Hinzufügen'}));

        expect(onChange).toHaveBeenLastCalledWith({
            type: 'NoCodeExpression',
            operatorIdentifier: 'not',
            operands: [nested],
        });
        await waitFor(() => expect(screen.getByRole('treeitem', {name: 'Bedingung: Funktion Nicht, Wahrheitswert'})).toHaveFocus());
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

        expect(screen.getByRole('treeitem', {name: 'Summand 1: Funktion Nicht, Wahrheitswert'}))
            .toHaveAccessibleDescription('Typkonflikt: Erwartet wird „Zahlenwert“, geliefert wird „Wahrheitswert“.');
        expect(screen.getByRole('treeitem', {name: 'Summand 2: zwei, Zahlenwert'}))
            .toHaveAccessibleDescription('Typkonflikt: Erwartet wird „Zahlenwert“, „zwei“ lässt sich nicht umwandeln.');
    });

    it('offers only type-compatible targets when moving', () => {
        render(<Harness onChange={vi.fn()}
                        initial={typed}/>);
        const negation = screen.getByRole('treeitem', {name: 'Wert 2: Funktion Nicht, Wahrheitswert'});
        negation.focus();

        fireEvent.keyDown(negation, {key: ' '});
        fireEvent.keyDown(negation, {key: 'ArrowDown'});
        fireEvent.keyDown(negation, {key: 'ArrowDown'});

        // The empty number slot of "Addiere" and the swap with "1" would put a truth value into a number.
        expect(screen.getByRole('status')).toHaveTextContent('Ziel: Wert 1: Funktion Addiere, Zahlenwert, die Werte werden getauscht.');
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

        expect(await screen.findByRole('treeitem', {name: 'Wert 1: Funktion Addiere, Zahlenwert'}))
            .toHaveAccessibleDescription('Testergebnis: 43');
        expect(screen.getByRole('treeitem', {name: 'Summand 1: Betrag, Zahlenwert'})).toHaveAccessibleDescription('Testergebnis: 42');
        expect(screen.getByText('Fehler: Division durch Null ist nicht erlaubt')).toBeInTheDocument();
        expect(screen.getByRole('treeitem', {name: 'Bedingung: Funktion Ist Gleich, Wahrheitswert'}))
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

    it.each([true, false])('shows the selected if result and keeps the skipped branch editable when the condition is %s', async condition => {
        const conditional: NoCodeOperatorDetailsDTO = {
            ...equals,
            identifier: 'if',
            label: 'Wenn, dann, sonst',
            signatures: [{
                returnType: NoCodeDataType.Runtime,
                parameters: [
                    {...not.signatures[0].parameters[0], label: 'Bedingung'},
                    {...equals.signatures[0].parameters[0], label: 'Ergebnis bei Wahr'},
                    {...equals.signatures[0].parameters[1], label: 'Ergebnis bei Falsch'},
                ],
            }],
        };
        const initial: NoCodeOperand = {
            type: 'NoCodeExpression',
            operatorIdentifier: 'if',
            operands: [
                {type: 'NoCodeStaticValue', value: String(condition)},
                {type: 'NoCodeExpression', operatorIdentifier: 'not', operands: [
                    {type: 'NoCodeExpression', operatorIdentifier: 'always-true', operands: []},
                ]},
                {type: 'NoCodeStaticValue', value: 'Alternative'},
            ],
        };
        const evaluated = (value: unknown) => ({status: NoCodeDryRunStatus.Evaluated, value, message: null, operands: []});
        const skipped = {
            status: NoCodeDryRunStatus.Skipped, value: null, operands: [],
            message: condition ? 'Die Bedingung ist wahr.' : 'Die Bedingung ist falsch.',
        };
        api.post.mockResolvedValue({
            ...evaluated(condition ? false : 'Alternative'),
            operands: [
                evaluated(String(condition)),
                condition ? {...evaluated(false), operands: [evaluated(true)]} : skipped,
                condition ? skipped : evaluated('Alternative'),
            ],
        });
        render(<Harness initial={initial} returnType={NoCodeDataType.Runtime} operators={[conditional, not, alwaysTrue]} onChange={vi.fn()}/>);
        fireEvent.click(screen.getByRole('button', {name: 'Testlauf'}));
        fireEvent.click(screen.getByRole('button', {name: 'Test ausführen'}));
        const result = await screen.findByRole('region', {name: 'Gesamtergebnis'});
        const expectedResult = condition ? 'Falsch' : '„Alternative“';
        expect(within(result).getByText(expectedResult)).toBeVisible();
        expect(result).toHaveTextContent(condition ? 'Datentyp: Wahrheitswert' : 'Datentyp: Textwert');
        expect(screen.getByRole('treeitem', {name: /Funktion Wenn, dann, sonst/})).toHaveAccessibleDescription(`Testergebnis: ${expectedResult}`);
        const thenBranch = screen.getByRole('treeitem', {name: 'Ergebnis bei Wahr: Funktion Nicht, Wahrheitswert'});
        const elseBranch = screen.getByRole('treeitem', {name: 'Ergebnis bei Falsch: Alternative, Beliebiger Wert'});
        const nested = within(thenBranch).getByRole('treeitem', {name: 'Wert: Funktion Immer wahr, Wahrheitswert'});
        expect(condition ? elseBranch : thenBranch).toHaveAccessibleDescription(`Nicht ausgewertet: ${skipped.message}`);
        if (condition) {
            expect(thenBranch).toHaveAccessibleDescription('Testergebnis: Falsch');
            expect(nested).toHaveAccessibleDescription('Testergebnis: Wahr');
        } else {
            expect(nested).not.toHaveAccessibleDescription();
            expect(elseBranch).not.toHaveAccessibleDescription();
        }

        fireEvent.click(condition ? within(elseBranch).getByRole('button', {name: 'Wert „Alternative“ löschen'})
            : within(nested).getByRole('button', {name: 'Funktion „Immer wahr“ löschen'}));
        fireEvent.click(screen.getByRole('button', {name: 'Test ausführen'}));
        const emptySlot = condition ? 'Ergebnis bei Falsch: leer, Beliebiger Wert' : 'Wert: leer, Wahrheitswert';
        expect(screen.getByRole('treeitem', {name: emptySlot})).toHaveAttribute('aria-invalid', 'true');
        expect(api.post).toHaveBeenCalledTimes(1);
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
    it.each([
        [NoCodeDataType.Date, '2026-11-22', '2026-11-23', '22.11.2026', '23.11.2026'],
        [NoCodeDataType.Time, '09:00', '17:00:00', '09:00:00 Uhr', '17:00:00 Uhr'],
        [NoCodeDataType.DateTime, '2026-11-22T12:30:00Z', '2026-11-22T13:30:00Z', '22.11.2026 13:30:00 Uhr', '22.11.2026 14:30:00 Uhr'],
    ] as const)('reads a fixed %s comparison as a complete conditional and collapsed sentence', (type, left, right, displayedLeft, displayedRight) => {
        const comparison = temporalComparison(type);
        const [first, , second] = comparison.signatures[0].parameters;
        const initial: NoCodeOperand = {
            type: 'NoCodeExpression', operatorIdentifier: comparison.identifier,
            operands: [{type: 'NoCodeStaticValue', value: left}, {type: 'NoCodeStaticValue', value: 'vor-oder-gleich'}, {type: 'NoCodeStaticValue', value: right}],
        };
        const original = structuredClone(initial);
        const onChange = vi.fn();
        render(<Harness initial={initial} operators={[comparison]} onChange={onChange}/>);

        const root = screen.getByRole('treeitem', {name: `Bedingung: Funktion ${comparison.label}, Wahrheitswert`});
        expect(root.firstElementChild).toHaveTextContent('ergibt wahr');
        expect(screen.getByRole('treeitem', {name: `${first.label}: ${displayedLeft}, ${NoCodeDataTypeLabels[type]}`})).toHaveTextContent(/^wenn/);
        expect(screen.getByRole('treeitem', {name: `${second.label}: ${displayedRight}, ${NoCodeDataTypeLabels[type]}`})).toHaveTextContent(/ist$/);
        fireEvent.click(screen.getByRole('button', {name: `${comparison.label}: zuklappen`}));
        expect(root).toHaveAccessibleDescription(`${displayedLeft} ist früher als oder gleich ${displayedRight}`);
        expect(initial).toEqual(original);
        expect(onChange).not.toHaveBeenCalled();
        expect(api.post).not.toHaveBeenCalled();
    });

    it('uses parameter names for a dynamic comparison and restores the sentence when a fixed choice is selected', async () => {
        const user = userEvent.setup();
        const comparison = temporalComparison(NoCodeDataType.Time);
        const initial: NoCodeOperand = {
            type: 'NoCodeExpression', operatorIdentifier: comparison.identifier,
            operands: [{type: 'NoCodeStaticValue', value: '09:00:00'}, {type: 'NoCodeProcessDataReference', path: 'vergleich'}, {type: 'NoCodeStaticValue', value: '17:00:00'}],
        };
        const onChange = vi.fn();
        render(<Harness initial={initial} operators={[comparison]} onChange={onChange}/>);
        const root = screen.getByRole('treeitem', {name: 'Bedingung: Funktion Uhrzeiten vergleichen, Wahrheitswert'});

        expect(root.firstElementChild).toHaveTextContent('Ergebnistyp: Wahrheitswert');
        expect(root.firstElementChild).not.toHaveTextContent('ergibt wahr');
        expect(screen.getByRole('treeitem', {name: 'Uhrzeit: 09:00:00 Uhr, Uhrzeitwert'})).toHaveTextContent(/^Uhrzeit/);
        expect(screen.getByRole('treeitem', {name: 'Vergleichsuhrzeit: 17:00:00 Uhr, Uhrzeitwert'})).toHaveTextContent(/^Vergleichsuhrzeit/);
        expect(screen.queryByText('wenn')).not.toBeInTheDocument();
        expect(screen.queryByText('ist')).not.toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', {name: 'Uhrzeiten vergleichen: zuklappen'}));
        expect(root).toHaveAccessibleDescription('Uhrzeiten vergleichen: Uhrzeit: 09:00:00 Uhr; Vergleichsart: Vorgangsdaten → vergleich; Vergleichsuhrzeit: 17:00:00 Uhr');
        fireEvent.click(screen.getByRole('button', {name: 'Uhrzeiten vergleichen: aufklappen'}));

        const mode = screen.getByRole('treeitem', {name: 'Vergleichsart: Vorgangsdaten → vergleich, Beliebiger Wert'});
        fireEvent.keyDown(mode, {key: 'Delete'});
        const empty = screen.getByRole('treeitem', {name: 'Vergleichsart: leer, Textwert'});
        fireEvent.keyDown(empty, {key: 'Enter'});
        await user.click(screen.getByRole('button', {name: 'früher als'}));

        expect(root.firstElementChild).toHaveTextContent('ergibt wahr');
        expect(screen.getByRole('treeitem', {name: 'Uhrzeit: 09:00:00 Uhr, Uhrzeitwert'})).toHaveTextContent(/^wenn/);
        expect(screen.getByRole('treeitem', {name: 'Vergleichsuhrzeit: 17:00:00 Uhr, Uhrzeitwert'})).toHaveTextContent(/ist$/);
        expect(onChange).toHaveBeenLastCalledWith({...initial, operands: [initial.operands![0], {type: 'NoCodeStaticValue', value: 'vor'}, initial.operands![2]]});
        expect(api.post).not.toHaveBeenCalled();
    });

    it.each([
        [currentToday, 'Datumswert'],
        [currentNow, 'Datums- und Zeitwert'],
    ] as const)('shows the parameterless time function $0.label without value inputs or a collapse action', (operator, typeLabel) => {
        const onChange = vi.fn();
        render(<Harness onChange={onChange}
                        initial={{type: 'NoCodeExpression', operatorIdentifier: operator.identifier, operands: []}}
                        returnType={operator.signatures[0].returnType}
                        operators={[operator]}/>);

        const item = screen.getByRole('treeitem', {name: `Bedingung: Funktion ${operator.label}, ${typeLabel}`});
        expect(item).not.toHaveAttribute('aria-expanded');
        expect(within(item).getByText(`Ergebnistyp: ${typeLabel}`)).toBeInTheDocument();
        expect(screen.getByRole('button', {name: `${operator.label}, ${typeLabel}`})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: `${operator.label}: zuklappen`})).not.toBeInTheDocument();
        expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
        expect(onChange).not.toHaveBeenCalled();
        expect(api.post).not.toHaveBeenCalled();
    });

    it('keeps today readable when a surrounding date calculation is collapsed without changing the draft', () => {
        const subtract = dateArithmetic('subtract-from-date', 'Vom Datum abziehen', 'minus');
        subtract.signatures[0].parameters[2].options.push({label: 'Jahre', value: 'jahre', singularLabel: 'Jahr'});
        const initial: NoCodeOperand = {
            type: 'NoCodeExpression',
            operatorIdentifier: subtract.identifier,
            operands: [
                {type: 'NoCodeExpression', operatorIdentifier: 'create-today', operands: []},
                {type: 'NoCodeStaticValue', value: '18'},
                {type: 'NoCodeStaticValue', value: 'jahre'},
            ],
        };
        const original = structuredClone(initial);
        const onChange = vi.fn();
        render(<Harness onChange={onChange} initial={initial}
                        returnType={NoCodeDataType.Date} operators={[subtract, currentToday]}/>);

        const today = screen.getByRole('treeitem', {name: 'Ausgangsdatum: Funktion Heutiges Datum, Datumswert'});
        expect(today).toHaveTextContent(/^das Datum.*Heutiges Datum/);
        expect(today).not.toHaveAttribute('aria-expanded');
        fireEvent.click(screen.getByRole('button', {name: 'Vom Datum abziehen: zuklappen'}));
        expect(screen.getByRole('treeitem', {name: 'Bedingung: Funktion Vom Datum abziehen, Datumswert'}))
            .toHaveAccessibleDescription('(heutiges Datum) minus 18 Jahre');
        fireEvent.click(screen.getByRole('button', {name: 'Vom Datum abziehen: aufklappen'}));
        expect(screen.getByRole('treeitem', {name: 'Ausgangsdatum: Funktion Heutiges Datum, Datumswert'})).toBeInTheDocument();
        expect(initial).toEqual(original);
        expect(onChange).not.toHaveBeenCalled();
        expect(api.post).not.toHaveBeenCalled();
    });

    it.each([
        ['subtract-from-date', 'Vom Datum abziehen', NoCodeDataType.Date, '2026-08-15'],
        ['subtract-from-datetime', 'Vom Zeitpunkt abziehen', NoCodeDataType.DateTime, '2026-11-22T12:30:00Z'],
    ] as const)('explains an invalid quantity of %s at its field and clears the error after correction', async (identifier, label, type, value) => {
        const onChange = vi.fn();
        render(<Harness onChange={onChange}
                        returnType={type}
                        operators={[dateArithmetic(identifier, label, 'minus', type)]}
                        initial={dateArithmeticExpression(identifier, value)}/>);
        fireEvent.click(screen.getByRole('button', {name: '1, Zahlenwert'}));
        const input = await screen.findByRole('textbox', {name: /^Anzahl/});

        fireEvent.change(input, {target: {value: '1.5'}});
        expect(input).toHaveAttribute('aria-invalid', 'true');
        expect(input).toHaveAccessibleDescription('Geben Sie für „Anzahl“ eine ganze Zahl ein.');
        fireEvent.change(input, {target: {value: '2147483648'}});
        expect(input).toHaveAttribute('aria-invalid', 'true');
        expect(input).toHaveAccessibleDescription('Geben Sie für „Anzahl“ eine Zahl zwischen −2.147.483.648 und 2.147.483.647 ein.');

        for (const valid of [' 5 ', '.0']) {
            fireEvent.change(input, {target: {value: valid}});
            expect(input).not.toHaveAttribute('aria-invalid', 'true');
            expect(screen.queryByText(/Typkonflikt/)).not.toBeInTheDocument();
            expect(onChange).toHaveBeenLastCalledWith({
                type: 'NoCodeExpression',
                operatorIdentifier: identifier,
                operands: [
                    {type: 'NoCodeStaticValue', value},
                    {type: 'NoCodeStaticValue', value: valid},
                    {type: 'NoCodeStaticValue', value: 'tage'},
                ],
            });
        }
        fireEvent.keyDown(input, {key: 'Escape'});
        await waitFor(() => expect(screen.queryByRole('textbox', {name: /^Anzahl/})).not.toBeInTheDocument());
        expect(screen.getByRole('treeitem', {name: 'Anzahl: .0, Zahlenwert'})).not.toHaveAttribute('aria-invalid', 'true');
        expect(screen.queryByText(/Typkonflikt/)).not.toBeInTheDocument();
    });

    it.each([
        ['subtract-from-date', 'Vom Datum abziehen', 'minus', NoCodeDataType.Date, '2026-08-15', '15.08.2026'],
        ['add-to-date', 'Zum Datum hinzufügen', 'plus', NoCodeDataType.Date, '2026-08-15', '15.08.2026'],
        ['subtract-from-datetime', 'Vom Zeitpunkt abziehen', 'minus', NoCodeDataType.DateTime, '2026-11-22T12:30:00Z', '22.11.2026 13:30:00 Uhr'],
        ['add-to-datetime', 'Zum Zeitpunkt hinzufügen', 'plus', NoCodeDataType.DateTime, '2026-11-22T12:30:00Z', '22.11.2026 13:30:00 Uhr'],
    ] as const)('keeps the expanded pills and collapsed reading consistent when editing the quantity of %s', async (identifier, label, direction, type, value, displayedValue) => {
        const onChange = vi.fn();
        const initial = dateArithmeticExpression(identifier, value);
        const typeLabel = type === NoCodeDataType.DateTime ? 'Datums- und Zeitwert' : 'Datumswert';
        const parameterLabel = type === NoCodeDataType.DateTime ? 'Ausgangszeitpunkt' : 'Ausgangsdatum';
        const phrase = type === NoCodeDataType.DateTime ? 'den Zeitpunkt' : 'das Datum';
        render(<Harness onChange={onChange}
                        returnType={type}
                        operators={[dateArithmetic(identifier, label, direction, type)]}
                        initial={initial}/>);

        const startingValue = screen.getByRole('treeitem', {name: `${parameterLabel}: ${displayedValue}, ${typeLabel}`});
        expect(startingValue).toHaveTextContent(new RegExp(`^${phrase}`));
        expect(startingValue).toHaveTextContent(displayedValue);
        expect(screen.getByRole('treeitem', {name: 'Einheit: Tag, Textwert'})).toBeInTheDocument();
        expect(onChange).not.toHaveBeenCalled();
        fireEvent.click(screen.getByRole('button', {name: `${label}: zuklappen`}));
        const root = screen.getByRole('treeitem', {name: `Bedingung: Funktion ${label}, ${typeLabel}`});
        expect(root).toHaveAccessibleDescription(`${displayedValue} ${direction} 1 Tag`);

        fireEvent.click(screen.getByRole('button', {name: `${label}: aufklappen`}));
        fireEvent.click(screen.getByRole('button', {name: '1, Zahlenwert'}));
        const input = await screen.findByRole('textbox', {name: /^Anzahl/});
        fireEvent.change(input, {target: {value: '2'}});
        fireEvent.keyDown(input, {key: 'Escape'});
        await waitFor(() => expect(screen.queryByRole('textbox', {name: /^Anzahl/})).not.toBeInTheDocument());
        expect(screen.getByRole('treeitem', {name: 'Einheit: Tage, Textwert'})).toBeInTheDocument();
        expect(onChange).toHaveBeenLastCalledWith({
            ...initial,
            operands: [
                {type: 'NoCodeStaticValue', value},
                {type: 'NoCodeStaticValue', value: '2'},
                {type: 'NoCodeStaticValue', value: 'tage'},
            ],
        });
        fireEvent.click(screen.getByRole('button', {name: `${label}: zuklappen`}));
        expect(root).toHaveAccessibleDescription(`${displayedValue} ${direction} 2 Tage`);
    });

    it('offers singular unit labels for one and stores the original option value when selecting a different unit', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        render(<Harness onChange={onChange}
                        returnType={NoCodeDataType.Date}
                        operators={[dateArithmetic('subtract-from-date', 'Vom Datum abziehen', 'minus')]}
                        initial={dateArithmeticExpression('subtract-from-date')}/>);
        await user.click(screen.getByRole('button', {name: 'Tag, Textwert'}));
        const input = await screen.findByRole('combobox', {name: 'Einheit'});
        expect(input).toHaveTextContent('Tag');
        await user.click(input);
        await user.click(await screen.findByRole('option', {name: 'Woche'}));

        expect(onChange).toHaveBeenLastCalledWith({
            type: 'NoCodeExpression',
            operatorIdentifier: 'subtract-from-date',
            operands: [
                {type: 'NoCodeStaticValue', value: '2026-08-15'},
                {type: 'NoCodeStaticValue', value: '1'},
                {type: 'NoCodeStaticValue', value: 'wochen'},
            ],
        });
        await user.keyboard('{Escape}');
        await waitFor(() => expect(screen.queryByRole('combobox', {name: 'Einheit'})).not.toBeInTheDocument());
        expect(screen.getByRole('treeitem', {name: 'Einheit: Woche, Textwert'})).toBeInTheDocument();
    });

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
    const sentenceNot: NoCodeOperatorDetailsDTO = {
        ...not,
        sentenceLead: 'ergibt wahr',
        humanReadableTemplate: 'nicht „#0“',
        signatures: [{
            returnType: NoCodeDataType.Boolean,
            parameters: [{
                type: NoCodeDataType.Boolean,
                label: 'Bedingung',
                description: 'Die Bedingung, deren Ergebnis Sie umkehren möchten.',
                sentencePhrase: 'wenn',
                sentenceSuffix: 'falsch ist',
                options: [{label: 'Wahr', value: 'true'}, {label: 'Falsch', value: 'false'}],
            }],
        }],
    };
    const operatorItemName = 'Bedingung: Funktion Ist Gleich, Wahrheitswert';
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
        const nestedItem = screen.getByRole('treeitem', {name: 'Wert 2: Funktion Text verketten, Textwert'});
        expect(nestedItem).toHaveTextContent(/^gleich.*Text verketten.*ergibt.*a.*gefolgt von.*b.*ist$/);
        expect(screen.queryByText('Text 1')).not.toBeInTheDocument();
        expect(screen.getByRole('treeitem', {name: 'Text 1: a, Textwert'})).toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', {name: 'Text verketten: zuklappen'}));
        expect(nestedItem).toHaveTextContent(/ist$/);
    });

    it('shows a complete readable sentence below the collapsed header', () => {
        const onChange = vi.fn();
        render(<Harness onChange={onChange}
                        operators={[{
                            ...sentenceEquals,
                            humanReadableTemplate: '#0 ist gleich #1',
                        }]}/>);
        fireEvent.click(screen.getByRole('button', {name: 'Ist Gleich: zuklappen'}));
        const root = screen.getByRole('treeitem', {name: operatorItemName});
        expect(root.firstElementChild).toHaveTextContent('Ist Gleich');
        expect(root.firstElementChild).not.toHaveTextContent('„a“');
        expect(root).not.toHaveTextContent('ergibt wahr');
        expect(root).toHaveAccessibleDescription('„a“ ist gleich „b“');
        expect(screen.getByText('„a“ ist gleich „b“')).toBeInTheDocument();
        expect(screen.queryByRole('treeitem', {name: 'Wert 1: a, Beliebiger Wert'})).not.toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', {name: 'Ist Gleich: aufklappen'}));
        expect(screen.getByRole('treeitem', {name: 'Wert 1: a, Beliebiger Wert'})).toBeInTheDocument();
        expect(root).not.toHaveAttribute('aria-describedby');
        expect(onChange).not.toHaveBeenCalled();
    });

    it.each([['true', 'Wahr'], ['false', 'Falsch']])('reads a fixed Boolean %s without quotes after collapsing', (value, label) => {
        const onChange = vi.fn();
        render(<Harness onChange={onChange}
                        operators={[sentenceNot]}
                        initial={{type: 'NoCodeExpression', operatorIdentifier: 'not', operands: [{type: 'NoCodeStaticValue', value}]}}/>);
        const root = screen.getByRole('treeitem', {name: 'Bedingung: Funktion Nicht, Wahrheitswert'});
        expect(within(root).getByRole('treeitem', {name: `Bedingung: ${label}, Wahrheitswert`}))
            .toHaveTextContent(new RegExp(`^wenn.*${label}.*falsch ist$`));

        fireEvent.click(screen.getByRole('button', {name: 'Nicht: zuklappen'}));
        expect(root).toHaveAccessibleDescription(`nicht ${label}`);
        expect(screen.getByText(`nicht ${label}`)).toBeVisible();
        expect(onChange).not.toHaveBeenCalled();
    });

    it('also summarizes an individually collapsed nested expression without changing its parent', () => {
        const onChange = vi.fn();
        render(<Harness onChange={onChange}
                        operators={[sentenceEquals, {...not, humanReadableTemplate: 'nicht #0'}]}
                        initial={nested}/>);
        const root = screen.getByRole('treeitem', {name: operatorItemName});
        const child = screen.getByRole('treeitem', {name: 'Wert 2: Funktion Nicht, Wahrheitswert'});
        fireEvent.click(screen.getByRole('button', {name: 'Nicht: zuklappen'}));

        expect(root).toHaveAttribute('aria-expanded', 'true');
        expect(child).toHaveAttribute('aria-expanded', 'false');
        expect(child).toHaveAccessibleDescription('nicht Fehlender Parameter „Wert“');
        expect(screen.getByRole('treeitem', {name: 'Wert 1: a, Beliebiger Wert'})).toBeInTheDocument();
        expect(onChange).not.toHaveBeenCalled();
    });

    it.each([false, true])('reveals technical details by keyboard without expanding or editing the expression (readOnly: %s)', async (readOnly) => {
        const user = userEvent.setup();
        const onChange = vi.fn();
        render(<Harness onChange={onChange}
                        readOnly={readOnly}
                        operators={[dateComparison]}
                        initial={{
                            ...incompleteComparison,
                            operands: [
                                {type: 'NoCodeStaticValue', value: '2000-01-01'},
                                {type: 'NoCodeStaticValue', value: 'vor-oder-gleich'},
                                {type: 'NoCodeStaticValue', value: '2025-01-01'},
                            ],
                        } as NoCodeOperand}/>);
        fireEvent.click(screen.getByRole('button', {name: 'Datumswerte vergleichen: zuklappen'}));
        const root = screen.getByRole('treeitem', {name: 'Bedingung: Funktion Datumswerte vergleichen, Wahrheitswert'});
        expect(root).toHaveTextContent('früher als oder gleich');
        expect(screen.getByText('vor-oder-gleich')).not.toBeVisible();

        const details = screen.getByRole('button', {name: 'Technische Details'});
        details.focus();
        await user.keyboard('{ArrowRight}{Home}{End}');
        expect(details).toHaveFocus();
        expect(root).toHaveAttribute('aria-expanded', 'false');
        await user.keyboard('{Enter}');
        expect(details).toHaveAttribute('aria-expanded', 'true');
        expect(screen.getByText('vor-oder-gleich')).toBeVisible();
        await user.keyboard(' ');
        expect(details).toHaveAttribute('aria-expanded', 'false');
        expect(screen.getByText('vor-oder-gleich')).not.toBeVisible();
        expect(root).toHaveAttribute('aria-expanded', 'false');
        expect(onChange).not.toHaveBeenCalled();
    });

    it('expands only the reading text and keeps the expression collapsed', async () => {
        vi.spyOn(HTMLElement.prototype, 'scrollHeight', 'get').mockReturnValue(100);
        vi.spyOn(HTMLElement.prototype, 'clientHeight', 'get').mockReturnValue(60);
        const user = userEvent.setup();
        const onChange = vi.fn();
        render(<Harness onChange={onChange}
                        operators={[{...sentenceEquals, humanReadableTemplate: '#0 ist gleich #1'}]}/>);
        fireEvent.click(screen.getByRole('button', {name: 'Ist Gleich: zuklappen'}));
        const root = screen.getByRole('treeitem', {name: operatorItemName});
        const expand = screen.getByRole('button', {name: 'Vollständig anzeigen'});
        expand.focus();
        await user.keyboard('{Enter}');
        expect(screen.getByRole('button', {name: 'Weniger anzeigen'})).toHaveAttribute('aria-expanded', 'true');
        expect(root).toHaveAttribute('aria-expanded', 'false');
        expect(screen.queryByRole('treeitem', {name: 'Wert 1: a, Beliebiger Wert'})).not.toBeInTheDocument();
        await user.keyboard(' ');
        expect(screen.getByRole('button', {name: 'Vollständig anzeigen'})).toHaveAttribute('aria-expanded', 'false');
        expect(onChange).not.toHaveBeenCalled();
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

    it.each(['Escape', 'Schließen'])('closes the function description with %s and returns focus', async (method) => {
        const onChange = vi.fn();
        render(<Harness onChange={onChange}
                        operators={[sentenceEquals]}/>);
        const operatorItem = screen.getByRole('treeitem', {name: operatorItemName});

        operatorItem.focus();
        fireEvent.keyDown(operatorItem, {
            key: 'F10',
            shiftKey: true,
        });
        fireEvent.click(await screen.findByRole('menuitem', {name: 'Beschreibung anzeigen'}));
        const dialog = await screen.findByRole('dialog', {name: 'Ist Gleich'});
        const closeButton = within(dialog).getAllByRole('button', {name: 'Schließen'}).at(-1)!;
        expect(closeButton).toHaveClass('MuiButton-text');
        if (method === 'Escape') fireEvent.keyDown(dialog, {key: 'Escape'});
        else fireEvent.click(closeButton);

        await waitFor(() => expect(operatorItem).toHaveFocus());
        expect(onChange).not.toHaveBeenCalled();
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
        expect(tooltip).toHaveTextContent('Ergibt den festgelegten Wert „a“.');
        expect(tooltip).toHaveTextContent('Wert 1: Der erste Vergleichswert.');
        fireEvent.click(within(tooltip).getByRole('button', {name: 'Mehr...'}));

        const dialog = await screen.findByRole('dialog', {name: 'Wert selbst festlegen'});
        expect(dialog).toHaveTextContent('Einen Wert direkt eingeben oder aus verfügbaren Optionen auswählen.');
        expect(dialog).toHaveTextContent('Der festgelegte Wert wird bei jeder Ausführung unverändert verwendet.');
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
        expect(await screen.findByRole('tooltip')).toHaveTextContent(`Ergibt den festgelegten Wert „${label}“.`);
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

        expect(await screen.findByRole('tooltip')).toHaveTextContent('Ergibt den aktuellen Wert des Formularelements „Betrag“.');
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
        const dialog = await screen.findByRole('dialog', {name: 'Wert selbst festlegen'});
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
    const root = screen.getByRole('treeitem', {name: 'Bedingung: Funktion Ist Gleich, Wahrheitswert'});
    root.focus();
    fireEvent.keyDown(root, {key: 'Delete'});
    const dialog = screen.getByRole('dialog', {name: 'Funktion entfernen?'});
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
    expect(screen.getByRole('treeitem', {name: 'Bedingung: Funktion Ist Gleich, Wahrheitswert'})).toHaveFocus();
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
    fireEvent.click(screen.getByRole('button', {name: 'Funktion „Nicht“ löschen'}));
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
    const root = screen.getByRole('treeitem', {name: 'Bedingung: Funktion Ist Gleich, Wahrheitswert'});
    fireEvent.keyDown(root, {
        key: 'F10',
        shiftKey: true,
    });
    fireEvent.click(await screen.findByRole('menuitem', {name: 'Löschen'}));
    const dialog = await screen.findByRole('dialog', {name: 'Funktion entfernen?'});
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
    const child = screen.getByRole('treeitem', {name: 'Wert 2: Funktion Nicht, Wahrheitswert'});
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
    fireEvent.click(screen.getByRole('button', {name: 'Mit Funktion ermitteln'}));
    const picker = await screen.findByRole('dialog', {name: 'Funktion auswählen'});
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
    fireEvent.click(screen.getByRole('button', {name: 'Wert „a“ löschen'}));
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
        expect(screen.getByRole('button', {name: 'Wert „a“ löschen'})).toBeEnabled();
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
    for (const surface of [row, screen.getByText('Wert 1'), screen.getByRole('button', {name: 'Wert „a“ löschen'}), screen.getByRole('button', {name: 'Aktionen für „a“'})]) {
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
    const name = 'Bedingung: Funktion Ist Gleich, Wahrheitswert';
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
