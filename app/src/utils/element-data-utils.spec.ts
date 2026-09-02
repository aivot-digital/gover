import {describe, expect, it} from 'vitest';
import {ElementType} from '../data/element-type/element-type';
import {ComputedElementValueSource, createDerivedRuntimeElementData} from '../models/element-data';
import {
    applyElementErrorSuppressions,
    collectChangedElementErrorSuppressionTargets,
    mapAuthoredElementValues,
    mergeElementErrorSuppressionTargets,
    normalizeReplicatingContainerValues,
    preserveDerivedErrors,
    resolveReplicatingContainerItemDerivedData,
    resolveVisibility,
    resolveValueForResolvedOverride,
    walkAuthoredElementValues,
} from './element-data-utils';
import {AnyElement} from '../models/elements/any-element';

describe('resolveValueForResolvedOverride', () => {
    const field = {
        id: 'field',
        type: ElementType.Text,
    } as AnyElement;

    it('should prefer explicit authored values over stale derived values', () => {
        const derivedData = createDerivedRuntimeElementData({
            effectiveValues: {
                field: 'derived',
            },
            elementStates: {
                field: {
                    valueSource: ComputedElementValueSource.Derived,
                },
            },
        });

        expect(resolveValueForResolvedOverride(field, {field: 'authored'}, derivedData)).toBe('authored');
        expect(resolveValueForResolvedOverride(field, {field: null}, derivedData)).toBeNull();
    });

    it('should use derived values while no authored override exists', () => {
        const derivedData = createDerivedRuntimeElementData({
            effectiveValues: {
                field: 'derived',
            },
            elementStates: {
                field: {
                    valueSource: ComputedElementValueSource.Derived,
                },
            },
        });

        expect(resolveValueForResolvedOverride(field, {}, derivedData)).toBe('derived');
    });

    it('should keep identity values authoritative', () => {
        const derivedData = createDerivedRuntimeElementData({
            effectiveValues: {
                field: 'identity',
            },
            elementStates: {
                field: {
                    valueSource: ComputedElementValueSource.Identity,
                },
            },
        });

        expect(resolveValueForResolvedOverride(field, {field: 'authored'}, derivedData)).toBe('identity');
    });
});

describe('resolveVisibility', () => {
    const field = {
        id: 'field',
        type: ElementType.Text,
    } as AnyElement;
    const dynamicField = {
        id: 'dynamicField',
        type: ElementType.Text,
        visibility: {
            type: 'NoCode',
        },
    } as AnyElement;

    it('should keep static elements visible while derived state is missing', () => {
        expect(resolveVisibility(field, createDerivedRuntimeElementData())).toBe(true);
    });

    it('should hide dynamic visibility elements while derived state is missing', () => {
        expect(resolveVisibility(dynamicField, createDerivedRuntimeElementData())).toBe(false);
    });

    it('should use explicit derived visibility when available', () => {
        expect(resolveVisibility(dynamicField, createDerivedRuntimeElementData({
            elementStates: {
                dynamicField: {
                    visible: true,
                },
            },
        }))).toBe(true);
        expect(resolveVisibility(field, createDerivedRuntimeElementData({
            elementStates: {
                field: {
                    visible: false,
                },
            },
        }))).toBe(false);
    });
});

