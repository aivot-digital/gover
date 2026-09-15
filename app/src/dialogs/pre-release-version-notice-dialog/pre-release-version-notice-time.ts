import {DateTime} from 'luxon';

export function getPreReleaseNoticeDismissalExpiry(
    now: DateTime = DateTime.local(),
): string {
    return now
        .plus({days: 1})
        .startOf('day')
        .set({hour: 1})
        .toJSDate()
        .toISOString();
}

export function isPreReleaseNoticeDismissalActive(
    value: string | null,
    now: DateTime = DateTime.now(),
): boolean {
    if (value == null) {
        return false;
    }

    const expiry = DateTime.fromISO(value, {setZone: true});
    return expiry.isValid && now.toMillis() < expiry.toMillis();
}
