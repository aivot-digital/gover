import {type AuthoredInputValue, InputMode, InputVariableSource} from '../../../models/input-mode';
import {describe, expect, it, vi} from 'vitest';
import React from 'react';
import {act, fireEvent, render, screen, waitFor} from '@testing-library/react';
import {ElementType} from '../../../data/element-type/element-type';
import type {
    AuthoredElementValues,
    ComputedElementErrors,
    DerivedRuntimeElementData,
    ReplicatingContainerElementValues,
} from '../../../models/element-data';
import type {AnyElement} from '../../../models/elements/any-element';
import {
    ComputedElementValueSource,
    createDerivedRuntimeElementData,
    getLiteralElementValue,
    literalAuthoredValue,
} from '../../../models/element-data';
import {ElementDerivationContext} from './element-derivation-context';

const observeViewProps = vi.hoisted(() => vi.fn());

const dynamicChanges: Array<[AuthoredInputValue<unknown>, AuthoredInputValue<unknown>, unknown]> = [
    [
        {type: InputMode.Variable, reference: {source: InputVariableSource.ProcessData, path: 'amount'}},
        {type: InputMode.Variable, reference: {source: InputVariableSource.ProcessData, path: 'otherAmount'}},
        0,
    ],
    [
        {type: InputMode.LowCode, code: 'return 120;'},
        {type: InputMode.LowCode, code: 'return false;'},
        false,
    ],
    [
        {type: InputMode.NoCode, operand: {type: 'NoCodeStaticValue', value: '120'}},
        {type: InputMode.NoCode, operand: {type: 'NoCodeStaticValue', value: '240'}},
        null,
    ],
];

vi.mock('../../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => vi.fn(),
}));

vi.mock('../../../components/view-dispatcher/view-dispatcher.component', () => ({
    ViewDispatcherComponent: (props: any) => {
        observeViewProps(props);
        return (
        <>
            <button
                type="button"
                onClick={() => props.onAuthoredElementValuesChange({field: literalAuthoredValue('valid')}, ['field'])}
            >
                Wert setzen
            </button>
            <button
                type="button"
                onClick={() => props.onAuthoredElementValuesChange({
                    field: {
                        type: InputMode.Variable,
                        reference: {source: InputVariableSource.ProcessData, path: 'person.name'},
                    },
                }, ['field'])}
            >
                Dynamischen Wert setzen
            </button>
            <button
                type="button"
                onClick={() => props.onDerive(props.authoredElementValues, [], [])}
            >
                Validieren
            </button>
            <button
                type="button"
                onClick={() => {
                    const currentRows = getLiteralElementValue<unknown[]>(props.authoredElementValues, 'rows') ?? [];
                    props.onAuthoredElementValuesChange({
                        ...props.authoredElementValues,
                        rows: literalAuthoredValue([
                            ...currentRows,
                            {
                                id: `row-${currentRows.length + 1}`,
                                values: {},
                            },
                        ]),
                    }, ['rows', 'rowField']);
                }}
            >
                Datensatz hinzufügen
            </button>
            <button
                type="button"
                onClick={() => {
                    const currentRows = getLiteralElementValue<any[]>(props.authoredElementValues, 'rows') ?? [];
                    props.onAuthoredElementValuesChange({
                        ...props.authoredElementValues,
                        rows: literalAuthoredValue(currentRows.map((row: any, index: number) => index === 0 ? {
                            ...row,
                            values: {
                                ...(row.values ?? {}),
                                rowField: literalAuthoredValue('valid'),
                            },
                        } : row)),
                    }, ['rows', 'rowField']);
                }}
            >
                Ersten Datensatz ändern
            </button>
            <output data-testid="field-error">
                {props.derivedData.elementStates.field?.error ?? ''}
            </output>
            <output data-testid="field-effective-value">
                {JSON.stringify(props.derivedData.effectiveValues.field) ?? ''}
            </output>
            <output data-testid="row-1-error">
                {
                    props.derivedData.elementStates.rows?.subStates
                        ?.find((subState: any) => subState.id === 'row-1')
                        ?.states?.rowField?.error ?? ''
                }
            </output>
            <output data-testid="row-2-error">
                {
                    props.derivedData.elementStates.rows?.subStates
                        ?.find((subState: any) => subState.id === 'row-2')
                        ?.states?.rowField?.error ?? ''
                }
            </output>
            <output data-testid="row-2-visible">
                {
                    props.derivedData.elementStates.rows?.subStates
                        ?.find((subState: any) => subState.id === 'row-2')
                        ?.states?.rowField?.visible == null ?
                        '' :
                        String(props.derivedData.elementStates.rows?.subStates
                            ?.find((subState: any) => subState.id === 'row-2')
                            ?.states?.rowField?.visible)
                }
            </output>
            <output data-testid="row-2-disabled">
                {
                    props.derivedData.elementStates.rows?.subStates
                        ?.find((subState: any) => subState.id === 'row-2')
                        ?.states?.rowField?.disabled == null ?
                        '' :
                        String(props.derivedData.elementStates.rows?.subStates
                            ?.find((subState: any) => subState.id === 'row-2')
                            ?.states?.rowField?.disabled)
                }
            </output>
            <output data-testid="row-2-value-source">
                {
                    props.derivedData.elementStates.rows?.subStates
                        ?.find((subState: any) => subState.id === 'row-2')
                        ?.states?.rowField?.valueSource ?? ''
                }
            </output>
        </>
        );
    },
}));

