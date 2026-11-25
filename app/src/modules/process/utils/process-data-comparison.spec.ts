import {describe, expect, it} from 'vitest';
import {compareProcessData} from './process-data-comparison';

describe('process data comparisons', () => {
    it('ignores key order, including objects inside arrays', () => {
        const comparison = compareProcessData({b: 2, a: [{y: 2, x: 1}]}, {a: [{x: 1, y: 2}], b: 2});
        expect(comparison.unchanged).toBe(true);
        expect(comparison.currentJson).toBe(comparison.previousJson);
    });

    it('distinguishes added/removed fields, explicit nulls and changed arrays', () => {
        const comparison = compareProcessData(
            {person: {name: 'Ada', removed: null}, list: [1, 2], missing: true},
            {person: {name: 'Grace', added: null}, list: [2, 1], newObject: {nested: 1}},
        );
        expect(comparison.counts).toEqual({added: 2, removed: 2, changed: 2});
        expect(comparison.unchanged).toBe(false);
    });

    it('does not claim unchanged data when the predecessor is unavailable', () => {
        const comparison = compareProcessData(null, {});
        expect(comparison.unchanged).toBe(false);
        expect(comparison.previousJson).toBeUndefined();
    });
});
