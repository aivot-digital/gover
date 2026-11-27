import {getApplicationTimeZone, instantToDateTime} from '../../../utils/temporal-utils';

/** Format authored date-time values without discarding precision or merging repeated local clock times. */
export function formatNoCodeDateTimeValue(value: unknown): string | null {
    if (typeof value !== 'string') return null;
    const dateTime = instantToDateTime(value, getApplicationTimeZone(), true);
    if (dateTime == null || !dateTime.isValid || dateTime.year < 0 || dateTime.year > 9999) return null;

    const occurrences = dateTime.getPossibleOffsets().sort((left, right) => left.toMillis() - right.toMillis());
    const occurrenceIndex = occurrences.findIndex(candidate => candidate.offset === dateTime.offset);
    const occurrenceLabel = occurrences.length > 1 && occurrenceIndex >= 0
        ? ` (${occurrenceIndex === 0 ? 'erstes' : 'zweites'} Vorkommen)`
        : '';

    // Luxon retains milliseconds only. Keep the authored fractional digits separate
    // so distinct instants remain distinguishable even below millisecond precision.
    const fraction = /\.(\d+)/.exec(value)?.[1];
    const fractionalSeconds = fraction == null ? '' : `,${fraction}`;
    return `${dateTime.setLocale('de').toFormat('dd.MM.yyyy HH:mm:ss')}${fractionalSeconds} Uhr${occurrenceLabel}`;
}
