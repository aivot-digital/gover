import {describe, expect, it} from 'vitest';
import {humanizeNumber, pluralize} from './humanization-utils';

describe('humanizeNumber', () => {
    it.each<[number, string]>([
        [0, '0'],
        [1, 'eine'],
        [2, 'zwei'],
        [12, 'zwölf'],
        [13, '13'],
    ])('humanizes %i with an optional grammatical override', (count, expected) => {
        expect(humanizeNumber(count)).toBe(expected);
        expect(humanizeNumber(count, {1: 'einen'})).toBe(count === 1 ? 'einen' : expected);
        expect(humanizeNumber(count, {[count]: 'angepasst'})).toBe('angepasst');
    });
});

describe('German count labels', () => {
    it.each<[number, string]>([
        [0, '0 Einträge'],
        [1, '1 Eintrag'],
        [2, '2 Einträge'],
    ])('formats a count of %i', (count, expected) => {
        expect(`${count} ${pluralize(count, 'Eintrag', 'Einträge')}`).toBe(expected);
    });
});
