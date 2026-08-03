import {ElementType} from '../data/element-type/element-type';
import {ComputedElementValueSource, createDerivedRuntimeElementData} from '../models/element-data';
import {
    mapAuthoredElementValues,
    normalizeReplicatingContainerValues,
    resolveReplicatingContainerItemDerivedData,
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
