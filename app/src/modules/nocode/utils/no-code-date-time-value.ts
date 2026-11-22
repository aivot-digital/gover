import {type DateTime} from 'luxon';
import {
    getApplicationTimeZone,
    instantToDateTime,
    isLocalDateTimeIso,
    resolveLocalDateTime,
} from '../../../utils/temporal-utils';

const dateTimeLiteralPattern = /^(\d{4}-\d{2}-\d{2}T(?:[01]\d|2[0-3]):[0-5]\d:[0-5]\d)(?:\.(\d{1,9}))?(Z|[+-]\d{2}:\d{2})?$/;

/** Format authored date-time values without discarding precision or merging repeated local clock times. */
export function formatNoCodeDateTimeValue(value: unknown): string | null {
    if (typeof value !== 'string') return null;
    const match = dateTimeLiteralPattern.exec(value);
    if (match == null) return null;
    const [, localSeconds, fraction, offset] = match;
    if (!isLocalDateTimeIso(localSeconds) || offset != null && !isSupportedOffset(offset)) return null;

    const timeZone = getApplicationTimeZone();
    let dateTime: DateTime | null;
    if (offset != null) {
        dateTime = instantToDateTime(value, timeZone);
    } else {
        const resolved = resolveLocalDateTime(localSeconds, timeZone);
        if (!resolved.resolved) return null;
        dateTime = resolved.dateTime;
    }
    if (dateTime == null || !dateTime.isValid || dateTime.year < 0 || dateTime.year > 9999) return null;

    const occurrences = dateTime.getPossibleOffsets().sort((left, right) => left.toMillis() - right.toMillis());
    const occurrenceIndex = occurrences.findIndex(candidate => candidate.offset === dateTime.offset);
    const occurrenceLabel = occurrences.length > 1 && occurrenceIndex >= 0
        ? ` (${occurrenceIndex === 0 ? 'erstes' : 'zweites'} Vorkommen)`
        : '';

    // Luxon retains milliseconds only. Keep the authored fractional digits separate
    // so distinct instants remain distinguishable even below millisecond precision.
    const fractionalSeconds = fraction == null ? '' : `,${fraction}`;
    return `${dateTime.setLocale('de').toFormat('dd.MM.yyyy HH:mm:ss')}${fractionalSeconds} Uhr${occurrenceLabel}`;
}

function isSupportedOffset(offset: string): boolean {
    if (offset === 'Z') return true;
    const hours = Number(offset.slice(1, 3));
    const minutes = Number(offset.slice(4, 6));
    // Luxon accepts and normalizes offsets that OffsetDateTime rejects in the evaluator.
    return minutes < 60 && (hours < 18 || hours === 18 && minutes === 0);
}
