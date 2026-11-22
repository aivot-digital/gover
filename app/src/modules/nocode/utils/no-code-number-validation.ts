import {NoCodeNumberConstraints} from '../../../models/dtos/no-code-operator-details-dto';

/** Validate authored literals only; values supplied by references or functions are checked when evaluated. */
export function getNoCodeNumberValidationError(
    value: unknown,
    label: string,
    constraints?: NoCodeNumberConstraints | null,
): string | undefined {
    if (constraints == null) return undefined;

    const source = typeof value === 'string' || typeof value === 'number' ? String(value).trim() : '';
    const numeric = Number(source);
    if (!isNoCodeDecimalLiteral(source) || constraints.integerOnly && !isWholeDecimal(source)) {
        return constraints.integerOnly
            ? `Geben Sie für „${label}“ eine ganze Zahl ein.`
            : `Geben Sie für „${label}“ eine Zahl ein.`;
    }

    const {minimum, maximum} = constraints;
    if (minimum != null && numeric < minimum || maximum != null && numeric > maximum) {
        if (minimum != null && maximum != null) {
            return `Geben Sie für „${label}“ eine Zahl zwischen ${formatBound(minimum)} und ${formatBound(maximum)} ein.`;
        }
        if (minimum != null) return `Geben Sie für „${label}“ eine Zahl ab ${formatBound(minimum)} ein.`;
        return `Geben Sie für „${label}“ eine Zahl bis ${formatBound(maximum!)} ein.`;
    }

    if (!Number.isFinite(numeric)) return `Geben Sie für „${label}“ eine Zahl ein.`;
    return undefined;
}

/** Decimal syntax accepted by the evaluator, without interpreting or rounding the value. */
export function isNoCodeDecimalLiteral(source: string): boolean {
    return /^[+-]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][+-]?\d+)?$/.test(source);
}

function isWholeDecimal(source: string): boolean {
    const [coefficient, exponentText = '0'] = source.replace(/^[+-]/, '').split(/[eE]/);
    const exponent = Number(exponentText);
    const [whole, fraction = ''] = coefficient.split('.');
    const scale = fraction.length - exponent;
    // BigDecimal uses a signed 32-bit scale. Inspect decimal digits before converting
    // to Number: JS rounding would otherwise accept 1.00000000000000000001 as 1.
    if (!Number.isInteger(exponent) || exponent < -2147483648 || exponent > 2147483647
        || scale < -2147483648 || scale > 2147483647) return false;
    if (scale <= 0) return true;
    const digits = whole + fraction;
    return !/[1-9]/.test(digits.slice(Math.max(0, digits.length - scale)));
}

function formatBound(value: number): string {
    return value.toLocaleString('de-DE', {maximumFractionDigits: 20}).replace('-', '−');
}
