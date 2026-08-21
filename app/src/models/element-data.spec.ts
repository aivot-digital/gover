import {describe, expect, it} from 'vitest';
import {
    applyComputedErrors,
    type ComputedElementErrors,
    type ComputedElementStates,
    ComputedElementValueSource,
    hasAuthoredElementValuesSomeInput,
    hasAnyErrorRecursivelyInParent,
    resolveComputedElementSubState,
} from './element-data';
import {ElementType} from '../data/element-type/element-type';

describe('hasAuthoredElementValuesSomeInput', () => {
    it('should treat an explicit null as authored input', () => {
        expect(hasAuthoredElementValuesSomeInput({field: null})).toBe(true);
    });

    it('should ignore missing and undefined values', () => {
        expect(hasAuthoredElementValuesSomeInput({})).toBe(false);
        expect(hasAuthoredElementValuesSomeInput({field: undefined})).toBe(false);
    });
});

describe('hasAnyErrorRecursivelyInParent', () => {
    it.each([
        ElementType.SummaryStep,
        ElementType.SubmitStep,
    ])('should detect an error on a childless form step of type %s', (type) => {
        const step = {
            id: 'childless-step',
            type,
        } as any;
        const elementStates: ComputedElementStates = {
            'childless-step': {
                error: 'Step error',
            },
        };

        expect(hasAnyErrorRecursivelyInParent(step, elementStates)).toBe(true);
    });

    it('should detect an error on the parent itself', () => {
        const parent = {
            id: 'parent',
            type: ElementType.Step,
            children: [],
        } as any;
        const elementStates: ComputedElementStates = {
            parent: {
                error: 'Parent error',
            },
            sibling: {
                error: 'Sibling error',
            },
        };

        expect(hasAnyErrorRecursivelyInParent(parent, elementStates)).toBe(true);
    });

    it('should detect an empty error marker on the parent itself', () => {
        const parent = {
            id: 'parent',
            type: ElementType.Step,
            children: [],
        } as any;
        const elementStates: ComputedElementStates = {
            parent: {
                error: '',
            },
        };

        expect(hasAnyErrorRecursivelyInParent(parent, elementStates)).toBe(true);
    });

    it('should detect errors in regular descendants of the parent', () => {
        const parent = {
            id: 'parent',
            type: ElementType.Step,
            children: [
                {
                    id: 'group',
                    type: ElementType.GroupLayout,
                    children: [
                        {
                            id: 'field',
                            type: ElementType.Text,
                        },
                    ],
                },
            ],
        } as any;
        const elementStates: ComputedElementStates = {
            parent: {},
            group: {},
            field: {
                error: 'Field error',
            },
        };

        expect(hasAnyErrorRecursivelyInParent(parent, elementStates)).toBe(true);
    });

    it('should detect errors in replicated row descendants', () => {
        const parent = {
            id: 'parent',
            type: ElementType.Step,
            children: [
                {
                    id: 'list',
                    type: ElementType.ReplicatingContainer,
                    children: [
                        {
                            id: 'rowField',
                            type: ElementType.Text,
                        },
                    ],
                },
            ],
        } as any;
        const elementStates: ComputedElementStates = {
            parent: {},
            list: {
                subStates: [
                    {
                        id: 'row-1',
                        states: {
                            rowField: {
                                error: 'Row field error',
                            },
                        },
                    },
                ],
            },
        };

        expect(hasAnyErrorRecursivelyInParent(parent, elementStates)).toBe(true);
    });

    it('should detect empty error markers in replicated row descendants', () => {
        const parent = {
            id: 'parent',
            type: ElementType.Step,
            children: [
                {
                    id: 'list',
                    type: ElementType.ReplicatingContainer,
                    children: [
                        {
                            id: 'rowField',
                            type: ElementType.Text,
                        },
                    ],
                },
            ],
        } as any;
        const elementStates: ComputedElementStates = {
            parent: {},
            list: {
                subStates: [
                    {
                        id: 'row-1',
                        states: {
                            rowField: {
                                error: '',
                            },
                        },
                    },
                ],
            },
        };

        expect(hasAnyErrorRecursivelyInParent(parent, elementStates)).toBe(true);
    });

    it('should ignore errors outside the parent element tree', () => {
        const parent = {
            id: 'parent',
            type: ElementType.Step,
            children: [
                {
                    id: 'field',
                    type: ElementType.Text,
                },
            ],
        } as any;
        const elementStates: ComputedElementStates = {
            parent: {},
            field: {
            },
            sibling: {
                error: '',
            },
        };

        expect(hasAnyErrorRecursivelyInParent(parent, elementStates)).toBe(false);
    });
});