describe('preserveDerivedErrors', () => {
    it('should preserve previous validation errors while keeping fresh derived state authoritative', () => {
        const previousDerivedData = createDerivedRuntimeElementData({
            effectiveValues: {
                field: 'previous value',
            },
            elementStates: {
                field: {
                    visible: true,
                    error: 'Previous validation error',
                    errorDetails: {
                        label: 'Previous label',
                    },
                },
                runtimeFailure: {
                    error: 'Previous runtime error',
                },
            },
        });
        const nextDerivedData = createDerivedRuntimeElementData({
            effectiveValues: {
                field: 'next value',
            },
            elementStates: {
                field: {
                    visible: false,
                    error: null,
                    errorDetails: null,
                    valueSource: ComputedElementValueSource.Derived,
                },
                runtimeFailure: {
                    error: 'Fresh runtime error',
                    errorDetails: {
                        source: 'derivation',
                    },
                },
            },
        });

        expect(preserveDerivedErrors(previousDerivedData, nextDerivedData)).toEqual({
            effectiveValues: {
                field: 'next value',
            },
            elementStates: {
                field: {
                    visible: false,
                    error: 'Previous validation error',
                    errorDetails: {
                        label: 'Previous label',
                    },
                    valueSource: ComputedElementValueSource.Derived,
                },
                runtimeFailure: {
                    error: 'Fresh runtime error',
                    errorDetails: {
                        source: 'derivation',
                    },
                },
            },
        });
        expect(nextDerivedData.elementStates.field?.error).toBeNull();
        expect(previousDerivedData.effectiveValues.field).toBe('previous value');
    });

    it('should preserve replicated-row errors by row id without retaining removed rows or populating new rows', () => {
        const previousDerivedData = createDerivedRuntimeElementData({
            elementStates: {
                rows: {
                    subStates: [
                        {
                            id: 'row-1',
                            states: {
                                rowField: {
                                    error: 'Removed row error',
                                },
                            },
                        },
                        {
                            id: 'row-2',
                            states: {
                                rowField: {
                                    error: 'Retained row error',
                                    errorDetails: {
                                        row: 2,
                                    },
                                },
                            },
                        },
                    ],
                },
            },
        });
        const nextDerivedData = createDerivedRuntimeElementData({
            elementStates: {
                rows: {
                    subStates: [
                        {
                            id: 'row-2',
                            states: {
                                rowField: {
                                    visible: false,
                                    error: null,
                                },
                            },
                        },
                        {
                            id: 'row-3',
                            states: {
                                rowField: {
                                    error: null,
                                },
                            },
                        },
                    ],
                },
            },
        });

        expect(preserveDerivedErrors(previousDerivedData, nextDerivedData).elementStates.rows?.subStates).toEqual([
            {
                id: 'row-2',
                states: {
                    rowField: {
                        visible: false,
                        error: 'Retained row error',
                        errorDetails: {
                            row: 2,
                        },
                    },
                },
            },
            {
                id: 'row-3',
                states: {
                    rowField: {
                        error: null,
                    },
                },
            },
        ]);
    });

    it('should fall back to the row index for legacy sub-states without ids', () => {
        const previousDerivedData = createDerivedRuntimeElementData({
            elementStates: {
                rows: {
                    subStates: [
                        {
                            states: {
                                rowField: {
                                    error: 'Legacy row error',
                                },
                            },
                        },
                    ],
                },
            },
        });
        const nextDerivedData = createDerivedRuntimeElementData({
            elementStates: {
                rows: {
                    subStates: [
                        {
                            states: {
                                rowField: {
                                    error: null,
                                },
                            },
                        },
                    ],
                },
            },
        });

        expect(
            preserveDerivedErrors(previousDerivedData, nextDerivedData)
                .elementStates.rows?.subStates?.[0].states?.rowField?.error,
        ).toBe('Legacy row error');
    });
});

