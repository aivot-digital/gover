import {DateTime, IANAZone} from 'luxon';
import {
    IanaTimeZone,
    InstantIso,
    DateValueIso,
    LocalDateIso,
    LocalDateTimeIso,
    LocalTimeIso,
    YearIso,
    YearMonthIso,
} from './temporal-types';

const instantPattern = /^\d{4}-\d{2}-\d{2}T(?:[01]\d|2[0-3]):[0-5]\d:[0-5]\d(?:\.(\d{1,9}))?(?:Z|[+-]\d{2}:\d{2})$/;
const localDatePattern = /^\d{4}-\d{2}-\d{2}$/;
const yearMonthPattern = /^\d{4}-\d{2}$/;
const yearPattern = /^\d{4}$/;
const localTimePattern = /^(?:[01]\d|2[0-3]):[0-5]\d(?::[0-5]\d)?$/;
const localDateTimePattern = /^\d{4}-\d{2}-\d{2}T(?:[01]\d|2[0-3]):[0-5]\d:[0-5]\d$/;

export type TemporalPrecision = 'minute' | 'second';
export type CalendarDatePrecision = 'day' | 'month' | 'year';

export type LocalDateTimeResolution =
    | {
        resolved: true;
        dateTime: DateTime<true>;
        value: InstantIso;
    }
    | {
        resolved: false;
        reason: 'invalid' | 'nonexistent';
    };

export function parseIanaTimeZone(value: string): IanaTimeZone {
    if (value !== 'UTC' && !IANAZone.isValidZone(value)) {
        throw new Error(`Invalid IANA timezone: ${value}`);
    }

    return value as IanaTimeZone;
}

export function getApplicationTimeZone(): IanaTimeZone {
    return parseIanaTimeZone(AppConfig.applicationTimeZone);
}

export function getCurrentApplicationDate(): LocalDateIso {
    return DateTime
        .now()
        .setZone(getApplicationTimeZone())
        .toFormat('yyyy-MM-dd') as LocalDateIso;
}

export function isInstantIso(value: unknown): value is InstantIso {
    if (typeof value !== 'string' || !instantPattern.test(value)) {
        return false;
    }

    return DateTime.fromISO(value, {setZone: true}).isValid;
}

export function parseInstantIso(value: unknown): InstantIso {
    if (!isInstantIso(value)) {
        throw new Error(`Invalid ISO instant: ${String(value)}`);
    }

    return value;
}

export function isLocalDateIso(value: unknown): value is LocalDateIso {
    return typeof value === 'string'
        && localDatePattern.test(value)
        && DateTime.fromFormat(value, 'yyyy-MM-dd', {zone: 'UTC'}).isValid;
}

export function parseLocalDateIso(value: unknown): LocalDateIso {
    if (!isLocalDateIso(value)) {
        throw new Error(`Invalid local ISO date: ${String(value)}`);
    }

    return value;
}

export function isYearMonthIso(value: unknown): value is YearMonthIso {
    return typeof value === 'string'
        && yearMonthPattern.test(value)
        && DateTime.fromFormat(value, 'yyyy-MM', {zone: 'UTC'}).isValid;
}

export function isYearIso(value: unknown): value is YearIso {
    return typeof value === 'string' && yearPattern.test(value);
}

export function isLocalTimeIso(value: unknown): value is LocalTimeIso {
    return typeof value === 'string' && localTimePattern.test(value);
}

export function isLocalDateTimeIso(value: unknown): value is LocalDateTimeIso {
    if (typeof value !== 'string' || !localDateTimePattern.test(value)) {
        return false;
    }

    const parsed = DateTime.fromFormat(value, "yyyy-MM-dd'T'HH:mm:ss", {zone: 'UTC'});
    return parsed.isValid && parsed.toFormat("yyyy-MM-dd'T'HH:mm:ss") === value;
}

export function dateValueToDateTime(
    value: unknown,
    precision: CalendarDatePrecision,
): DateTime<true> | null {
    const format = precision === 'day'
        ? 'yyyy-MM-dd'
        : precision === 'month'
            ? 'yyyy-MM'
            : 'yyyy';
    const isValidValue = precision === 'day'
        ? isLocalDateIso(value)
        : precision === 'month'
            ? isYearMonthIso(value)
            : isYearIso(value);

    if (!isValidValue) {
        return null;
    }

    const parsed = DateTime.fromFormat(value as string, format, {zone: 'UTC'});
    return parsed.isValid ? parsed.startOf('day') : null;
}