describe('ElementDerivationContext', () => {
    it.each(dynamicChanges)('preserves an unchanged %j result when another field changes', async (value) => {
        const initial = {field: value, comment: literalAuthoredValue('Before')};
        const previous = createDerivedRuntimeElementData({
            effectiveValues: {field: 120, comment: 'Before'},
            elementStates: {field: {valueSource: ComputedElementValueSource.Authored}},
        });
        const harness = await setupOptimisticEdit(initial, previous);

        harness.edit({...structuredClone(initial), comment: literalAuthoredValue('After')}, ['comment']);

        expect(harness.latest().effectiveValues).toEqual({field: 120, comment: 'After'});
        expect(harness.latest().elementStates.field).toEqual(previous.elementStates.field);
        expect(harness.derive).toHaveBeenCalledTimes(1);
    });

    it.each(dynamicChanges)('invalidates an edited %j and derives without referencedIds', async (value, changed, result) => {
        const previous = createDerivedRuntimeElementData({effectiveValues: {field: 120}});
        const harness = await setupOptimisticEdit({field: value}, previous);
        const updated = {field: changed};

        harness.edit(updated, ['field']);

        expect(harness.latest().effectiveValues.field).toBeNull();
        expect(previous.effectiveValues.field).toBe(120);
        expect(harness.derive).toHaveBeenLastCalledWith(updated, ['ALL']);
        expect(harness.derive).toHaveBeenCalledTimes(2);
        await harness.finish(createDerivedRuntimeElementData({effectiveValues: {field: result}}));
        // Null is a legitimate backend result, including deliberately deferred authoring evaluation.
        expect(harness.latest().effectiveValues.field).toBe(result);
    });

    it('retains the values and child states of an unchanged dynamic container', async () => {
        const container = {type: InputMode.LowCode, code: 'return $.rows;'} as const;
        const previous = createDerivedRuntimeElementData({
            effectiveValues: {field: [{id: 'row-1', values: {name: 'Ada'}}]},
            elementStates: {field: {subStates: [{id: 'row-1', states: {name: {visible: true}}}]}},
        });
        const element = {id: 'root', type: ElementType.GroupLayout, children: [
            {id: 'field', type: ElementType.ReplicatingContainer, children: [{id: 'name', type: ElementType.Text}]},
            {id: 'comment', type: ElementType.Text},
        ]} as AnyElement;
        const harness = await setupOptimisticEdit({field: container}, previous, element);

        harness.edit({field: {...container}, comment: literalAuthoredValue('After')}, ['comment']);

        expect(harness.latest().effectiveValues.field).toEqual(previous.effectiveValues.field);
        expect(harness.latest().elementStates.field).toEqual(previous.elementStates.field);
        expect(harness.derive).toHaveBeenCalledTimes(1);
    });

    it('matches unchanged nested expressions by row ID and derives edits in a reordered row', async () => {
        const first = {type: InputMode.LowCode, code: 'return 1;'} as const;
        const second = {type: InputMode.LowCode, code: 'return 2;'} as const;
        const rows = [
            {id: 'row-1', values: {field: first}},
            {id: 'row-2', values: {field: second}},
        ];
        const previous = createDerivedRuntimeElementData({effectiveValues: {rows: [
            {id: 'row-1', values: {field: 1}},
            {id: 'row-2', values: {field: 2}},
        ]}});
        const element = {id: 'rows', type: ElementType.ReplicatingContainer,
            children: [{id: 'field', type: ElementType.Text}]} as AnyElement;
        const harness = await setupOptimisticEdit({rows: literalAuthoredValue(rows)}, previous, element);

        harness.edit({rows: literalAuthoredValue([
            structuredClone(rows[1]),
            {id: 'row-1', values: {field: {type: InputMode.LowCode, code: 'return 3;'}}},
        ])}, ['rows', 'field']);

        expect(harness.latest().effectiveValues.rows).toEqual([
            {id: 'row-2', values: {field: 2}},
            {id: 'row-1', values: {field: null}},
        ]);
        expect(harness.derive).toHaveBeenCalledTimes(2);
        await harness.finish(createDerivedRuntimeElementData({effectiveValues: {rows: [
            {id: 'row-2', values: {field: 2}},
            {id: 'row-1', values: {field: 3}},
        ]}}));
        expect(harness.latest().effectiveValues.rows[1].values.field).toBe(3);
    });

    it('derives a switch back to literal without clearing the new literal value', async () => {
        const harness = await setupOptimisticEdit(
            {field: {type: InputMode.LowCode, code: 'return 120;'}},
            createDerivedRuntimeElementData({effectiveValues: {field: 120}}),
        );

        harness.edit({field: literalAuthoredValue(7)}, ['field']);

        expect(harness.latest().effectiveValues.field).toBe(7);
        expect(harness.derive).toHaveBeenCalledTimes(2);
        await harness.finish(createDerivedRuntimeElementData({effectiveValues: {field: 7}}));
        expect(harness.latest().effectiveValues.field).toBe(7);
    });

    it('projects nested container children without unwrapping ordinary object payloads', async () => {
        const objectPayload = {type: 'Literal', value: 'Business data'};
        const children = [{
            id: 'group', type: ElementType.GroupLayout,
            children: [
                {id: 'name', type: ElementType.Text},
                {id: 'object', type: ElementType.Text},
                {
                    id: 'nested', type: ElementType.ReplicatingContainer,
                    children: [{id: 'note', type: ElementType.Text}],
                },
            ],
        }] as AnyElement[];
        const rows = [{id: 'row-1', values: {
            name: literalAuthoredValue('Ada'),
            object: literalAuthoredValue(objectPayload),
            nested: literalAuthoredValue([{id: 'nested-1', values: {note: literalAuthoredValue('Note')}}]),
        }}];

        const result = await projectContainerRows(rows, children);

        expect(result.effectiveValues.rows).toEqual([{id: 'row-1', values: {
            name: 'Ada',
            object: objectPayload,
            nested: [{id: 'nested-1', values: {note: 'Note'}}],
        }}]);
        expect(result.elementStates.rows?.subStates?.[0].states?.nested?.subStates?.[0]).toEqual({
            id: 'nested-1', states: {note: {valueSource: ComputedElementValueSource.Authored}},
        });
    });

    it.each([
        ['reordering', ['row-2', 'row-1'], ['Second', 'First']],
        ['deleting', ['row-2'], ['Second']],
        ['adding', ['row-3', 'row-1'], [undefined, 'First']],
    ] as const)('preserves derived values and states by row ID when %s rows', async (_, ids, names) => {
        const children = [
            {id: 'name', type: ElementType.Text},
            {id: 'locked', type: ElementType.Text, disabled: true},
            {id: 'identity', type: ElementType.Text},
        ] as AnyElement[];
        const previous = createDerivedRuntimeElementData({
            effectiveValues: {rows: [
                {id: 'row-1', values: {locked: 'First', identity: 'Verified first'}},
                {id: 'row-2', values: {locked: 'Second', identity: 'Verified second'}},
            ]},
            elementStates: {rows: {subStates: [
                {id: 'row-1', states: {
                    locked: {valueSource: ComputedElementValueSource.Derived, error: 'First error'},
                    identity: {valueSource: ComputedElementValueSource.Identity},
                }},
                {id: 'row-2', states: {
                    locked: {valueSource: ComputedElementValueSource.Derived, error: 'Second error'},
                    identity: {valueSource: ComputedElementValueSource.Identity},
                }},
            ]}},
        });
        const rows = ids.map((id) => ({id, values: {
            name: literalAuthoredValue(id),
            locked: literalAuthoredValue('Must not override derived data'),
        }}));

        const result = await projectContainerRows(rows, children, previous);

        expect(result.effectiveValues.rows.map((row: any) => row.id)).toEqual(ids);
        expect(result.effectiveValues.rows.map((row: any) => row.values.locked)).toEqual(names);
        expect(result.effectiveValues.rows.map((row: any) => row.values.name)).toEqual(ids);
        expect(result.effectiveValues.rows.map((row: any) => row.values.identity)).toEqual(
            names.map((name) => name == null ? undefined : `Verified ${name.toLowerCase()}`),
        );
        expect(result.elementStates.rows?.subStates?.map((state) => state.states?.locked?.error)).toEqual(
            names.map((name) => name == null ? undefined : `${name} error`),
        );
    });

    it('keeps identity, technical and computed-disabled child values authoritative', async () => {
        const children = [
            {id: 'identity', type: ElementType.Text},
            {id: 'technical', type: ElementType.Text, technical: true},
            {id: 'disabled', type: ElementType.Text},
        ] as AnyElement[];
        const previous = createDerivedRuntimeElementData({
            effectiveValues: {rows: [{id: 'row-1', values: {
                identity: 'Verified', technical: 'Calculated', disabled: 'Locked',
            }}]},
            elementStates: {rows: {subStates: [{id: 'row-1', states: {
                identity: {valueSource: ComputedElementValueSource.Identity},
                disabled: {disabled: true},
            }}]}},
        });
        const result = await projectContainerRows([{id: 'row-1', values: {
            identity: literalAuthoredValue('Authored'),
            technical: literalAuthoredValue('Authored'),
            disabled: literalAuthoredValue('Authored'),
        }}], children, previous);

        expect(result.effectiveValues).toEqual(previous.effectiveValues);
        expect(result.elementStates.rows?.subStates).toEqual(previous.elementStates.rows?.subStates);
    });

    it.each([null, []])('preserves the empty container value %j', async (rows) => {
        const result = await projectContainerRows(rows, []);
        expect(result.effectiveValues.rows).toEqual(rows);
        expect(result.elementStates.rows?.subStates).toEqual(rows);
    });

    it('should not persist external computed errors when authored values change', async () => {
        const onAuthoredElementValuesChange = vi.fn();
        const onDerivedDataChange = vi.fn();
        const computedErrors: ComputedElementErrors = {
            field: {
                error: 'Der Verantwortliche Personenkreis ist ein Pflichtfeld.',
            },
        };

        render(
            <ElementDerivationContext
                element={createRootElement()}
                authoredElementValues={{field: literalAuthoredValue(null)}}
                onAuthoredElementValuesChange={onAuthoredElementValuesChange}
                onDerivedDataChange={onDerivedDataChange}
                computedErrors={computedErrors}
                onDeriveOverride={() => Promise.resolve(createDerivedRuntimeElementData())}
            />,
        );

        await waitFor(() => expect(onDerivedDataChange).toHaveBeenCalled());
        expect(screen.getByTestId('field-error')).toHaveTextContent(computedErrors.field?.error as string);
        onDerivedDataChange.mockClear();

        fireEvent.click(screen.getByRole('button', {name: 'Wert setzen'}));

        expect(onAuthoredElementValuesChange).toHaveBeenCalledWith({field: literalAuthoredValue('valid')});
        const patchedDerivedData = onDerivedDataChange.mock.calls[0][0] as DerivedRuntimeElementData;
        expect(patchedDerivedData.elementStates.field?.error).toBeUndefined();
        await waitFor(() => expect(screen.getByTestId('field-error')).toBeEmptyDOMElement());
    });

    it('should not project authored input-mode wrappers into optimistic effective values', async () => {
        const onAuthoredElementValuesChange = vi.fn();
        const onDerivedDataChange = vi.fn();
        const rootElement = createRootElement();
        rootElement.children[0].inputModePolicy = {
            allowedModes: [InputMode.Literal, InputMode.Variable],
            allowedVariableSources: [InputVariableSource.ProcessData],
        };

        render(
            <ElementDerivationContext
                element={rootElement}
                authoredElementValues={{field: literalAuthoredValue(null)}}
                onAuthoredElementValuesChange={onAuthoredElementValuesChange}
                onDerivedDataChange={onDerivedDataChange}
                onDeriveOverride={() => Promise.resolve(createDerivedRuntimeElementData())}
                inputModesEnabled
            />,
        );

        await waitFor(() => expect(onDerivedDataChange).toHaveBeenCalled());
        fireEvent.click(screen.getByRole('button', {name: 'Dynamischen Wert setzen'}));

        expect(onAuthoredElementValuesChange).toHaveBeenCalledWith({
            field: {
                type: InputMode.Variable,
                reference: {source: InputVariableSource.ProcessData, path: 'person.name'},
            },
        });
        expect(screen.getByTestId('field-effective-value')).toHaveTextContent('null');
    });

    it('should retain newly derived row states when external errors only contain older rows', async () => {
        const onDerivedDataChange = vi.fn();
        const computedErrors: ComputedElementErrors = {
            rows: {
                error: 'Container error',
                subStates: [
                    {
                        id: 'row-1',
                        states: {
                            rowField: {
                                error: 'External row error',
                            },
                        },
                    },
                ],
            },
        };
        const onDeriveOverride = vi.fn((authoredElementValues: AuthoredElementValues) => {
            const rows = getLiteralElementValue<Array<{id?: string | null}>>(authoredElementValues, 'rows') ?? [];

            return Promise.resolve(createDerivedRuntimeElementData({
                effectiveValues: {},
                elementStates: {
                    rows: {
                        subStates: rows.map((row: {id?: string | null}) => ({
                            id: row.id,
                            states: {
                                rowField: {
                                    visible: row.id !== 'row-2',
                                    disabled: row.id === 'row-2',
                                    valueSource: ComputedElementValueSource.Derived,
                                    error: null,
                                },
                            },
                        })),
                    },
                    dependent: {
                        visible: true,
                    },
                },
            }));
        });

        render(
            <ReplicatingContainerDerivationHarness
                onDerivedDataChange={onDerivedDataChange}
                onDeriveOverride={onDeriveOverride}
                computedErrors={computedErrors}
            />
        );

        await waitFor(() => expect(onDeriveOverride).toHaveBeenCalledTimes(1));
        expect(screen.getByTestId('row-1-error')).toHaveTextContent('External row error');

        fireEvent.click(screen.getByRole('button', {name: 'Datensatz hinzufügen'}));

        await waitFor(() => expect(onDeriveOverride).toHaveBeenCalledTimes(2));
        expect(screen.getByTestId('row-2-visible')).toHaveTextContent('false');
        expect(screen.getByTestId('row-2-disabled')).toHaveTextContent('true');
        expect(screen.getByTestId('row-2-value-source')).toHaveTextContent(ComputedElementValueSource.Derived);
        expect(screen.getByTestId('row-1-error')).toHaveTextContent('External row error');
    });

    it('should preserve sibling row errors while suppressing only the changed row until explicit revalidation', async () => {
        const onDerivedDataChange = vi.fn();
        const validationError = 'Dieses Feld ist ein Pflichtfeld und darf nicht leer sein.';
        let shouldReturnValidationErrors = true;
        const onDeriveOverride = vi.fn((authoredElementValues: AuthoredElementValues, skipErrorsForElements: string[]) => {
            const rows = getLiteralElementValue<Array<{id?: string | null}>>(authoredElementValues, 'rows') ?? [];
            const shouldIncludeErrors = !skipErrorsForElements.includes('ALL') && shouldReturnValidationErrors;

            return Promise.resolve(createDerivedRuntimeElementData({
                effectiveValues: {},
                elementStates: {
                    field: {
                        error: shouldIncludeErrors ? validationError : null,
                    },
                    rows: {
                        subStates: rows.map((row: {id?: string | null}) => ({
                            id: row.id,
                            states: {
                                rowField: {
                                    error: shouldIncludeErrors ? `Fehler in ${row.id}` : null,
                                },
                            },
                        })),
                    },
                    dependent: {
                        visible: true,
                    },
                },
            }));
        });

        render(
            <ReplicatingContainerDerivationHarness
                onDerivedDataChange={onDerivedDataChange}
                onDeriveOverride={onDeriveOverride}
            />
        );

        await waitFor(() => expect(onDeriveOverride).toHaveBeenCalledTimes(1));

        fireEvent.click(screen.getByRole('button', {name: 'Validieren'}));
        await waitFor(() => expect(screen.getByTestId('field-error')).toHaveTextContent(validationError));
        expect(screen.getByTestId('row-1-error')).toHaveTextContent('Fehler in row-1');

        fireEvent.click(screen.getByRole('button', {name: 'Datensatz hinzufügen'}));
        await waitFor(() => expect(onDeriveOverride).toHaveBeenCalledTimes(3));
        expect(onDeriveOverride).toHaveBeenLastCalledWith(
            expect.objectContaining({
                rows: literalAuthoredValue([
                    {
                        id: 'row-1',
                        values: {
                            rowField: literalAuthoredValue(null),
                        },
                    },
                    {
                        id: 'row-2',
                        values: {},
                    },
                ]),
            }),
            ['ALL'],
        );
        expect(screen.getByTestId('field-error')).toHaveTextContent(validationError);
        expect(screen.getByTestId('row-1-error')).toHaveTextContent('Fehler in row-1');
        expect(screen.getByTestId('row-2-error')).toBeEmptyDOMElement();

        fireEvent.click(screen.getByRole('button', {name: 'Validieren'}));
        await waitFor(() => expect(onDeriveOverride).toHaveBeenCalledTimes(4));
        expect(screen.getByTestId('row-1-error')).toHaveTextContent('Fehler in row-1');
        expect(screen.getByTestId('row-2-error')).toHaveTextContent('Fehler in row-2');

        fireEvent.click(screen.getByRole('button', {name: 'Ersten Datensatz ändern'}));
        await waitFor(() => expect(onDeriveOverride).toHaveBeenCalledTimes(5));
        expect(screen.getByTestId('row-1-error')).toBeEmptyDOMElement();
        expect(screen.getByTestId('row-2-error')).toHaveTextContent('Fehler in row-2');

        shouldReturnValidationErrors = false;
        fireEvent.click(screen.getByRole('button', {name: 'Validieren'}));
        await waitFor(() => expect(onDeriveOverride).toHaveBeenCalledTimes(6));
        expect(screen.getByTestId('field-error')).toBeEmptyDOMElement();
        expect(screen.getByTestId('row-1-error')).toBeEmptyDOMElement();
        expect(screen.getByTestId('row-2-error')).toBeEmptyDOMElement();
    });
});