describe('resolveComputedElementSubState', () => {
    it('should not fall back to an indexed sub state with a different id', () => {
        const indexedSubState = {
            id: 'old-row',
            states: {
                field: {
                    error: 'Stale error',
                },
            },
        };

        expect(resolveComputedElementSubState([indexedSubState], 'new-row', 0)).toBeNull();
    });

    it('should resolve an id-less sub state by index', () => {
        const indexedSubState = {
            id: null,
            states: {
                field: {
                    error: 'Indexed error',
                },
            },
        };

        expect(resolveComputedElementSubState([indexedSubState], null, 0)).toBe(indexedSubState);
    });
});

describe('applyComputedErrors', () => {
    it('should override existing errors without changing unrelated state fields', () => {
        const computedErrors: ComputedElementErrors = {
            field: {
                error: 'New error',
                errorDetails: {
                    operand: {
                        type: 'NoCodeStaticValue',
                        value: '',
                    },
                    error: 'New operand error',
                    subErrors: null,
                    isValid: false,
                },
            },
        };
        const existingStates: ComputedElementStates = {
            field: {
                visible: false,
                error: 'Old error',
                errorDetails: {
                    error: 'Old operand error',
                },
                valueSource: ComputedElementValueSource.Derived,
                subStates: null,
            },
        };

        expect(applyComputedErrors(computedErrors, existingStates)).toEqual({
            field: {
                visible: false,
                error: 'New error',
                errorDetails: {
                    operand: {
                        type: 'NoCodeStaticValue',
                        value: '',
                    },
                    error: 'New operand error',
                    subErrors: null,
                    isValid: false,
                },
                valueSource: 'Derived',
                subStates: null,
            },
        });
        expect(existingStates.field?.error).toBe('Old error');
    });

    it('should preserve existing errors when no overriding error was provided', () => {
        const computedErrors: ComputedElementErrors = {
            field: {},
        };
        const existingStates: ComputedElementStates = {
            field: {
                error: 'Existing error',
                errorDetails: {
                    error: 'Existing operand error',
                },
            },
        };

        expect(applyComputedErrors(computedErrors, existingStates)).toEqual(existingStates);
    });

    it('should clear error details when computed errors explicitly provide null details', () => {
        const computedErrors: ComputedElementErrors = {
            field: {
                errorDetails: null,
            },
        };
        const existingStates: ComputedElementStates = {
            field: {
                error: 'Existing error',
                errorDetails: {
                    error: 'Existing operand error',
                },
            },
        };

        expect(applyComputedErrors(computedErrors, existingStates)).toEqual({
            field: {
                error: 'Existing error',
                errorDetails: null,
            },
        });
    });

    it('should apply nested errors recursively to sub states', () => {
        const computedErrors: ComputedElementErrors = {
            list: {
                error: null,
                subStates: [
                    {
                        id: 'row-1',
                        states: {
                            child: {
                                error: 'Updated nested error',
                                errorDetails: {
                                    error: 'Updated nested operand error',
                                },
                            },
                        },
                    },
                    {
                        id: 'row-2',
                        states: {
                            created: {
                                error: 'Created nested error',
                            },
                        },
                    },
                ],
            },
        };
        const existingStates: ComputedElementStates = {
            list: {
                error: 'Container error',
                subStates: [
                    {
                        id: 'row-1',
                        states: {
                            child: {
                                visible: true,
                                error: 'Old nested error',
                                errorDetails: {
                                    error: 'Old nested operand error',
                                },
                                valueSource: ComputedElementValueSource.Authored,
                            },
                        },
                    },
                    {
                        id: 'row-2',
                        states: {
                            retained: {
                                error: 'Retained sibling error',
                            },
                        },
                    },
                ],
            },
        };

        expect(applyComputedErrors(computedErrors, existingStates)).toEqual({
            list: {
                error: null,
                subStates: [
                    {
                        id: 'row-1',
                        states: {
                            child: {
                                visible: true,
                                error: 'Updated nested error',
                                errorDetails: {
                                    error: 'Updated nested operand error',
                                },
                                valueSource: ComputedElementValueSource.Authored,
                            },
                        },
                    },
                    {
                        id: 'row-2',
                        states: {
                            retained: {
                                error: 'Retained sibling error',
                            },
                            created: {
                                error: 'Created nested error',
                            },
                        },
                    },
                ],
            },
        });
    });

    it('should preserve derived rows missing from computed errors', () => {
        const computedErrors: ComputedElementErrors = {
            list: {
                subStates: [
                    {
                        id: 'row-1',
                        states: {
                            child: {
                                error: 'Updated error',
                            },
                        },
                    },
                ],
            },
        };
        const existingStates: ComputedElementStates = {
            list: {
                subStates: [
                    {
                        id: 'row-1',
                        states: {
                            child: {
                                visible: true,
                            },
                        },
                    },
                    {
                        id: 'row-2',
                        states: {
                            child: {
                                disabled: true,
                            },
                        },
                    },
                ],
            },
        };

        expect(applyComputedErrors(computedErrors, existingStates)).toEqual({
            list: {
                subStates: [
                    {
                        id: 'row-1',
                        states: {
                            child: {
                                visible: true,
                                error: 'Updated error',
                            },
                        },
                    },
                    {
                        id: 'row-2',
                        states: {
                            child: {
                                disabled: true,
                            },
                        },
                    },
                ],
            },
        });
    });

    it('should match reordered sub states by id', () => {
        const computedErrors: ComputedElementErrors = {
            list: {
                subStates: [
                    {
                        id: 'row-1',
                        states: {
                            child: {
                                error: 'First row error',
                            },
                        },
                    },
                    {
                        id: 'row-2',
                        states: {
                            child: {
                                error: 'Second row error',
                            },
                        },
                    },
                ],
            },
        };
        const existingStates: ComputedElementStates = {
            list: {
                subStates: [
                    {
                        id: 'row-2',
                        states: {
                            child: {
                                visible: false,
                            },
                        },
                    },
                    {
                        id: 'row-1',
                        states: {
                            child: {
                                visible: true,
                            },
                        },
                    },
                ],
            },
        };

        const result = applyComputedErrors(computedErrors, existingStates);

        expect(result.list?.subStates?.[0].states?.child).toEqual({
            visible: false,
            error: 'Second row error',
        });
        expect(result.list?.subStates?.[1].states?.child).toEqual({
            visible: true,
            error: 'First row error',
        });
    });

    it('should not recreate rows missing from derived states', () => {
        const computedErrors: ComputedElementErrors = {
            list: {
                subStates: [
                    {
                        id: 'removed-row',
                        states: {
                            child: {
                                error: 'Stale error',
                            },
                        },
                    },
                ],
            },
        };
        const existingStates: ComputedElementStates = {
            list: {
                subStates: [
                    {
                        id: 'current-row',
                        states: {
                            child: {
                                visible: true,
                            },
                        },
                    },
                ],
            },
        };

        expect(applyComputedErrors(computedErrors, existingStates)).toEqual(existingStates);
    });

    it('should preserve derived sub states when computed errors provide an empty array', () => {
        const computedErrors: ComputedElementErrors = {
            list: {
                subStates: [],
            },
        };
        const existingStates: ComputedElementStates = {
            list: {
                subStates: [
                    {
                        id: 'row-1',
                        states: {
                            child: {
                                error: 'Nested error',
                            },
                        },
                    },
                ],
            },
        };

        expect(applyComputedErrors(computedErrors, existingStates)).toEqual(existingStates);
    });
});