export function dateTimeToDateValueIso(
    value: DateTime,
    precision: CalendarDatePrecision,
): DateValueIso | null {
    if (!value.isValid) {
        return null;
    }

    const format = precision === 'day'
        ? 'yyyy-MM-dd'
        : precision === 'month'
            ? 'yyyy-MM'
            : 'yyyy';

    return value.toFormat(format) as DateValueIso;
}

export function localTimeIsoToDateTime(value: unknown): DateTime<true> | null {
    if (!isLocalTimeIso(value)) {
        return null;
    }

    const [hour, minute, second = 0] = value.split(':').map(Number);
    // A floating time needs a date for MUI/Luxon, but neither date nor zone is part
    // of its domain value. A fixed UTC carrier prevents browser-zone and DST shifts.
    const parsed = DateTime.fromObject(
        {
            year: 1970,
            month: 1,
            day: 1,
            hour,
            minute,
            second,
            millisecond: 0,
        },
        {zone: 'UTC'},
    );

    return parsed.isValid ? parsed : null;
}

export function dateTimeToLocalTimeIso(
    value: DateTime,
    precision: TemporalPrecision,
): LocalTimeIso | null {
    if (!value.isValid) {
        return null;
    }

    return value.toFormat(precision === 'second' ? 'HH:mm:ss' : 'HH:mm') as LocalTimeIso;
}

export function instantToDateTime(
    value: InstantIso | string,
    timeZone: IanaTimeZone,
): DateTime<true> | null {
    if (!isInstantIso(value)) {
        return null;
    }

    const parsed = DateTime.fromISO(value, {setZone: true}).setZone(timeZone);
    return parsed.isValid ? parsed : null;
}

export function instantToApplicationDateTime(value: unknown): DateTime<true> | null {
    if (!isInstantIso(value)) {
        return null;
    }

    return instantToDateTime(value, getApplicationTimeZone());
}

export function formatInstantInApplicationTimeZone(
    value: unknown,
    format: string,
): string | null {
    const dateTime = instantToApplicationDateTime(value);
    return dateTime?.setLocale('de').toFormat(format) ?? null;
}

export function instantToEpochMillis(value: unknown): number | null {
    if (!isInstantIso(value)) {
        return null;
    }

    return DateTime.fromISO(value, {setZone: true}).toMillis();
}

export function compareInstantIso(left: unknown, right: unknown): number | null {
    const leftValue = instantComparisonValue(left);
    const rightValue = instantComparisonValue(right);

    if (leftValue == null || rightValue == null) {
        return null;
    }

    const epochMillisComparison = leftValue.epochMillis - rightValue.epochMillis;
    if (epochMillisComparison !== 0) {
        return epochMillisComparison;
    }

    return leftValue.subMillisecondNanoseconds - rightValue.subMillisecondNanoseconds;
}

function instantComparisonValue(value: unknown): {
    epochMillis: number;
    subMillisecondNanoseconds: number;
} | null {
    if (!isInstantIso(value)) {
        return null;
    }

    const parsed = DateTime.fromISO(value, {setZone: true});
    const fraction = instantPattern.exec(value)?.[1] ?? '';
    const nanoseconds = fraction.padEnd(9, '0');

    return {
        epochMillis: parsed.toMillis(),
        // Luxon preserves milliseconds. Keep the remaining digits separately so
        // condition and range comparisons honor the wire contract's nanosecond precision.
        subMillisecondNanoseconds: Number(nanoseconds.slice(3)),
    };
}

export function formatEpochMillisInApplicationTimeZone(
    value: number,
    format: string,
): string | null {
    if (!Number.isFinite(value)) {
        return null;
    }

    const dateTime = DateTime.fromMillis(value, {zone: getApplicationTimeZone()});
    return dateTime.isValid ? dateTime.setLocale('de').toFormat(format) : null;
}

