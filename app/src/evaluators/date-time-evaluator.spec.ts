import {describe, expect, it} from 'vitest';
import {ConditionOperator} from '../data/condition-operator';
import {DateTimeEvaluator} from './date-time-evaluator';

function evaluate(operator: ConditionOperator, valueA?: string, valueB?: string): boolean {
    return DateTimeEvaluator[operator]!(valueA, valueB);
}

describe('DateTimeEvaluator', () => {
    it('should compare explicit instants independently of their offset representation', () => {
        expect(evaluate(
            ConditionOperator.Equals,
            '2026-07-29T09:30:00+02:00',
            '2026-07-29T07:30:00Z',
        )).toBe(true);
        expect(evaluate(
            ConditionOperator.LessThan,
            '2026-07-29T09:29:59+02:00',
            '2026-07-29T07:30:00Z',
        )).toBe(true);
    });

    it('should reject offsetless and legacy display values', () => {
        expect(evaluate(
            ConditionOperator.Equals,
            '2026-07-29T09:30:00',
            '2026-07-29T09:30:00+02:00',
        )).toBe(false);
        expect(evaluate(
            ConditionOperator.NotEquals,
            '29.07.2026 09:30',
            '2026-07-29T09:30:00+02:00',
        )).toBe(false);
    });

    it('should preserve instant ordering below millisecond precision', () => {
        expect(evaluate(
            ConditionOperator.Equals,
            '2026-07-29T07:30:00.000000001Z',
            '2026-07-29T09:30:00.000000001+02:00',
        )).toBe(true);
        expect(evaluate(
            ConditionOperator.LessThan,
            '2026-07-29T07:30:00.000000001Z',
            '2026-07-29T07:30:00.000000002Z',
        )).toBe(true);
    });
});
