import {describe, expect, it, vi} from 'vitest';
import {ConditionOperator} from '../data/condition-operator';
import {DateEvaluator} from './date-evaluator';

function evaluate(operator: ConditionOperator, valueA?: string, valueB?: string): boolean {
    return DateEvaluator[operator]!(valueA, valueB);
}

describe('DateEvaluator', () => {
    it('should compare canonical local date values by their precision', () => {
        expect(evaluate(ConditionOperator.Equals, '2026-07-29', '2026-07-29')).toBe(true);
        expect(evaluate(ConditionOperator.Equals, '2026-07-29', '2026-07')).toBe(true);
        expect(evaluate(ConditionOperator.Equals, '2026-07-29', '2026')).toBe(true);
        expect(evaluate(ConditionOperator.LessThan, '2026-07-29', '2026-07-30')).toBe(true);
        expect(evaluate(ConditionOperator.GreaterThanOrEqual, '2026-07-29', '2026-07-29')).toBe(true);
    });

    it('should reject legacy instant strings as date values', () => {
        expect(evaluate(
            ConditionOperator.Equals,
            '2026-07-29T00:00:00Z',
            '2026-07-29',
        )).toBe(false);
        expect(evaluate(
            ConditionOperator.NotEquals,
            '2026-07-29T00:00:00Z',
            '2026-07-29',
        )).toBe(false);
        expect(evaluate(
            ConditionOperator.Equals,
            '29.07.2026',
            '2026-07-29',
        )).toBe(false);
    });

    it('should calculate relative dates from the application date', () => {
        vi.useFakeTimers();

        try {
            vi.setSystemTime(new Date('2026-07-29T22:30:00Z'));
            expect(evaluate(ConditionOperator.DaysInPast, '2026-07-29', '1')).toBe(true);
            expect(evaluate(ConditionOperator.DaysInFuture, '2026-07-31', '1')).toBe(true);
        } finally {
            vi.useRealTimers();
        }
    });

    it('should compare partial authored dates independently of the current date', () => {
        vi.useFakeTimers();

        try {
            vi.setSystemTime(new Date('2026-02-01T12:00:00Z'));
            expect(evaluate(ConditionOperator.Equals, '2026-01-31', '31.')).toBe(true);
            expect(evaluate(ConditionOperator.Equals, '2028-02-29', '29.02.')).toBe(true);
        } finally {
            vi.useRealTimers();
        }
    });

    it('should reject non-integer relative amounts', () => {
        expect(evaluate(ConditionOperator.DaysInPast, '2026-07-29', '1day')).toBe(false);
        expect(evaluate(ConditionOperator.DaysInPast, '2026-07-29', '1.5')).toBe(false);
        expect(evaluate(ConditionOperator.DaysInPast, '2026-07-29', '')).toBe(false);
        expect(evaluate(ConditionOperator.DaysInPast, '2026-07-29', ' 1')).toBe(false);
    });
});