describe('replicating container error suppression', () => {
    const rowField = {
        id: 'rowField',
        type: ElementType.Text,
    } as AnyElement;
    const rows = {
        id: 'rows',
        type: ElementType.ReplicatingContainer,
        children: [rowField],
    } as AnyElement;
    const root = {
        id: 'root',
        type: ElementType.GroupLayout,
        children: [rows],
    } as AnyElement;

    it('should treat an appended row as a container change without suppressing existing row children', () => {
        const previousValues = {
            rows: [
                {
                    id: 'row-1',
                    values: {
                        rowField: 'existing',
                    },
                },
            ],
        };
        const nextValues = {
            rows: [
                ...previousValues.rows,
                {
                    id: 'row-2',
                    values: {},
                },
            ],
        };

        expect(collectChangedElementErrorSuppressionTargets(root, previousValues, nextValues)).toEqual([
            {
                elementId: 'rows',
                parentRows: [],
            },
        ]);
    });

    it('should treat a deleted row as a container change without suppressing remaining row children', () => {
        const retainedRow = {
            id: 'row-2',
            values: {
                rowField: 'retained',
            },
        };
        const previousValues = {
            rows: [
                {
                    id: 'row-1',
                    values: {
                        rowField: 'deleted',
                    },
                },
                retainedRow,
            ],
        };
        const nextValues = {
            rows: [retainedRow],
        };

        expect(collectChangedElementErrorSuppressionTargets(root, previousValues, nextValues)).toEqual([
            {
                elementId: 'rows',
                parentRows: [],
            },
        ]);
    });

    it('should suppress only the changed row field and follow its stable row id after reordering', () => {
        const previousValues = {
            rows: [
                {
                    id: 'row-1',
                    values: {
                        rowField: 'previous',
                    },
                },
                {
                    id: 'row-2',
                    values: {
                        rowField: 'unchanged',
                    },
                },
            ],
        };
        const nextValues = {
            rows: [
                {
                    id: 'row-1',
                    values: {
                        rowField: 'changed',
                    },
                },
                previousValues.rows[1],
            ],
        };
        const targets = collectChangedElementErrorSuppressionTargets(root, previousValues, nextValues);
        const derivedData = createDerivedRuntimeElementData({
            elementStates: {
                rows: {
                    error: 'Container error',
                    errorDetails: {
                        target: 'container',
                    },
                    subStates: [
                        {
                            id: 'row-2',
                            states: {
                                rowField: {
                                    error: 'Unchanged row error',
                                    errorDetails: {
                                        target: 'row-2',
                                    },
                                },
                            },
                        },
                        {
                            id: 'row-1',
                            states: {
                                rowField: {
                                    error: 'Changed row error',
                                    errorDetails: {
                                        target: 'row-1',
                                    },
                                },
                            },
                        },
                    ],
                },
            },
        });

        expect(targets).toEqual([
            {
                elementId: 'rows',
                parentRows: [],
            },
            {
                elementId: 'rowField',
                parentRows: [
                    {
                        replicatingContainerElementId: 'rows',
                        rowId: 'row-1',
                        rowIndex: 0,
                    },
                ],
            },
        ]);
        expect(applyElementErrorSuppressions(derivedData, targets).elementStates).toEqual({
            rows: {
                error: null,
                errorDetails: null,
                subStates: [
                    {
                        id: 'row-2',
                        states: {
                            rowField: {
                                error: 'Unchanged row error',
                                errorDetails: {
                                    target: 'row-2',
                                },
                            },
                        },
                    },
                    {
                        id: 'row-1',
                        states: {
                            rowField: {
                                error: null,
                                errorDetails: null,
                            },
                        },
                    },
                ],
            },
        });
        expect(derivedData.elementStates.rows?.error).toBe('Container error');
    });

    it('should collect nested container changes without marking children of a newly appended nested row', () => {
        const nestedField = {
            id: 'nestedField',
            type: ElementType.Text,
        } as AnyElement;
        const nestedRows = {
            id: 'nestedRows',
            type: ElementType.ReplicatingContainer,
            children: [nestedField],
        } as AnyElement;
        const nestedRoot = {
            id: 'root',
            type: ElementType.GroupLayout,
            children: [
                {
                    id: 'outerRows',
                    type: ElementType.ReplicatingContainer,
                    children: [nestedRows],
                },
            ],
        } as AnyElement;
        const previousValues = {
            outerRows: [
                {
                    id: 'outer-1',
                    values: {
                        nestedRows: [
                            {
                                id: 'nested-1',
                                values: {
                                    nestedField: 'unchanged',
                                },
                            },
                        ],
                    },
                },
            ],
        };
        const nextValues = {
            outerRows: [
                {
                    id: 'outer-1',
                    values: {
                        nestedRows: [
                            ...previousValues.outerRows[0].values.nestedRows,
                            {
                                id: 'nested-2',
                                values: {},
                            },
                        ],
                    },
                },
            ],
        };

        expect(collectChangedElementErrorSuppressionTargets(nestedRoot, previousValues, nextValues)).toEqual([
            {
                elementId: 'outerRows',
                parentRows: [],
            },
            {
                elementId: 'nestedRows',
                parentRows: [
                    {
                        replicatingContainerElementId: 'outerRows',
                        rowId: 'outer-1',
                        rowIndex: 0,
                    },
                ],
            },
        ]);
    });

    it('should deduplicate stable row targets and use the row index for legacy targets', () => {
        const targetAtPreviousIndex = {
            elementId: 'rowField',
            parentRows: [
                {
                    replicatingContainerElementId: 'rows',
                    rowId: 'row-1',
                    rowIndex: 0,
                },
            ],
        };
        const targetAtCurrentIndex = {
            ...targetAtPreviousIndex,
            parentRows: [
                {
                    ...targetAtPreviousIndex.parentRows[0],
                    rowIndex: 1,
                },
            ],
        };
        const legacyTarget = {
            elementId: 'rowField',
            parentRows: [
                {
                    replicatingContainerElementId: 'rows',
                    rowId: null,
                    rowIndex: 1,
                },
            ],
        };
        const derivedData = createDerivedRuntimeElementData({
            elementStates: {
                rows: {
                    subStates: [
                        {
                            states: {
                                rowField: {
                                    error: 'First legacy row error',
                                },
                            },
                        },
                        {
                            states: {
                                rowField: {
                                    error: 'Second legacy row error',
                                },
                            },
                        },
                    ],
                },
            },
        });

        expect(mergeElementErrorSuppressionTargets(
            [targetAtPreviousIndex],
            [targetAtCurrentIndex],
        )).toEqual([targetAtCurrentIndex]);
        expect(
            applyElementErrorSuppressions(derivedData, [legacyTarget])
                .elementStates.rows?.subStates?.[1].states?.rowField?.error,
        ).toBeNull();
        expect(
            applyElementErrorSuppressions(derivedData, [legacyTarget])
                .elementStates.rows?.subStates?.[0].states?.rowField?.error,
        ).toBe('First legacy row error');
    });
});

