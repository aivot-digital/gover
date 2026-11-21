import {NoCodeDataType} from '../../../data/no-code-data-type';
import {NoCodeParameterOption} from '../../../models/dtos/no-code-operator-details-dto';

/** Formatting describes authored values; only an explicit parameter type permits displaying a string as typed. */
export function formatNoCodeValue(value: unknown, type = NoCodeDataType.Runtime, options: NoCodeParameterOption[] = [], compact = false): string {
    if (value == null) return 'Leerer Wert';
    if (value === '') return 'Leerer Text';
    const option = options.find((candidate) => candidate.value === String(value));
    if (option != null) return compact ? option.label : `„${option.label}“ (${String(value)})`;
    if (typeof value === 'boolean' || type === NoCodeDataType.Boolean && (value === 'true' || value === 'false')) {
        return value === true || value === 'true' ? 'Wahr' : 'Falsch';
    }
    if (typeof value === 'number' || type === NoCodeDataType.Number && typeof value === 'string' && /^[-+]?\d+(?:[.,]\d+)?(?:[eE][-+]?\d+)?$/.test(value)) return String(value);
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