async function projectContainerRows(
    rows: ReplicatingContainerElementValues | null,
    children: AnyElement[],
    previous = createDerivedRuntimeElementData(),
): Promise<DerivedRuntimeElementData> {
    const authored = {rows: literalAuthoredValue(rows)};
    const authoredSnapshot = structuredClone(authored);
    const previousSnapshot = structuredClone(previous);
    const onDerivedDataChange = vi.fn();
    render(<ElementDerivationContext
        element={{id: 'rows', type: ElementType.ReplicatingContainer, children} as AnyElement}
        authoredElementValues={{}}
        onAuthoredElementValuesChange={vi.fn()}
        onDerivedDataChange={onDerivedDataChange}
        onDeriveOverride={() => Promise.resolve(previous)}
    />);
    await waitFor(() => expect(onDerivedDataChange).toHaveBeenCalled());
    onDerivedDataChange.mockClear();

    await act(async () => {
        await observeViewProps.mock.lastCall![0].onAuthoredElementValuesChange(authored, ['rows']);
    });

    expect(onDerivedDataChange).toHaveBeenCalledTimes(1);
    expect(authored).toEqual(authoredSnapshot);
    expect(previous).toEqual(previousSnapshot);
    return onDerivedDataChange.mock.lastCall![0];
}

async function setupOptimisticEdit(
    initial: AuthoredElementValues,
    previous: DerivedRuntimeElementData,
    element = {id: 'root', type: ElementType.GroupLayout, children: [
        {id: 'field', type: ElementType.Text},
        {id: 'comment', type: ElementType.Text},
    ]} as AnyElement,
) {
    let resolveDerivation!: (result: DerivedRuntimeElementData) => void;
    const pending = new Promise<DerivedRuntimeElementData>((resolve) => { resolveDerivation = resolve; });
    const derive = vi.fn().mockResolvedValueOnce(previous).mockReturnValue(pending);
    const onDerivedDataChange = vi.fn();
    render(<ElementDerivationContext
        element={element}
        authoredElementValues={initial}
        onAuthoredElementValuesChange={vi.fn()}
        onDerivedDataChange={onDerivedDataChange}
        onDeriveOverride={derive}
        inputModesEnabled
    />);
    await waitFor(() => expect(onDerivedDataChange).toHaveBeenCalled());
    let editPromise: Promise<void>;
    return {
        derive,
        latest: () => onDerivedDataChange.mock.lastCall![0] as DerivedRuntimeElementData,
        edit: (values: AuthoredElementValues, ids: string[]) => {
            act(() => { editPromise = observeViewProps.mock.lastCall![0].onAuthoredElementValuesChange(values, ids); });
        },
        finish: async (result: DerivedRuntimeElementData) => {
            await act(async () => {
                resolveDerivation(result);
                await editPromise;
            });
        },
    };
}

