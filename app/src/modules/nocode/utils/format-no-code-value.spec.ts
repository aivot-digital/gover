import {describe, expect, it} from 'vitest';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import {formatNoCodeValue} from './format-no-code-value';

describe('formatNoCodeValue for authored clock times', () => {
    it.each([
        ['09:30', '09:30:00 Uhr'],
        ['09:30:45', '09:30:45 Uhr'],
        ['00:00', '00:00:00 Uhr'],
        ['23:59:59', '23:59:59 Uhr'],
    ])('reads the typed time %s with seconds in both display modes', (value, expected) => {
        expect(formatNoCodeValue(value, NoCodeDataType.Time)).toBe(expected);
        expect(formatNoCodeValue(value, NoCodeDataType.Time, [], true)).toBe(expected);
    });

    it.each(['24:00', '09:60', '09:30:60', '09:30:00.123', ' 09:30 ', '2026-11-22T09:30:00Z'])
    ('keeps unsupported time input %s visible without turning it into a valid-looking clock time', value => {
        expect(formatNoCodeValue(value, NoCodeDataType.Time)).toBe(`„${value}“`);
        expect(formatNoCodeValue(value, NoCodeDataType.Time, [], true)).toBe(value);
    });

    it('preserves text semantics and authored selection labels', () => {
        const value = '09:30:00';
        expect(formatNoCodeValue(value, NoCodeDataType.String)).toBe('„09:30:00“');
        expect(formatNoCodeValue(value, NoCodeDataType.Runtime)).toBe('„09:30:00“');
        const options = [{label: 'Beginn der Sprechzeit', value}];
        expect(formatNoCodeValue(value, NoCodeDataType.Time, options)).toBe('„Beginn der Sprechzeit“ (09:30:00)');
        expect(formatNoCodeValue(value, NoCodeDataType.Time, options, true)).toBe('Beginn der Sprechzeit');
    });
});
