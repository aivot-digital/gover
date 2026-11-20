import {useState} from 'react';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import {NoCodeTestData} from '../../../models/functions/no-code-editor-metadata';
import {act, fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {NoCodeDryRunPanel} from './no-code-dry-run-panel';
import {NoCodeDryRunResultDTO, NoCodeDryRunStatus as Status} from '../../../models/dtos/no-code-dry-run-dto';
import {toNoCodeEditorNode} from '../utils/no-code-editor-store';
import {NoCodeOperand} from '../../../models/functions/no-code-expression';
import {ElementType} from '../../../data/element-type/element-type';
import {InputVariableSource} from '../../../models/input-mode';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';

const api = vi.hoisted(() => ({post: vi.fn()}));
vi.mock('../../../hooks/use-api', () => ({useApi: () => api}));
vi.mock('../../../components/code-editor/monaco-editor', () => ({
    MonacoEditor: (props: import('@monaco-editor/react').EditorProps) => <textarea
        aria-label={props.options?.ariaLabel}
        value={props.value ?? ''}
        onChange={event => props.onChange?.(event.target.value, {} as never)}/>,
}));
vi.mock('../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));
const evaluated = (value: unknown): NoCodeDryRunResultDTO => ({
    status: Status.Evaluated,
    value,
    message: null,
    operands: [],
});
const number = {
    ...generateElementWithDefaultValues(ElementType.Number),
    id: 'amount',
    label: 'Betrag',
};
const text = {
    ...generateElementWithDefaultValues(ElementType.Text),
    id: 'name',
    label: 'Name',
};
const elements = [{
    element: number,
    parents: [],
}, {
    element: text,
    parents: [],
}];
const literal: NoCodeOperand = {
    type: 'NoCodeStaticValue',
    value: 'test',
};

function setup(operand: NoCodeOperand = literal, run = true) {
    const root = toNoCodeEditorNode(operand);
    const onResults = vi.fn();
    const view = render(<NoCodeDryRunPanel root={root}
                                           allElements={elements}
                                           onResults={onResults}/>);
    fireEvent.click(screen.getByRole('button', {name: 'Testlauf'}));
    if (run) fireEvent.click(screen.getByRole('button', {name: 'Test ausführen'}));
    return {
        ...view,
        root,
        onResults,
    };
}

beforeEach(() => {
    api.post.mockReset().mockResolvedValue(evaluated('test'));
});

describe('NoCodeDryRunPanel', () => {
    it('places the run action after the inputs and directly before the result', async () => {
        setup({
            type: 'NoCodeReference',
            elementId: 'amount',
        }, false);
        const input = screen.getByRole('textbox', {name: /^Testwert für „Betrag“/});
        const run = screen.getByRole('button', {name: 'Test ausführen'});
        expect(input.compareDocumentPosition(run) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
        fireEvent.click(run);
        const result = await screen.findByRole('region', {name: 'Gesamtergebnis'});
        await waitFor(() => expect(run).toBeEnabled());
        expect(within(result).getByRole('heading', {
            name: 'Gesamtergebnis',
            level: 4,
        })).toBeInTheDocument();
        expect(run.nextElementSibling).toBe(result);
        expect(screen.queryByRole('button', {name: 'Mit diesen Daten testen'})).not.toBeInTheDocument();
    });

    it('shows a typed overall result even for a lone fixed value', async () => {
        api.post.mockResolvedValue(evaluated(0));
        setup();
        expect(screen.getByRole('status')).toHaveTextContent('Testlauf wird ausgeführt');
        const result = await screen.findByRole('region', {name: 'Gesamtergebnis'});
        expect(result).toHaveTextContent('0');
        expect(result).toHaveTextContent('Datentyp: Zahlenwert');
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
    });

    it.each(['a'.repeat(300), {
        items: [{
            name: 'Lang',
            id: 7,
        }],
    }])('opens complete results using the keyboard: %j', async (value) => {
        const user = userEvent.setup();
        api.post.mockResolvedValue(evaluated(value));
        setup();
        const button = await screen.findByRole('button', {name: 'Gesamtergebnis ansehen'});
        button.focus();
        await user.keyboard('{Enter}');
        const dialog = screen.getByRole('dialog');
        const full = within(dialog).getByLabelText('Vollständiges Testergebnis');
        expect(full).toHaveAttribute('tabindex', '0');
        expect(full.textContent).toBe(typeof value === 'string' ? value : JSON.stringify(value, null, 2));
        await user.keyboard('{Escape}');
        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        expect(button).toHaveFocus();
    });

    it('invalidates an old result immediately and sends no request for invalid numeric input', async () => {
        api.post.mockResolvedValue(evaluated(42));
        setup({
            type: 'NoCodeReference',
            elementId: 'amount',
        });
        await screen.findByRole('region', {name: 'Gesamtergebnis'});
        const input = screen.getByRole('textbox', {name: /^Testwert für „Betrag“/});
        fireEvent.change(input, {target: {value: 'not a number'}});
        expect(screen.getByRole('region', {name: 'Gesamtergebnis'})).toHaveTextContent('Nicht mehr aktuell');
        expect(input).toHaveAttribute('aria-invalid', 'true');
        await new Promise((resolve) => setTimeout(resolve, 450));
        expect(api.post).toHaveBeenCalledTimes(1);
        fireEvent.change(input, {target: {value: '0'}});
        fireEvent.click(screen.getByRole('button', {name: 'Test ausführen'}));
        await waitFor(() => expect(api.post).toHaveBeenCalledTimes(2));
        expect(api.post.mock.lastCall![1].elementData.effectiveValues).toEqual({amount: 0});
    });

    it('represents missing, empty string and null separately with JSON input', async () => {
        const user = userEvent.setup();
        setup({
            type: 'NoCodeReference',
            elementId: 'name',
        });
        await waitFor(() => expect(api.post).toHaveBeenCalledTimes(1));
        expect(api.post.mock.lastCall![1].elementData.effectiveValues).toEqual({});
        await user.click(screen.getByRole('button', {name: /^Wertoptionen/}));
        await user.click(screen.getByRole('menuitemradio', {name: 'JSON'}));
        const input = screen.getByRole('textbox', {name: /^Testwert für „Name“/});
        fireEvent.change(input, {target: {value: '""'}});
        fireEvent.click(screen.getByRole('button', {name: 'Test ausführen'}));
        await waitFor(() => expect(api.post.mock.lastCall![1].elementData.effectiveValues).toEqual({name: ''}));
        fireEvent.change(input, {target: {value: 'null'}});
        fireEvent.click(screen.getByRole('button', {name: 'Test ausführen'}));
        await waitFor(() => expect(api.post.mock.lastCall![1].elementData.effectiveValues).toEqual({name: null}));
    });

    it('offers a rerun in the stale notice and removes the notice after the new result arrives', async () => {
        setup({
            type: 'NoCodeReference',
            elementId: 'amount',
        });
        await screen.findByRole('region', {name: 'Gesamtergebnis'});
        fireEvent.change(screen.getByRole('textbox', {name: /^Testwert für „Betrag“/}), {target: {value: '12'}});
        const result = screen.getByRole('region', {name: 'Gesamtergebnis'});
        const notice = within(result).getByRole('status');
        expect(notice).toHaveTextContent('Führen Sie den Test erneut aus, um das Ergebnis zu aktualisieren.');
        let resolve!: (value: NoCodeDryRunResultDTO) => void;
        api.post.mockImplementationOnce(() => new Promise(done => {
            resolve = done;
        }));

        fireEvent.click(within(notice).getByRole('button', {name: 'Erneut testen'}));

        expect(api.post).toHaveBeenCalledTimes(2);
        expect(api.post.mock.lastCall![1].elementData.effectiveValues).toEqual({amount: 12});
        expect(within(notice).getByRole('button', {name: 'Erneut testen'})).toBeDisabled();
        await act(async () => resolve(evaluated(12)));
        expect(within(result).queryByRole('status')).not.toBeInTheDocument();
        expect(result).toHaveTextContent('12');
    });

    it('offers retry after a request error and distinguishes skipped results', async () => {
        api.post.mockRejectedValueOnce(new Error('network')).mockResolvedValue({
            status: Status.Skipped,
            value: null,
            operands: [],
            message: 'Dieser Operator liest geschützte Daten.',
        });
        setup();
        fireEvent.click(await screen.findByRole('button', {name: 'Erneut testen'}));
        const result = await screen.findByRole('region', {name: 'Gesamtergebnis'});
        expect(result).toHaveTextContent('Nicht ausgewertet: Dieser Operator liest geschützte Daten.');
        expect(screen.queryByRole('button', {name: 'Erneut testen'})).not.toBeInTheDocument();
    });

    it('requires explicit visibility and submits the selected state instead of a default', async () => {
        const user = userEvent.setup();
        setup({
            type: 'NoCodeExpression',
            operatorIdentifier: 'is-visible',
            operands: [{
                type: 'NoCodeStaticValue',
                value: 'name',
            }],
        });
        await new Promise((resolve) => setTimeout(resolve, 450));
        expect(api.post).not.toHaveBeenCalled();
        await user.click(screen.getByRole('combobox', {name: /^Sichtbarkeit für „Name“/}));
        await user.click(screen.getByRole('option', {name: 'Nicht sichtbar'}));
        fireEvent.click(screen.getByRole('button', {name: 'Test ausführen'}));
        await waitFor(() => expect(api.post).toHaveBeenCalledTimes(1));
        expect(api.post.mock.lastCall![1].elementData.elementStates).toEqual({name: {visible: false}});
    });

    it('explains unsupported dynamic visibility instead of computing a guessed result', async () => {
        setup({
            type: 'NoCodeExpression',
            operatorIdentifier: 'is-invisible',
            operands: [{
                type: 'NoCodeProcessDataReference',
                path: 'fieldId',
            }],
        });
        expect(screen.getByText(/Die Element-ID für die Sichtbarkeitsprüfung wird dynamisch/)).toBeInTheDocument();
        await new Promise((resolve) => setTimeout(resolve, 450));
        expect(api.post).not.toHaveBeenCalled();
    });

    it('ignores responses that arrive after the panel is closed', async () => {
        let resolve!: (result: NoCodeDryRunResultDTO) => void;
        api.post.mockImplementation(() => new Promise((done) => {
            resolve = done;
        }));
        const {onResults} = setup();
        await waitFor(() => expect(api.post).toHaveBeenCalledTimes(1));
        fireEvent.click(screen.getByRole('button', {name: 'Testlauf'}));
        await act(async () => resolve(evaluated('old')));
        expect(onResults).toHaveBeenLastCalledWith(null);
        expect(screen.queryByRole('region', {name: 'Gesamtergebnis'})).not.toBeInTheDocument();
    });
});

it('aborts superseded requests and waits for another explicit test after typing', async () => {
    vi.useFakeTimers();
    try {
        api.post.mockImplementation(() => new Promise(() => {
        }));
        const {unmount} = setup({
            type: 'NoCodeReference',
            elementId: 'amount',
        });
        await act(async () => {
            await vi.advanceTimersByTimeAsync(400);
        });
        const first = api.post.mock.lastCall![2].abortController as AbortController;
        const input = screen.getByRole('textbox', {name: /^Testwert/});
        fireEvent.change(input, {target: {value: '1'}});
        expect(first.signal.aborted).toBe(true);
        await act(async () => {
            await vi.advanceTimersByTimeAsync(200);
        });
        fireEvent.change(input, {target: {value: '12'}});
        await act(async () => {
            await vi.advanceTimersByTimeAsync(399);
        });
        expect(api.post).toHaveBeenCalledTimes(1);
        await act(async () => {
            await vi.advanceTimersByTimeAsync(1);
        });
        expect(api.post).toHaveBeenCalledTimes(1);
        fireEvent.click(screen.getByRole('button', {name: 'Test ausführen'}));
        expect(api.post).toHaveBeenCalledTimes(2);
        expect(api.post.mock.lastCall![1].elementData.effectiveValues).toEqual({amount: 12});
        const second = api.post.mock.lastCall![2].abortController as AbortController;
        unmount();
        expect(second.signal.aborted).toBe(true);
    } finally {
        vi.useRealTimers();
    }
});

it('ends the waiting state after 30 seconds, ignores late responses and allows retry', async () => {
    vi.useFakeTimers();
    try {
        let resolve!: (value: NoCodeDryRunResultDTO) => void;
        api.post.mockImplementation(() => new Promise(done => {
            resolve = done;
        }));
        const {onResults} = setup();
        await act(async () => {
            await vi.advanceTimersByTimeAsync(30_400);
        });
        expect(screen.getByText(/Der Testlauf dauert zu lange/)).toBeInTheDocument();
        expect(screen.queryByText('Testlauf wird ausgeführt …')).not.toBeInTheDocument();
        expect(api.post.mock.lastCall![2].abortController.signal.aborted).toBe(true);
        await act(async () => resolve(evaluated('late')));
        expect(onResults).toHaveBeenLastCalledWith(null);
        expect(screen.queryByRole('region', {name: 'Gesamtergebnis'})).not.toBeInTheDocument();
        api.post.mockResolvedValue(evaluated('new'));
        fireEvent.click(screen.getByRole('button', {name: 'Erneut testen'}));
        await act(async () => {
            await vi.advanceTimersByTimeAsync(400);
        });
        expect(screen.getByRole('region', {name: 'Gesamtergebnis'})).toHaveTextContent('new');
    } finally {
        vi.useRealTimers();
    }
});


it('does not test on opening or typing and only shows input errors after blur or test', async () => {
    setup({
        type: 'NoCodeReference',
        elementId: 'amount',
    }, false);
    const input = screen.getByRole('textbox', {name: /^Testwert/});
    fireEvent.change(input, {target: {value: 'unfinished'}});
    expect(input).not.toHaveAttribute('aria-invalid', 'true');
    await new Promise((resolve) => setTimeout(resolve, 450));
    expect(api.post).not.toHaveBeenCalled();
    fireEvent.blur(input);
    expect(input).toHaveAttribute('aria-invalid', 'true');
    fireEvent.click(screen.getByRole('button', {name: 'Test ausführen'}));
    expect(api.post).not.toHaveBeenCalled();
    fireEvent.change(input, {target: {value: '12'}});
    expect(api.post).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole('button', {name: 'Test ausführen'}));
    await screen.findByRole('region', {name: 'Gesamtergebnis'});
    expect(api.post).toHaveBeenCalledOnce();
});

it('keeps an old result stale even when the inputs return to their previous value', async () => {
    setup({
        type: 'NoCodeReference',
        elementId: 'amount',
    });
    await screen.findByRole('region', {name: 'Gesamtergebnis'});
    const input = screen.getByRole('textbox', {name: /^Testwert/});
    fireEvent.change(input, {target: {value: '12'}});
    fireEvent.change(input, {target: {value: ''}});
    expect(screen.getByRole('region', {name: 'Gesamtergebnis'})).toHaveTextContent('Nicht mehr aktuell');
    expect(api.post).toHaveBeenCalledOnce();
});

async function choose(label: string, option: string) {
    if (label.startsWith('Eingabeformat für „')) {
        fireEvent.click(screen.getByRole('button', {name: label.replace('Eingabeformat', 'Wertoptionen')}));
        fireEvent.click(await screen.findByRole('menuitemradio', {name: option === 'Wert' ? 'Wert eingeben' : option}));
        await waitFor(() => expect(screen.queryByRole('menu')).not.toBeInTheDocument());
    } else if (label.startsWith('Datentyp')) {
        fireEvent.click(screen.getByRole('button', {name: name => name.startsWith(label)}));
        fireEvent.click(await screen.findByRole('menuitemradio', {name: option}));
        await waitFor(() => expect(screen.queryByRole('menu')).not.toBeInTheDocument());
    } else if (label.startsWith('Eingabeformat')) {
        fireEvent.click(within(screen.getByRole('group', {name: label})).getByRole('button', {name: option.startsWith('JSON') ? 'JSON' : option}));
    } else {
        fireEvent.mouseDown(screen.getByRole('combobox', {name: label}));
        fireEvent.click(await screen.findByRole('option', {name: option}));
    }
}

it('collects referenced values without JSON and keeps data sources separate', async () => {
    setup({
        type: 'NoCodeExpression',
        operatorIdentifier: 'equals',
        operands: [
            {
                type: 'NoCodeProcessDataReference',
                path: 'person.name',
            },
            {
                type: 'NoCodeProcessDataReference',
                path: 'person.name',
            },
            {
                type: 'NoCodeInstanceDataReference',
                path: 'caseNumber',
            },
            {
                type: 'NoCodeNodeDataReference',
                nodeDataKey: 'check',
                path: 'approved',
            },
        ],
    }, false);
    expect(screen.queryByRole('textbox', {name: /\(JSON\)/})).not.toBeInTheDocument();
    expect(screen.getAllByRole('textbox', {name: 'Testwert für „$.person.name“'})).toHaveLength(1);
    fireEvent.change(screen.getByRole('textbox', {name: 'Testwert für „$.person.name“'}), {target: {value: 'Anna'}});
    fireEvent.change(screen.getByRole('textbox', {name: 'Testwert für „$$.caseNumber“'}), {target: {value: 'V-123'}});
    await choose('Datentyp für „_.check.approved“', 'Wahrheitswert');
    await choose('Testwert für „_.check.approved“', 'Falsch');
    expect(api.post).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole('button', {name: 'Test ausführen'}));
    await waitFor(() => expect(api.post).toHaveBeenCalledOnce());
    expect(api.post.mock.lastCall![1]).toMatchObject({
        processData: {person: {name: 'Anna'}},
        instanceData: {caseNumber: 'V-123'},
        nodeData: {check: {approved: false}},
    });
});

it('preserves nested values and unrelated JSON across mode switches and after closing the panel', async () => {
    setup({
        type: 'NoCodeProcessDataReference',
        path: 'person.age',
    }, false);
    await choose('Datentyp für „$.person.age“', 'Zahlenwert');
    fireEvent.change(screen.getByRole('textbox', {name: 'Testwert für „$.person.age“'}), {target: {value: '0'}});
    await choose('Eingabeformat für Vorgangsdaten', 'JSON (Expertenmodus)');
    const json = screen.getByRole('textbox', {name: /^Vorgangsdaten \(JSON\)/});
    expect(JSON.parse((json as HTMLInputElement).value)).toEqual({person: {age: 0}});
    fireEvent.change(json, {target: {value: '{"person":{"age":0,"name":"Anna"},"keep":false}'}});
    await choose('Eingabeformat für Vorgangsdaten', 'Eingabefelder');
    expect(screen.getByRole('textbox', {name: 'Testwert für „$.person.age“'})).toHaveValue('0');
    expect(screen.getByRole('button', {name: 'Datentyp für „$.person.age“: Zahlenwert'})).toHaveTextContent('Zahl');
    fireEvent.click(screen.getByRole('button', {name: 'Testlauf'}));
    fireEvent.click(screen.getByRole('button', {name: 'Testlauf'}));
    fireEvent.change(screen.getByRole('textbox', {name: 'Testwert für „$.person.age“'}), {target: {value: '12'}});
    fireEvent.click(screen.getByRole('button', {name: 'Test ausführen'}));
    await waitFor(() => expect(api.post).toHaveBeenCalledOnce());
    expect(api.post.mock.lastCall![1].processData).toEqual({
        person: {
            age: 12,
            name: 'Anna',
        },
        keep: false,
    });
});

it('offers missing, null and empty text directly for process data', async () => {
    setup({
        type: 'NoCodeProcessDataReference',
        path: 'name',
    }, false);
    for (const [option, expected] of [['Leerer Text', {name: ''}], ['Leerer Wert (null)', {name: null}], ['Kein Wert (nicht vorhanden)', {}]] as const) {
        await choose('Eingabeformat für „$.name“', option);
        fireEvent.click(screen.getByRole('button', {name: 'Test ausführen'}));
        await waitFor(() => expect(api.post.mock.lastCall![1].processData).toEqual(expected));
        await waitFor(() => expect(screen.getByRole('button', {name: 'Test ausführen'})).not.toBeDisabled());
    }
});

it('explains JSON fallback for arrays and prevents lossy conversion of a parent value', async () => {
    const {
        rerender,
        onResults,
    } = setup({
        type: 'NoCodeProcessDataReference',
        path: 'people[0].name',
    }, false);
    expect(screen.getByRole('textbox', {name: /^Vorgangsdaten \(JSON\)/})).toBeInTheDocument();
    expect(screen.getByText(/Listenpfade/)).toBeInTheDocument();
    const root = toNoCodeEditorNode({
        type: 'NoCodeProcessDataReference',
        path: 'person.name',
    });
    rerender(<NoCodeDryRunPanel root={root}
                                allElements={elements}
                                onResults={onResults}/>);
    await choose('Eingabeformat für Vorgangsdaten', 'JSON (Expertenmodus)');
    fireEvent.change(screen.getByRole('textbox', {name: /^Vorgangsdaten \(JSON\)/}), {target: {value: '{"person":42}'}});
    expect(within(screen.getByRole('group', {name: 'Eingabeformat für Vorgangsdaten'})).getByRole('button', {name: 'Eingabefelder'})).toBeDisabled();
    expect(screen.getByText(/Ein übergeordneter Wert ist kein Datenbereich/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', {name: 'Test ausführen'}));
    await waitFor(() => expect(api.post.mock.lastCall![1].processData).toEqual({person: 42}));
});

it('uses form options and boolean fields while preserving their actual submitted values', async () => {
    const select = {
        ...generateElementWithDefaultValues(ElementType.Select),
        id: 'state',
        label: 'Bundesland',
        options: [{
            label: 'Berlin',
            value: 'BE',
        }],
    };
    const checkbox = {
        ...generateElementWithDefaultValues(ElementType.Checkbox),
        id: 'approved',
        label: 'Zustimmung',
    };
    const root = toNoCodeEditorNode({
        type: 'NoCodeExpression',
        operatorIdentifier: 'equals',
        operands: [
            {
                type: 'NoCodeReference',
                elementId: 'state',
            }, {
                type: 'NoCodeReference',
                elementId: 'approved',
            },
        ],
    });
    render(<NoCodeDryRunPanel root={root}
                              allElements={[{
                                  element: select,
                                  parents: [],
                              }, {
                                  element: checkbox,
                                  parents: [],
                              }]}
                              onResults={vi.fn()}/>);
    fireEvent.click(screen.getByRole('button', {name: 'Testlauf'}));
    fireEvent.mouseDown(screen.getByRole('combobox', {name: 'Testwert für „Bundesland“'}));
    fireEvent.click(await screen.findByRole('option', {name: /Berlin/}));
    await choose('Testwert für „Zustimmung“', 'Falsch');
    fireEvent.click(screen.getByRole('button', {name: 'Test ausführen'}));
    await waitFor(() => expect(api.post.mock.lastCall![1].elementData.effectiveValues).toEqual({
        state: 'BE',
        approved: false,
    }));
});

it('does not execute or lose unfinished field input on an attempted switch to JSON', async () => {
    setup({
        type: 'NoCodeProcessDataReference',
        path: 'amount',
    }, false);
    await choose('Datentyp für „$.amount“', 'Zahlenwert');
    const input = screen.getByRole('textbox', {name: 'Testwert für „$.amount“'});
    fireEvent.change(input, {target: {value: '-'}});
    expect(input).not.toHaveAttribute('aria-invalid', 'true');
    expect(within(screen.getByRole('group', {name: 'Eingabeformat für Vorgangsdaten'})).getByRole('button', {name: 'JSON'})).toBeDisabled();
    fireEvent.click(screen.getByRole('button', {name: 'Test ausführen'}));
    expect(api.post).not.toHaveBeenCalled();
    expect(input).toHaveValue('-');
    expect(input).toHaveAttribute('aria-invalid', 'true');
    fireEvent.change(input, {target: {value: '-12'}});
    await choose('Eingabeformat für Vorgangsdaten', 'JSON (Expertenmodus)');
    expect(JSON.parse((screen.getByRole('textbox', {name: /^Vorgangsdaten \(JSON\)/}) as HTMLInputElement).value)).toEqual({amount: -12});
});

it('preserves the order and unknown entries of a multi-selection imported from JSON', async () => {
    const select = {
        ...generateElementWithDefaultValues(ElementType.MultiCheckbox),
        id: 'choices',
        label: 'Auswahl',
        options: [
            {
                label: 'A',
                value: 'a',
            }, {
                label: 'B',
                value: 'b',
            },
        ],
    };
    const root = toNoCodeEditorNode({
        type: 'NoCodeReference',
        elementId: 'choices',
    });
    render(<NoCodeDryRunPanel root={root}
                              allElements={[{
                                  element: select,
                                  parents: [],
                              }]}
                              onResults={vi.fn()}/>);
    fireEvent.click(screen.getByRole('button', {name: 'Testlauf'}));
    await choose('Eingabeformat für „Auswahl“', 'JSON');
    fireEvent.change(screen.getByRole('textbox', {name: 'Testwert für „Auswahl“'}), {target: {value: '["other","a"]'}});
    await choose('Eingabeformat für „Auswahl“', 'Wert');
    expect(screen.getByRole('checkbox', {name: 'other'})).toBeChecked();
    expect(screen.getByRole('checkbox', {name: 'A'})).toBeChecked();
    fireEvent.click(screen.getByRole('checkbox', {name: 'B'}));
    fireEvent.click(screen.getByRole('button', {name: 'Test ausführen'}));
    await waitFor(() => expect(api.post.mock.lastCall![1].elementData.effectiveValues).toEqual({choices: ['other', 'a', 'b']}));
    await waitFor(() => expect(screen.getByRole('button', {name: 'Test ausführen'})).not.toBeDisabled());
    for (const checkbox of screen.getAllByRole('checkbox')) fireEvent.click(checkbox);
    fireEvent.click(screen.getByRole('button', {name: 'Test ausführen'}));
    await waitFor(() => expect(api.post.mock.lastCall![1].elementData.effectiveValues).toEqual({choices: []}));
});

it('keeps a safe route to JSON when a changed expression refers inside a primitive parent', async () => {
    const {
        rerender,
        onResults,
    } = setup({
        type: 'NoCodeProcessDataReference',
        path: 'person',
    }, false);
    await choose('Eingabeformat für Vorgangsdaten', 'JSON (Expertenmodus)');
    fireEvent.change(screen.getByRole('textbox', {name: /^Vorgangsdaten \(JSON\)/}), {target: {value: '{"person":42}'}});
    await choose('Eingabeformat für Vorgangsdaten', 'Eingabefelder');
    rerender(<NoCodeDryRunPanel root={toNoCodeEditorNode({
        type: 'NoCodeProcessDataReference',
        path: 'person.name',
    })}
                                allElements={elements}
                                onResults={onResults}/>);
    expect(screen.getByRole('textbox', {name: 'Testwert für „$.person.name“'})).toBeDisabled();
    await choose('Eingabeformat für Vorgangsdaten', 'JSON (Expertenmodus)');
    expect(JSON.parse((screen.getByRole('textbox', {name: /^Vorgangsdaten \(JSON\)/}) as HTMLInputElement).value)).toEqual({person: 42});
});

it('groups labels and references, exposes value options by keyboard and mounts JSON only when selected', async () => {
    const user = userEvent.setup();
    const root = toNoCodeEditorNode({
        type: 'NoCodeProcessDataReference',
        path: 'person.firstName',
    });
    render(<NoCodeDryRunPanel root={root}
                              allElements={elements}
                              onResults={vi.fn()}
                              presentation={{
                                  variables: [
                                      {
                                          source: InputVariableSource.ProcessData,
                                          path: 'person.firstName',
                                          label: 'Vorname',
                                      },
                                  ],
                              }}/>);
    await user.click(screen.getByRole('button', {name: 'Testlauf'}));
    expect(screen.getByRole('heading', {
        name: 'Vorgangsdaten für den Test',
        level: 4,
    })).toBeInTheDocument();
    expect(screen.getByText('Vorname')).toBeInTheDocument();
    expect(screen.getByText('$.person.firstName')).toBeInTheDocument();
    expect(screen.getByRole('textbox', {name: 'Testwert für „Vorname“'})).toHaveAccessibleDescription('$.person.firstName');
    expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', {name: 'JSON formatieren'})).not.toBeInTheDocument();
    const options = screen.getByRole('button', {name: 'Wertoptionen für „Vorname“'});
    options.focus();
    await user.keyboard('{Enter}');
    expect(screen.getByRole('menuitemradio', {name: 'Wert eingeben'})).toHaveAttribute('aria-checked', 'true');
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('menu')).not.toBeInTheDocument());
    expect(options).toHaveFocus();
    await user.click(options);
    await user.click(screen.getByRole('menuitemradio', {name: 'Leerer Text'}));
    await waitFor(() => expect(screen.queryByRole('menu')).not.toBeInTheDocument());
    expect(screen.getByText('Es wird ein Text ohne Zeichen bereitgestellt.')).toBeVisible();
    const toggle = screen.getByRole('group', {name: 'Eingabeformat für Vorgangsdaten'});
    expect(within(toggle).getByRole('button', {name: 'Eingabefelder'})).toHaveAttribute('aria-pressed', 'true');
    await user.click(within(toggle).getByRole('button', {name: 'JSON'}));
    expect(within(toggle).getByRole('button', {name: 'JSON'})).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', {name: 'JSON formatieren'})).toBeInTheDocument();
    expect(JSON.parse((screen.getByRole('textbox', {name: 'Vorgangsdaten (JSON)'}) as HTMLInputElement).value)).toEqual({person: {firstName: ''}});
    expect(api.post).not.toHaveBeenCalled();
});

it('persists examples only by explicit choice, restores edits and removes them without losing the local draft', async () => {
    const onSave = vi.fn();

    function SavedHarness({initial}: { initial?: NoCodeTestData }) {
        const [saved, setSaved] = useState(initial);
        const root = toNoCodeEditorNode({
            type: 'NoCodeReference',
            elementId: 'amount',
            editorMetadata: {testData: saved},
        });
        return <NoCodeDryRunPanel root={root}
                                  allElements={elements}
                                  onResults={vi.fn()}
                                  onTestDataChange={data => {
                                      setSaved(data);
                                      onSave(data);
                                  }}/>;
    }

    const view = render(<SavedHarness/>);
    fireEvent.click(screen.getByRole('button', {name: 'Testlauf'}));
    const input = screen.getByRole('textbox', {name: /^Testwert für „Betrag“/});
    fireEvent.change(input, {target: {value: '12'}});
    expect(onSave).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole('checkbox', {name: 'Testdaten mit dem Ausdruck speichern'}));
    expect(onSave.mock.lastCall![0].elementValues.amount.text).toBe('12');
    fireEvent.change(input, {target: {value: '-'}});
    const saved = onSave.mock.lastCall![0];
    expect(saved.elementValues.amount.text).toBe('-');
    expect(api.post).not.toHaveBeenCalled();
    view.unmount();
    render(<SavedHarness initial={saved}/>);
    fireEvent.click(screen.getByRole('button', {name: 'Testlauf'}));
    expect(screen.getByRole('textbox', {name: /^Testwert für „Betrag“/})).toHaveValue('-');
    expect(screen.getByRole('checkbox', {name: 'Testdaten mit dem Ausdruck speichern'})).toBeChecked();
    fireEvent.click(screen.getByRole('button', {name: 'Test ausführen'}));
    expect(api.post).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole('checkbox', {name: 'Testdaten mit dem Ausdruck speichern'}));
    expect(onSave).toHaveBeenLastCalledWith(undefined);
    expect(screen.getByRole('textbox', {name: /^Testwert für „Betrag“/})).toHaveValue('-');
});

