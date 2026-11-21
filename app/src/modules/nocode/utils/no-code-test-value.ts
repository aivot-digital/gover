import {NoCodeDataType} from '../../../data/no-code-data-type';
import {
    isInstantIso,
    isLocalDateIso,
    isYearIso,
    isYearMonthIso,
    localTimeIsoToDateTime,
} from '../../../utils/temporal-utils';

export interface ParsedNoCodeTestValue {
    value?: unknown;
    error?: string;
}

/** Empty input is absent. JSON can explicitly represent null or an empty string. Invalid text never falls back silently. */
export function parseNoCodeTestValue(text: string | undefined, type: NoCodeDataType, json = false): ParsedNoCodeTestValue {
    if (text == null || text === '') return {};
    let value: unknown = text;
    if (json || type === NoCodeDataType.List || type === NoCodeDataType.Object) {
        try {
            value = JSON.parse(text);
        } catch {
            return {error: 'Geben Sie gültiges JSON ein.'};
        }
        if (value == null) return {value: null};
    } else if (type === NoCodeDataType.Number) {
        if (!/^[-+]?(?:\d+(?:[.,]\d*)?|[.,]\d+)(?:[eE][-+]?\d+)?$/.test(text.trim())) return {error: 'Geben Sie eine Zahl ein, zum Beispiel 42 oder 3,5.'};
        value = Number(text.trim().replace(',', '.'));
    } else if (type === NoCodeDataType.Boolean) {
        value = text === 'true' || text === 'wahr' ? true : text === 'false' || text === 'falsch' ? false : text;
    }
    const valid = type === NoCodeDataType.Runtime ||
        type === NoCodeDataType.Boolean && typeof value === 'boolean' ||
        type === NoCodeDataType.Number && typeof value === 'number' && Number.isFinite(value) ||
        type === NoCodeDataType.String && typeof value === 'string' ||
        type === NoCodeDataType.List && Array.isArray(value) ||
        type === NoCodeDataType.Object && typeof value === 'object' && !Array.isArray(value) ||
        type === NoCodeDataType.Date && (isLocalDateIso(value) || isYearMonthIso(value) || isYearIso(value)) ||
        type === NoCodeDataType.Time && localTimeIsoToDateTime(value) != null ||
        type === NoCodeDataType.DateTime && isInstantIso(value);
    return valid ? {value} : {error: 'Der Testwert passt nicht zum angegebenen Datentyp. Prüfen Sie das Format.'};
}
