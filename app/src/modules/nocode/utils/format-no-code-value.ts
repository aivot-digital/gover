import {NoCodeDataType} from '../../../data/no-code-data-type';
import {NoCodeParameterOption} from '../../../models/dtos/no-code-operator-details-dto';
import {dateValueToDateTime, isYearIso, localTimeIsoToDateTime} from '../../../utils/temporal-utils';
import {isNoCodeDecimalLiteral} from './no-code-number-validation';
import {formatNoCodeDateTimeValue} from './no-code-date-time-value';

/** Only an explicit parameter or result type permits displaying a string as a temporal value. */
export function formatNoCodeValue(value: unknown, type = NoCodeDataType.Runtime, options: NoCodeParameterOption[] = [], compact = false): string {
    if (value == null) return 'Leerer Wert';
    if (value === '') return 'Leerer Text';
    const option = options.find((candidate) => candidate.value === String(value));
    if (option != null) return compact ? option.label : `„${option.label}“ (${String(value)})`;
    if (type === NoCodeDataType.Date && typeof value === 'string') {
        const date = dateValueToDateTime(value, 'day');
        if (date != null) return date.toFormat('dd.MM.yyyy');
        const month = dateValueToDateTime(value, 'month');
        if (month != null) return month.toFormat('MM.yyyy');
        if (isYearIso(value)) return value;
    }
    if (type === NoCodeDataType.DateTime) {
        const dateTime = formatNoCodeDateTimeValue(value);
        if (dateTime != null) return dateTime;
    }
    if (type === NoCodeDataType.Time) {
        const time = localTimeIsoToDateTime(value);
        if (time != null) return time.toFormat("HH:mm:ss 'Uhr'");
    }
    if (typeof value === 'boolean' || type === NoCodeDataType.Boolean && (value === 'true' || value === 'false')) {
        return value === true || value === 'true' ? 'Wahr' : 'Falsch';
    }
    if (typeof value === 'number') return String(value);
    if (type === NoCodeDataType.Number && typeof value === 'string' && isNoCodeDecimalLiteral(value.trim())) return value.trim();
    if (typeof value === 'object') return JSON.stringify(value);
    return compact ? String(value) : `„${String(value)}“`;
}

export function noCodeResultType(value: unknown): string {
    if (value == null) return 'Leerer Wert';
    if (Array.isArray(value)) return 'Liste';
    return ({
        string: 'Textwert',
        boolean: 'Wahrheitswert',
        number: 'Zahlenwert',
        object: 'Objekt',
    } as Record<string, string>)[typeof value] ?? 'Unbekannter Typ';
}