it('restores special states and JSON examples, excludes authoring data from requests and keeps results fresh on note edits', async () => {
    const testData: NoCodeTestData = {
        version: 1,
        visibility: {},
        sourceValues: {
            instanceData: {
                mode: 'json',
                json: '{"caseNumber":"TEST-42"}',
            },
        },
        elementValues: {
            name: {
                type: NoCodeDataType.String,
                mode: 'empty',
                text: '',
            },
        },
    };
    const operand: NoCodeOperand = {
        type: 'NoCodeExpression',
        operatorIdentifier: 'test',
        operands: [
            {
                type: 'NoCodeReference',
                elementId: 'name',
            }, {
                type: 'NoCodeInstanceDataReference',
                path: 'caseNumber',
            },
        ],
        editorMetadata: {
            notes: 'Interne Notiz',
            testData,
        },
    };
    const root = toNoCodeEditorNode(operand)!;
    const view = render(<NoCodeDryRunPanel root={root}
                                           allElements={elements}
                                           onResults={vi.fn()}/>);
    fireEvent.click(screen.getByRole('button', {name: 'Testlauf'}));
    expect(api.post).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole('button', {name: 'Test ausführen'}));
    await screen.findByRole('region', {name: 'Gesamtergebnis'});
    expect(api.post.mock.lastCall![1].elementData.effectiveValues).toEqual({name: ''});
    expect(api.post.mock.lastCall![1].instanceData).toEqual({caseNumber: 'TEST-42'});
    expect(JSON.stringify(api.post.mock.lastCall![1].expression)).not.toContain('editorMetadata');
    view.rerender(<NoCodeDryRunPanel root={{
        ...root,
        editorMetadata: {
            ...root.editorMetadata,
            notes: 'Neue Notiz',
        },
    }}
                                     allElements={elements}
                                     onResults={vi.fn()}/>);
    expect(screen.queryByText(/Nicht mehr aktuell/)).not.toBeInTheDocument();
    expect(api.post).toHaveBeenCalledOnce();
});