interface ReplicatingContainerDerivationHarnessProps {
    onDerivedDataChange: (derivedData: DerivedRuntimeElementData) => void;
    onDeriveOverride: (authoredElementValues: AuthoredElementValues, skipErrorsForElements: string[]) => Promise<DerivedRuntimeElementData>;
    computedErrors?: ComputedElementErrors;
}

function ReplicatingContainerDerivationHarness(props: ReplicatingContainerDerivationHarnessProps) {
    const {
        onDerivedDataChange,
        onDeriveOverride,
        computedErrors,
    } = props;
    const element = React.useMemo(() => createRootElementWithReplicatingContainer(), []);
    const [authoredElementValues, setAuthoredElementValues] = React.useState<AuthoredElementValues>({
        field: literalAuthoredValue(null),
        rows: literalAuthoredValue([
            {
                id: 'row-1',
                values: {
                    rowField: literalAuthoredValue(null),
                },
            },
        ]),
    });

    return (
        <ElementDerivationContext
            element={element}
            authoredElementValues={authoredElementValues}
            onAuthoredElementValuesChange={setAuthoredElementValues}
            onDerivedDataChange={onDerivedDataChange}
            onDeriveOverride={onDeriveOverride}
            computedErrors={computedErrors}
        />
    );
}

function createRootElement(): any {
    return {
        id: 'root',
        type: ElementType.GroupLayout,
        children: [
            {
                id: 'field',
                type: ElementType.Text,
                disabled: false,
                technical: false,
            },
        ],
    };
}

function createRootElementWithReplicatingContainer(): any {
    return {
        id: 'root',
        type: ElementType.GroupLayout,
        children: [
            {
                id: 'field',
                type: ElementType.Text,
                disabled: false,
                technical: false,
            },
            {
                id: 'rows',
                type: ElementType.ReplicatingContainer,
                disabled: false,
                technical: false,
                children: [
                    {
                        id: 'rowField',
                        type: ElementType.Text,
                        disabled: false,
                        technical: false,
                    },
                ],
            },
            {
                id: 'dependent',
                type: ElementType.Text,
                visibility: {
                    type: 'NoCode',
                    referencedIds: ['rows'],
                },
            },
        ],
    };
}
