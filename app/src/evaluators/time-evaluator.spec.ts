import {describe, expect, it} from 'vitest';
import {ConditionOperator} from '../data/condition-operator';
import {TimeEvaluator} from './time-evaluator';

function evaluate(operator: ConditionOperator, valueA?: string, valueB?: string): boolean {
    return TimeEvaluator[operator]!(valueA, valueB);
}

describe('TimeEvaluator', () => {
    it('should compare canonical local times including seconds', () => {
        expect(evaluate(ConditionOperator.Equals, '09:30', '09:30:00')).toBe(true);
        expect(evaluate(ConditionOperator.LessThan, '09:30:14', '09:30:15')).toBe(true);
        expect(evaluate(ConditionOperator.GreaterThan, '09:30:15', '09:30:14')).toBe(true);
    });

    it('should not treat invalid values as equal', () => {
        expect(evaluate(ConditionOperator.Equals, 'invalid', 'invalid')).toBe(false);
        expect(evaluate(ConditionOperator.NotEquals, 'invalid', '09:30')).toBe(false);
    });

    it('should reject legacy instant strings as time values', () => {
        expect(evaluate(
            ConditionOperator.Equals,
            '2026-07-29T09:30:00Z',
            '09:30',
        )).toBe(false);
    });
});
