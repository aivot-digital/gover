import {describe, expect, it} from 'vitest';
import {ConditionOperator} from '../data/condition-operator';
import {ChipInputEvaluator} from './chip-input-evaluator';

describe('ChipInputEvaluator', () => {
    it.each([
        [undefined, true],
        [[], true],
        [['A'], false],
    ] as const)('handles empty lists: %j', (value, empty) => {
        const entries = value == null ? undefined : [...value];
        expect(ChipInputEvaluator[ConditionOperator.Empty]!(entries, null)).toBe(empty);
        expect(ChipInputEvaluator[ConditionOperator.NotEmpty]!(entries, null)).toBe(!empty);
    });

    it.each([
        [['A', 'B'], ['A', 'B'], true, false],
        [['A'], ['A', 'B'], false, false],
        [['A'], ['B', 'C'], false, true],
        [undefined, 'A', false, true],
        [undefined, [], true, true],
        [['A'], null, false, false],
        [['A'], 'A', true, false],
        [['A'], 'B', false, true],
    ])('uses all matching entries for Includes and no matching entries for NotIncludes: %j, %j', (value, compared, includes, notIncludes) => {
        expect(ChipInputEvaluator[ConditionOperator.Includes]!(value as string[] | undefined, compared)).toBe(includes);
        expect(ChipInputEvaluator[ConditionOperator.NotIncludes]!(value as string[] | undefined, compared)).toBe(notIncludes);
    });
});