export function formatRelativeEpochMillisInApplicationTimeZone(
    value: number,
    base: number = Date.now(),
): string | null {
    if (!Number.isFinite(value) || !Number.isFinite(base)) {
        return null;
    }

    const zone = getApplicationTimeZone();
    const dateTime = DateTime.fromMillis(value, {zone});
    const baseDateTime = DateTime.fromMillis(base, {zone});

    if (!dateTime.isValid || !baseDateTime.isValid) {
        return null;
    }

    return dateTime
        .setLocale('de')
        // Move exact half-unit values across the boundary so past and future values round symmetrically.
        .toRelative({base: baseDateTime, rounding: 'round', padding: 1});
}

export function formatRelativeInstantInApplicationTimeZone(
    value: unknown,
    base: number = Date.now(),
): string | null {
    const epochMillis = instantToEpochMillis(value);
    return epochMillis == null
        ? null
        : formatRelativeEpochMillisInApplicationTimeZone(epochMillis, base);
}

export function dateTimeToLocalDateTimeIso(
    value: DateTime,
    precision: TemporalPrecision,
): LocalDateTimeIso | null {
    if (!value.isValid) {
        return null;
    }

    const normalized = precision === 'second'
        ? value.set({millisecond: 0})
        : value.set({second: 0, millisecond: 0});

    return normalized.toFormat("yyyy-MM-dd'T'HH:mm:ss") as LocalDateTimeIso;
}

export function resolveLocalDateTime(
    value: LocalDateTimeIso,
    timeZone: IanaTimeZone,
): LocalDateTimeResolution {
    if (!isLocalDateTimeIso(value)) {
        return {
            resolved: false,
            reason: 'invalid',
        };
    }

    // UTC is only a neutral parsing zone here: localValue is used to validate and
    // extract wall-clock fields. The actual instant is resolved in timeZone below.
    const localValue = DateTime.fromFormat(value, "yyyy-MM-dd'T'HH:mm:ss", {zone: 'UTC'});
    const zonedValue = DateTime.fromObject(
        {
            year: localValue.year,
            month: localValue.month,
            day: localValue.day,
            hour: localValue.hour,
            minute: localValue.minute,
            second: localValue.second,
            millisecond: 0,
        },
        {
            zone: timeZone,
        },
    );

    if (!zonedValue.isValid) {
        return {
            resolved: false,
            reason: 'invalid',
        };
    }

    if (zonedValue.toFormat("yyyy-MM-dd'T'HH:mm:ss") !== value) {
        return {
            resolved: false,
            reason: 'nonexistent',
        };
    }

    const possibleValues = zonedValue.getPossibleOffsets();
    // Ambiguous wall times use the earlier occurrence, matching the backend policy.
    const earliestValue = possibleValues.reduce((earliest, candidate) => {
        return candidate.toMillis() < earliest.toMillis() ? candidate : earliest;
    });

    return {
        resolved: true,
        dateTime: earliestValue,
        // ZZ always emits a numeric offset, including +00:00, matching the API contract.
        value: earliestValue.toFormat("yyyy-MM-dd'T'HH:mm:ssZZ") as InstantIso,
    };
}

export function resolvePickerDateTime(
    value: DateTime,
    timeZone: IanaTimeZone,
    precision: TemporalPrecision,
): LocalDateTimeResolution {
    // MUI may retain the adapter's carrier zone. Preserve the displayed wall-clock
    // fields, then resolve those fields explicitly in the application timezone.
    const localValue = dateTimeToLocalDateTimeIso(value.setZone(timeZone, {keepLocalTime: true}), precision);

    if (localValue === null) {
        return {
            resolved: false,
            reason: 'invalid',
        };
    }

    return resolveLocalDateTime(localValue, timeZone);
}

export function canonicalizeInstant(
    value: InstantIso | string,
    timeZone: IanaTimeZone,
    precision: TemporalPrecision,
): InstantIso | null {
    const dateTime = instantToDateTime(value, timeZone);

    if (dateTime === null) {
        return null;
    }

    const normalized = precision === 'second'
        ? dateTime.set({millisecond: 0})
        : dateTime.set({second: 0, millisecond: 0});

    // Keep the application's numeric offset on the wire instead of normalizing UTC to Z.
    return normalized.toFormat("yyyy-MM-dd'T'HH:mm:ssZZ") as InstantIso;
}
