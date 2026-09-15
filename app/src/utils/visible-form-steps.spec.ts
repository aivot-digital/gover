import {describe, expect, it} from 'vitest';
import {
    resolveVisibleFormStepIndex,
    resolveVisibleFormStepIndexAfterChange,
} from './visible-form-steps';

describe('resolveVisibleFormStepIndex', () => {
    it('should return null when no visible steps exist', () => {
        expect(resolveVisibleFormStepIndex(2, 0)).toBeNull();
    });

    it('should clamp the active step to the last visible step', () => {
        expect(resolveVisibleFormStepIndex(3, 3)).toBe(2);
    });

    it('should clamp negative step indexes to the first visible step', () => {
        expect(resolveVisibleFormStepIndex(-1, 3)).toBe(0);
    });
});

describe('resolveVisibleFormStepIndexAfterChange', () => {
    it('should keep the previously active step when it still exists at another index', () => {
        expect(resolveVisibleFormStepIndexAfterChange(2, [
            'step-b',
            'step-c',
            'step-d',
        ], 'step-c')).toBe(1);
    });

    it('should move to the next step position when the active step was removed', () => {
        expect(resolveVisibleFormStepIndexAfterChange(1, [
            'step-a',
            'step-c',
            'step-d',
        ], 'step-b')).toBe(1);
    });

    it('should move to the previous last step when the removed active step was last', () => {
        expect(resolveVisibleFormStepIndexAfterChange(3, [
            'step-a',
            'step-b',
            'step-c',
        ], 'step-d')).toBe(2);
    });
});
