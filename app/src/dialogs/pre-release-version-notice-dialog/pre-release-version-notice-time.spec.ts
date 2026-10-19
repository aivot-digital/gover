import {DateTime} from 'luxon';
import {describe, expect, it} from 'vitest';
import {
    getPreReleaseNoticeDismissalExpiry,
    isPreReleaseNoticeDismissalActive,
} from './pre-release-version-notice-time';

describe('pre-release-version-notice-time', () => {
    it('should expire at 01:00 on the next local day across DST changes', () => {
        expect(getPreReleaseNoticeDismissalExpiry(
            DateTime.fromISO('2026-03-28T12:00:00', {zone: 'Europe/Berlin'}),
        )).toBe('2026-03-29T00:00:00.000Z');
        expect(getPreReleaseNoticeDismissalExpiry(
            DateTime.fromISO('2026-10-24T12:00:00', {zone: 'Europe/Berlin'}),
        )).toBe('2026-10-24T23:00:00.000Z');
    });

    it('should only accept valid dismissal expiries in the future', () => {
        const now = DateTime.fromISO('2026-08-07T10:00:00Z', {setZone: true});

        expect(isPreReleaseNoticeDismissalActive('2026-08-07T10:00:01Z', now)).toBe(true);
        expect(isPreReleaseNoticeDismissalActive('2026-08-07T10:00:00Z', now)).toBe(false);
        expect(isPreReleaseNoticeDismissalActive('invalid', now)).toBe(false);
        expect(isPreReleaseNoticeDismissalActive(null, now)).toBe(false);
    });
});