describe('replicating container row values', () => {
    const field = {
        id: 'rowField',
        type: ElementType.Text,
    } as AnyElement;
    const list = {
        id: 'rows',
        type: ElementType.ReplicatingContainer,
        children: [field],
    } as AnyElement;
    const root = {
        id: 'root',
        type: ElementType.GroupLayout,
        children: [list],
    } as AnyElement;

    it('should walk and map nested row values', () => {
        const authoredValues = {
            rows: [
                {
                    id: 'row-1',
                    values: {
                        rowField: 'Ada',
                    },
                },
            ],
        };
        const visitedValues: unknown[] = [];

        walkAuthoredElementValues(root, authoredValues, (element, value) => {
            if (element.id === 'rowField') {
                visitedValues.push(value);
            }
        });

        expect(visitedValues).toEqual(['Ada']);
        expect(mapAuthoredElementValues(root, authoredValues, (element, value) => {
            return element.id === 'rowField' ? 'Grace' : value;
        })).toEqual({
            rows: [
                {
                    id: 'row-1',
                    values: {
                        rowField: 'Grace',
                    },
                },
            ],
        });
    });

    it('should resolve derived data from nested row values', () => {
        const derivedData = createDerivedRuntimeElementData({
            effectiveValues: {
                rows: [
                    {
                        id: 'row-1',
                        values: {
                            rowField: 'derived',
                        },
                    },
                ],
            },
            elementStates: {
                rows: {
                    subStates: [
                        {
                            id: 'row-1',
                            states: {
                                rowField: {
                                    error: 'Row error',
                                },
                            },
                        },
                    ],
                },
            },
        });

        expect(resolveReplicatingContainerItemDerivedData(list, derivedData, 0)).toEqual({
            effectiveValues: {
                rowField: 'derived',
            },
            elementStates: {
                rowField: {
                    error: 'Row error',
                },
            },
        });
    });

    it('should resolve row state by row id before index', () => {
        const derivedData = createDerivedRuntimeElementData({
            effectiveValues: {
                rows: [
                    {
                        id: 'row-2',
                        values: {
                            rowField: 'derived',
                        },
                    },
                ],
            },
            elementStates: {
                rows: {
                    subStates: [
                        {
                            id: 'row-1',
                            states: {
                                rowField: {
                                    error: 'Wrong row',
                                },
                            },
                        },
                        {
                            id: 'row-2',
                            states: {
                                rowField: {
                                    error: 'Right row',
                                },
                            },
                        },
                    ],
                },
            },
        });

        expect(resolveReplicatingContainerItemDerivedData(list, derivedData, 0).elementStates).toEqual({
            rowField: {
                error: 'Right row',
            },
        });
    });

    it('should propagate computed state for a newly added row', () => {
        const derivedData = createDerivedRuntimeElementData({
            effectiveValues: {
                rows: [
                    {
                        id: 'row-1',
                        values: {},
                    },
                    {
                        id: 'row-2',
                        values: {
                            rowField: 'derived',
                        },
                    },
                ],
            },
            elementStates: {
                rows: {
                    subStates: [
                        {
                            id: 'row-1',
                            states: {},
                        },
                        {
                            id: 'row-2',
                            states: {
                                rowField: {
                                    visible: false,
                                    disabled: true,
                                    valueSource: ComputedElementValueSource.Derived,
                                },
                            },
                        },
                    ],
                },
            },
        });

        expect(resolveReplicatingContainerItemDerivedData(list, derivedData, 1)).toEqual({
            effectiveValues: {
                rowField: 'derived',
            },
            elementStates: {
                rowField: {
                    visible: false,
                    disabled: true,
                    valueSource: ComputedElementValueSource.Derived,
                },
            },
        });
    });

    it('should not use another identified row state when the requested row is missing', () => {
        const derivedData = createDerivedRuntimeElementData({
            effectiveValues: {
                rows: [
                    {
                        id: 'new-row',
                        values: {
                            rowField: 'new value',
                        },
                    },
                ],
            },
            elementStates: {
                rows: {
                    subStates: [
                        {
                            id: 'old-row',
                            states: {
                                rowField: {
                                    error: 'Stale error',
                                },
                            },
                        },
                    ],
                },
            },
        });

        expect(resolveReplicatingContainerItemDerivedData(list, derivedData, 0)).toEqual({
            effectiveValues: {
                rowField: 'new value',
            },
            elementStates: {},
        });
    });

    it('should normalize legacy bare row values', () => {
        expect(normalizeReplicatingContainerValues(root, {
            rows: [
                {
                    rowField: 'legacy',
                },
            ],
        })).toEqual({
            rows: [
                {
                    values: {
                        rowField: 'legacy',
                    },
                },
            ],
        });
    });
});
