import {afterEach, beforeEach, describe, expect, it} from 'vitest';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import {formatNoCodeValue} from './format-no-code-value';
import {formatNoCodeDateTimeValue} from './no-code-date-time-value';

describe('formatNoCodeDateTimeValue', () => {
    const originalTimeZone = AppConfig.applicationTimeZone;

    beforeEach(() => {
        AppConfig.applicationTimeZone = 'Europe/Berlin';
    });

    afterEach(() => {
        AppConfig.applicationTimeZone = originalTimeZone;
    });

    it('uses the configured timezone for different representations of the same instant', () => {
        const value = '2026-11-22T12:30:00Z';
        expect(formatNoCodeDateTimeValue(value)).toBe('22.11.2026 13:30:00 Uhr');
        expect(formatNoCodeDateTimeValue('2026-11-22T13:30:00+01:00')).toBe('22.11.2026 13:30:00 Uhr');
        AppConfig.applicationTimeZone = 'UTC';
        expect(formatNoCodeDateTimeValue(value)).toBe('22.11.2026 12:30:00 Uhr');
    });

    it('converts the date as well as the time when crossing a timezone boundary', () => {
        expect(formatNoCodeDateTimeValue('2026-07-29T23:30:00Z')).toBe('30.07.2026 01:30:00 Uhr');
        AppConfig.applicationTimeZone = 'Asia/Kolkata';
        expect(formatNoCodeDateTimeValue('2026-07-29T23:30:00Z')).toBe('30.07.2026 05:00:00 Uhr');
    });

    it('accepts the evaluator’s extreme valid offsets and treats explicit offsets as absolute instants', () => {
        expect(formatNoCodeDateTimeValue('2026-11-22T13:30:00+18:00')).toBe('21.11.2026 20:30:00 Uhr');
        expect(formatNoCodeDateTimeValue('2026-11-22T13:30:00-18:00')).toBe('23.11.2026 08:30:00 Uhr');
        expect(formatNoCodeDateTimeValue('2026-03-29T02:30:00+01:00')).toBe('29.03.2026 03:30:00 Uhr');
    });

    it('keeps fractional seconds beyond millisecond precision visible', () => {
        expect(formatNoCodeDateTimeValue('2026-11-22T12:30:45.123456789Z'))
            .toBe('22.11.2026 13:30:45,123456789 Uhr');
        expect(formatNoCodeDateTimeValue('2026-11-22T12:30:45.000000001Z'))
            .toBe('22.11.2026 13:30:45,000000001 Uhr');
        expect(formatNoCodeDateTimeValue('2026-03-29T00:59:59.999999999Z'))
            .toBe('29.03.2026 01:59:59,999999999 Uhr');
        expect(formatNoCodeDateTimeValue('2026-03-29T01:00:00.000000001Z'))
            .toBe('29.03.2026 03:00:00,000000001 Uhr');
    });

    it('distinguishes both occurrences of repeated local times regardless of authored offset', () => {
        const first = '25.10.2026 02:30:00,123456789 Uhr (erstes Vorkommen)';
        const second = '25.10.2026 02:30:00,123456789 Uhr (zweites Vorkommen)';
        expect(formatNoCodeDateTimeValue('2026-10-25T02:30:00.123456789+02:00')).toBe(first);
        expect(formatNoCodeDateTimeValue('2026-10-25T00:30:00.123456789Z')).toBe(first);
        expect(formatNoCodeDateTimeValue('2026-10-25T02:30:00.123456789+01:00')).toBe(second);
        expect(formatNoCodeDateTimeValue('2026-10-25T01:30:00.123456789Z')).toBe(second);
    });

    it('supports legacy local timestamp literals using the same timezone resolution as the evaluator', () => {
        expect(formatNoCodeDateTimeValue('2026-11-22T13:30:00')).toBe('22.11.2026 13:30:00 Uhr');
        expect(formatNoCodeDateTimeValue('2026-11-22T13:30:00.123456789'))
            .toBe('22.11.2026 13:30:00,123456789 Uhr');
        expect(formatNoCodeDateTimeValue('2026-10-25T02:30:00.000000001'))
            .toBe('25.10.2026 02:30:00,000000001 Uhr (erstes Vorkommen)');
        expect(formatNoCodeDateTimeValue('2026-03-29T02:30:00')).toBeNull();
        expect(formatNoCodeDateTimeValue('2026-03-29T02:30:00.123456789')).toBeNull();
    });

    it('keeps the two occurrences distinguishable for a non-hour time change', () => {
        AppConfig.applicationTimeZone = 'Australia/Lord_Howe';
        expect(formatNoCodeDateTimeValue('2026-04-05T01:45:00+11:00'))
            .toBe('05.04.2026 01:45:00 Uhr (erstes Vorkommen)');
        expect(formatNoCodeDateTimeValue('2026-04-05T01:45:00+10:30'))
            .toBe('05.04.2026 01:45:00 Uhr (zweites Vorkommen)');
    });

    it.each([
        null, undefined, 42, 'kein Zeitpunkt', '2026-11-22', '2026-11-22T13:30',
        '2026-11-22T13:30:00[Europe/Berlin]', '2026-11-22T24:00:00Z',
        '2026-02-30T13:30:00Z', '2026-02-30T13:30:00',
        '2026-11-22T13:30:00.1234567890Z', '+12026-11-22T13:30:00Z', '9999-12-31T23:30:00-18:00',
        '2026-11-22T13:30:00+19:00', '2026-11-22T13:30:00+18:01',
        '2026-11-22T13:30:00+99:99', '2026-11-22T13:30:00+00:60',
    ])('leaves the unsupported or invalid value %j to the existing raw-value fallback', value => {
        expect(formatNoCodeDateTimeValue(value)).toBeNull();
    });

    it('formats typed date-time values only and keeps option labels and invalid literals visible', () => {
        const value = '2026-11-22T13:30:00+01:00';
        expect(formatNoCodeValue(value, NoCodeDataType.DateTime)).toBe('22.11.2026 13:30:00 Uhr');
        expect(formatNoCodeValue(value, NoCodeDataType.DateTime, [], true)).toBe('22.11.2026 13:30:00 Uhr');
        expect(formatNoCodeValue(value, NoCodeDataType.String)).toBe(`„${value}“`);
        expect(formatNoCodeValue(value, NoCodeDataType.Runtime)).toBe(`„${value}“`);
        expect(formatNoCodeValue(value, NoCodeDataType.DateTime, [{label: 'Termin', value}]))
            .toBe(`„Termin“ (${value})`);
        expect(formatNoCodeValue('2026-02-30T13:30:00Z', NoCodeDataType.DateTime))
            .toBe('„2026-02-30T13:30:00Z“');
        expect(formatNoCodeValue('2026-11-22T13:30:00+19:00', NoCodeDataType.DateTime, [], true))
            .toBe('2026-11-22T13:30:00+19:00');
    });
});