it('keeps completed results and entered values when closing and reopening the test area', async () => {
    const {onResults} = setup({
        type: 'NoCodeReference',
        elementId: 'amount',
    }, false);
    fireEvent.change(screen.getByRole('textbox', {name: /^Testwert für „Betrag“/}), {target: {value: '42'}});
    fireEvent.click(screen.getByRole('button', {name: 'Test ausführen'}));
    await screen.findByRole('region', {name: 'Gesamtergebnis'});
    const completed = onResults.mock.lastCall![0];
    expect(completed).toBeInstanceOf(Map);
    onResults.mockClear();
    fireEvent.click(screen.getByRole('button', {name: 'Testlauf'}));
    expect(onResults).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole('button', {name: 'Testlauf'}));
    expect(screen.getByRole('textbox', {name: /^Testwert für „Betrag“/})).toHaveValue('42');
    expect(screen.getByRole('region', {name: 'Gesamtergebnis'})).not.toHaveTextContent('Nicht mehr aktuell');
    expect(api.post).toHaveBeenCalledOnce();
    expect(onResults).not.toHaveBeenCalled();
});

it('invalidates retained results when the expression changes while the test area is closed', async () => {
    const {
        rerender,
        onResults,
    } = setup(literal);
    await screen.findByRole('region', {name: 'Gesamtergebnis'});
    fireEvent.click(screen.getByRole('button', {name: 'Testlauf'}));
    rerender(<NoCodeDryRunPanel root={toNoCodeEditorNode({
        type: 'NoCodeStaticValue',
        value: 'changed',
    })}
                                allElements={elements}
                                onResults={onResults}/>);
    expect(onResults).toHaveBeenLastCalledWith(null);
    fireEvent.click(screen.getByRole('button', {name: 'Testlauf'}));
    expect(screen.getByRole('region', {name: 'Gesamtergebnis'})).toHaveTextContent('Nicht mehr aktuell');
    expect(api.post).toHaveBeenCalledOnce();
});
