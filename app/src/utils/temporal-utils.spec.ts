import {describe, expect, it, vi} from 'vitest';
import {
    canonicalizeInstant,
    compareInstantIso,
    dateTimeToDateValueIso,
    dateTimeToLocalTimeIso,
    dateValueToDateTime,
    formatEpochMillisInApplicationTimeZone,
    formatInstantInApplicationTimeZone,
    formatRelativeEpochMillisInApplicationTimeZone,
    formatRelativeInstantInApplicationTimeZone,
    getCurrentApplicationDate,
    instantToEpochMillis,
    isInstantIso,
    isLocalDateIso,
    isLocalDateTimeIso,
    isLocalTimeIso,
    isYearIso,
    isYearMonthIso,
    localTimeIsoToDateTime,
    parseIanaTimeZone,
    resolveLocalDateTime,
} from './temporal-utils';
import {LocalDateTimeIso} from './temporal-types';
import {DateTime} from 'luxon';

describe('temporal-utils', () => {
    const berlin = parseIanaTimeZone('Europe/Berlin');

    it('should validate the temporal wire formats by semantic type', () => {
        expect(isInstantIso('2026-07-29T09:00:00+02:00')).toBe(true);
        expect(isInstantIso('2026-07-29T07:00:00Z')).toBe(true);
        expect(isInstantIso('2026-07-29T09:00:00')).toBe(false);
        expect(isInstantIso('2026-07-29T24:00:00Z')).toBe(false);

        expect(isLocalDateIso('2026-07-29')).toBe(true);
        expect(isLocalDateIso('2026-02-30')).toBe(false);
        expect(isYearMonthIso('2026-07')).toBe(true);
        expect(isYearMonthIso('2026-13')).toBe(false);
        expect(isYearIso('2026')).toBe(true);
        expect(isYearIso('26')).toBe(false);
        expect(isLocalTimeIso('09:30')).toBe(true);
        expect(isLocalTimeIso('09:30:15')).toBe(true);
        expect(isLocalTimeIso('24:00')).toBe(false);
        expect(isLocalDateTimeIso('2026-07-29T09:30:00')).toBe(true);
        expect(isLocalDateTimeIso('2026-07-29T09:30:00Z')).toBe(false);
    });

    it('should resolve a regular local datetime with the application timezone offset', () => {
        const result = resolveLocalDateTime(
            '2026-07-29T09:00:00' as LocalDateTimeIso,
            berlin,
        );

        expect(result).toMatchObject({
            resolved: true,
            value: '2026-07-29T09:00:00+02:00',
        });
    });

    it('should reject a local datetime in the spring DST gap', () => {
        const result = resolveLocalDateTime(
            '2026-03-29T02:30:00' as LocalDateTimeIso,
            berlin,
        );

        expect(result).toEqual({
            resolved: false,
            reason: 'nonexistent',
        });
    });

    it('should select the earlier occurrence in the autumn DST overlap', () => {
        const result = resolveLocalDateTime(
            '2026-10-25T02:30:00' as LocalDateTimeIso,
            berlin,
        );

        expect(result).toMatchObject({
            resolved: true,
            value: '2026-10-25T02:30:00+02:00',
        });
    });

    it('should canonicalize incoming instants to the application timezone', () => {
        expect(canonicalizeInstant('2026-07-29T07:00:00Z', berlin, 'minute'))
            .toBe('2026-07-29T09:00:00+02:00');
        expect(canonicalizeInstant('2026-01-29T08:00:00Z', berlin, 'minute'))
            .toBe('2026-01-29T09:00:00+01:00');
    });

    it('should format instants in the application timezone', () => {
        expect(formatInstantInApplicationTimeZone(
            '2026-07-29T07:00:00Z',
            'dd.MM.yyyy, HH:mm:ss',
        )).toBe('29.07.2026, 09:00:00');
        expect(formatInstantInApplicationTimeZone(
            '2026-01-29T08:00:00Z',
            'dd.MM.yyyy, HH:mm:ss',
        )).toBe('29.01.2026, 09:00:00');
        expect(formatInstantInApplicationTimeZone(
            '2026-07-29T09:00:00',
            'dd.MM.yyyy, HH:mm:ss',
        )).toBeNull();
    });

    it('should derive comparable epoch milliseconds only from explicit instants', () => {
        expect(instantToEpochMillis('2026-07-29T09:00:00+02:00'))
            .toBe(instantToEpochMillis('2026-07-29T07:00:00Z'));
        expect(instantToEpochMillis('2026-07-29T09:00:00')).toBeNull();
    });

    it('should compare instants without discarding sub-millisecond precision', () => {
        expect(compareInstantIso(
            '2026-07-29T07:00:00.000000001Z',
            '2026-07-29T09:00:00.000000001+02:00',
        )).toBe(0);
        expect(compareInstantIso(
            '2026-07-29T07:00:00.000000001Z',
            '2026-07-29T07:00:00.000000002Z',
        )).toBeLessThan(0);
        expect(compareInstantIso(
            '2026-07-29T07:00:00',
            '2026-07-29T07:00:00Z',
        )).toBeNull();
    });

    it('should format epoch milliseconds in the application timezone', () => {
        expect(formatEpochMillisInApplicationTimeZone(
            Date.parse('2026-07-29T07:00:00Z'),
            'HH:mm:ss',
        )).toBe('09:00:00');
        expect(formatEpochMillisInApplicationTimeZone(Number.NaN, 'HH:mm:ss')).toBeNull();
    });

    it('should format relative instants with German labels and rounded distances', () => {
        const base = Date.parse('2026-08-07T10:30:00Z');

        expect(formatRelativeInstantInApplicationTimeZone(
            '2026-07-18T22:00:00Z',
            base,
        )).toBe('vor 20 Tagen');
        expect(formatRelativeEpochMillisInApplicationTimeZone(
            base - 30_000,
            base,
        )).toBe('vor 30 Sekunden');
        expect(formatRelativeEpochMillisInApplicationTimeZone(
            base - 90 * 60_000,
            base,
        )).toBe('vor 2 Stunden');
        expect(formatRelativeEpochMillisInApplicationTimeZone(
            base + 60 * 60_000,
            base,
        )).toBe('in 1 Stunde');
    });

    it('should reject invalid values for relative formatting', () => {
        expect(formatRelativeInstantInApplicationTimeZone('2026-08-07T10:30:00')).toBeNull();
        expect(formatRelativeEpochMillisInApplicationTimeZone(Number.NaN)).toBeNull();
        expect(formatRelativeEpochMillisInApplicationTimeZone(0, Number.NaN)).toBeNull();
    });

    it('should parse and format calendar values without creating an instant', () => {
        expect(dateValueToDateTime('2026-07-29', 'day')?.toISO())
            .toBe('2026-07-29T00:00:00.000Z');
        expect(dateValueToDateTime('2026-07', 'month')?.toISO())
            .toBe('2026-07-01T00:00:00.000Z');
        expect(dateValueToDateTime('2026', 'year')?.toISO())
            .toBe('2026-01-01T00:00:00.000Z');
        expect(dateValueToDateTime('2026-02-30', 'day')).toBeNull();

        const pickerValue = DateTime.fromISO('2026-07-29T23:00:00', {zone: 'Asia/Tokyo'});
        expect(dateTimeToDateValueIso(pickerValue, 'day')).toBe('2026-07-29');
        expect(dateTimeToDateValueIso(pickerValue, 'month')).toBe('2026-07');
        expect(dateTimeToDateValueIso(pickerValue, 'year')).toBe('2026');
    });

    it('should parse and format local times without date or offset', () => {
        expect(localTimeIsoToDateTime('09:30')?.toISO()).toBe('1970-01-01T09:30:00.000Z');
        expect(localTimeIsoToDateTime('09:30:15')?.toISO()).toBe('1970-01-01T09:30:15.000Z');
        expect(localTimeIsoToDateTime('24:00')).toBeNull();

        const pickerValue = DateTime.fromISO('2026-07-29T09:30:15', {zone: 'America/Los_Angeles'});
        expect(dateTimeToLocalTimeIso(pickerValue, 'minute')).toBe('09:30');
        expect(dateTimeToLocalTimeIso(pickerValue, 'second')).toBe('09:30:15');
    });

    it('should derive the current date in the application timezone', () => {
        vi.useFakeTimers();

        try {
            vi.setSystemTime(new Date('2026-07-29T22:30:00Z'));
            expect(getCurrentApplicationDate()).toBe('2026-07-30');
        } finally {
            vi.useRealTimers();
        }
    });
});
