import {applyComputedErrors, ComputedElementErrors, ComputedElementStates} from './element-data';

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
                valueSource: 'Derived',
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
                        child: {
                            error: 'Updated nested error',
                            errorDetails: {
                                error: 'Updated nested operand error',
                            },
                        },
                    },
                    {
                        created: {
                            error: 'Created nested error',
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
                        child: {
                            visible: true,
                            error: 'Old nested error',
                            errorDetails: {
                                error: 'Old nested operand error',
                            },
                            valueSource: 'Authored',
                        },
                    },
                    {
                        retained: {
                            error: 'Retained sibling error',
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
                        child: {
                            visible: true,
                            error: 'Updated nested error',
                            errorDetails: {
                                error: 'Updated nested operand error',
                            },
                            valueSource: 'Authored',
                        },
                    },
                    {
                        retained: {
                            error: 'Retained sibling error',
                        },
                        created: {
                            error: 'Created nested error',
                        },
                    },
                ],
            },
        });
    });

    it('should clear sub states when computed errors provide an empty array', () => {
        const computedErrors: ComputedElementErrors = {
            list: {
                subStates: [],
            },
        };
        const existingStates: ComputedElementStates = {
            list: {
                subStates: [
                    {
                        child: {
                            error: 'Nested error',
                        },
                    },
                ],
            },
        };

        expect(applyComputedErrors(computedErrors, existingStates)).toEqual({
            list: {
                subStates: [],
            },
        });
    });
});
