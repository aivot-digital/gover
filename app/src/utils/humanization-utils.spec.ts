import {describe, expect, it} from 'vitest';
import {pluralize} from './humanization-utils';

describe('German count labels', () => {
    it.each<[number, string]>([
        [0, '0 Einträge'],
        [1, '1 Eintrag'],
        [2, '2 Einträge'],
    ])('formats a count of %i', (count, expected) => {
        expect(`${count} ${pluralize(count, 'Eintrag', 'Einträge')}`).toBe(expected);
    });
});
