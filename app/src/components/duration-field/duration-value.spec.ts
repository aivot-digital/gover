import {describe, expect, it} from 'vitest';
import {formatDurationSeconds, validateDurationSeconds} from './duration-value';
import {DurationLargestUnit, DurationPrecision} from '../../models/elements/form/input/duration-field-element';

describe('duration value contract', () => {
    it.each([
        [0, '0 Sekunden'], [86400, '1 Tag'], [172800, '2 Tage'],
        [99015, '1 Tag 3 Stunden 30 Minuten 15 Sekunden'], [9000, '2 Stunden 30 Minuten'],
    ])('formats %s with fixed 24-hour days', (value, display) => {
        expect(formatDurationSeconds(value, DurationPrecision.Second, DurationLargestUnit.Day)).toBe(display);
    });

    it('uses days in bound errors without changing validation or precision', () => {
        expect(validateDurationSeconds(86400, DurationPrecision.Minute, 86400, 172800, DurationLargestUnit.Day)).toBeUndefined();
        expect(validateDurationSeconds(0, DurationPrecision.Minute, 86400, 172800, DurationLargestUnit.Day)).toBe('Die Zeitdauer muss mindestens 1 Tag betragen.');
        expect(validateDurationSeconds(259200, DurationPrecision.Minute, 86400, 172800, DurationLargestUnit.Day)).toBe('Die Zeitdauer darf höchstens 2 Tage betragen.');
        expect(validateDurationSeconds(86401, DurationPrecision.Minute, undefined, undefined, DurationLargestUnit.Day)).toContain('ganzen Minuten');
    });

    it.each([
        [null, 'Keine Angabe'], [0, '0 Sekunden'], [1, '1 Sekunde'], [60, '1 Minute'],
        [3600, '1 Stunde'], [9000, '2 Stunden 30 Minuten'], [99015, '27 Stunden 30 Minuten 15 Sekunden'],
    ])('formats %s', (value, display) => expect(formatDurationSeconds(value)).toBe(display));

    it('formats zero at the configured precision', () => {
        expect(formatDurationSeconds(0, DurationPrecision.Minute)).toBe('0 Minuten');
    });

    it.each([-1, 0.5, Infinity, NaN, Number.MAX_SAFE_INTEGER + 1, '', '9000'])('rejects invalid seconds %s', value => {
        expect(validateDurationSeconds(value, DurationPrecision.Second)).toBeDefined();
    });

    it('checks precision and inclusive bounds', () => {
        expect(validateDurationSeconds(null)).toBeUndefined();
        expect(validateDurationSeconds(0)).toBeUndefined();
        expect(validateDurationSeconds(61)).toContain('ganzen Minuten');
        expect(validateDurationSeconds(61, DurationPrecision.Second)).toBeUndefined();
        expect(validateDurationSeconds(60, DurationPrecision.Minute, 60, 120)).toBeUndefined();
        expect(validateDurationSeconds(120, DurationPrecision.Minute, 60, 120)).toBeUndefined();
        expect(validateDurationSeconds(0, DurationPrecision.Minute, 60, 120)).toContain('mindestens');
        expect(validateDurationSeconds(180, DurationPrecision.Minute, 60, 120)).toContain('höchstens');
    });
});
