import {describe, expect, it} from 'vitest';
import {DateTime} from 'luxon';
import {GoverAdapterLuxon, NONEXISTENT_LOCAL_DATETIME_REASON} from './gover-adapter-luxon';

describe('GoverAdapterLuxon', () => {
    const adapter = new GoverAdapterLuxon({locale: 'de'});

    it('should reject selecting an hour in the spring DST gap', () => {
        const value = DateTime.fromISO('2026-03-29T01:30:00', {zone: 'Europe/Berlin'});
        const updated = adapter.setHours(value, 2);

        expect(updated.isValid).toBe(false);
        expect(updated.invalidReason).toBe(NONEXISTENT_LOCAL_DATETIME_REASON);
    });

    it('should reject moving an existing local time onto a date where it does not exist', () => {
        const value = DateTime.fromISO('2026-03-28T02:30:00', {zone: 'Europe/Berlin'});
        const updated = adapter.setDate(value, 29);

        expect(updated.isValid).toBe(false);
        expect(updated.invalidReason).toBe(NONEXISTENT_LOCAL_DATETIME_REASON);
    });

    it('should retain regular local time changes', () => {
        const value = DateTime.fromISO('2026-03-29T01:30:00', {zone: 'Europe/Berlin'});
        const updated = adapter.setHours(value, 3);

        expect(updated.isValid).toBe(true);
        expect(updated.toISO()).toBe('2026-03-29T03:30:00.000+02:00');
    });
});
