import {DurationLargestUnit, DurationPrecision} from '../../models/elements/form/input/duration-field-element';

export const MAX_DURATION_SECONDS = Number.MAX_SAFE_INTEGER;

export function formatDurationSeconds(
    value: number | null | undefined,
    precision = DurationPrecision.Second,
    largestUnit: DurationLargestUnit | null = DurationLargestUnit.Hour,
): string {
    if (value == null) return 'Keine Angabe';
    if (!Number.isSafeInteger(value) || value < 0) return 'Ungültige Zeitdauer';
    const days = largestUnit === DurationLargestUnit.Day ? Math.floor(value / 86400) : 0;
    const hours = Math.floor((largestUnit === DurationLargestUnit.Day ? value % 86400 : value) / 3600);
    const minutes = Math.floor(value % 3600 / 60);
    const seconds = value % 60;
    const parts = [
        days > 0 ? `${days.toLocaleString('de-DE')} ${days === 1 ? 'Tag' : 'Tage'}` : null,
        hours > 0 ? `${hours.toLocaleString('de-DE')} ${hours === 1 ? 'Stunde' : 'Stunden'}` : null,
        minutes > 0 ? `${minutes} ${minutes === 1 ? 'Minute' : 'Minuten'}` : null,
        seconds > 0 ? `${seconds} ${seconds === 1 ? 'Sekunde' : 'Sekunden'}` : null,
    ].filter(Boolean);
    return parts.join(' ') || (precision === DurationPrecision.Minute ? '0 Minuten' : '0 Sekunden');
}

export function validateDurationSeconds(
    value: unknown,
    precision = DurationPrecision.Minute,
    minDuration?: number | null,
    maxDuration?: number | null,
    largestUnit?: DurationLargestUnit | null,
): string | undefined {
    if (value == null) return undefined;
    if (typeof value !== 'number' || !Number.isSafeInteger(value) || value < 0) {
        return 'Bitte geben Sie eine Zeitdauer in ganzen, nicht negativen Sekunden innerhalb des unterstützten Zahlenbereichs ein.';
    }
    if (precision === DurationPrecision.Minute && value % 60 !== 0) {
        return 'Bitte geben Sie eine Zeitdauer in ganzen Minuten ein.';
    }
    if (minDuration != null && value < minDuration) {
        return `Die Zeitdauer muss mindestens ${formatDurationSeconds(minDuration, precision, largestUnit)} betragen.`;
    }
    if (maxDuration != null && value > maxDuration) {
        return `Die Zeitdauer darf höchstens ${formatDurationSeconds(maxDuration, precision, largestUnit)} betragen.`;
    }
    return undefined;
}
