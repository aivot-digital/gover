import {
    getApplicationTimeZone,
    instantToDateTime,
    isLocalDateIso,
    isLocalTimeIso,
    isYearIso,
    isYearMonthIso,
} from '../../../utils/temporal-utils';
import {NoCodeDataType, NoCodeDataTypeLabels} from '../../../data/no-code-data-type';
import {NoCodeDateInput, NoCodeOperatorDetailsDTO, NoCodeParameter, resolveNoCodeSignature} from '../../../models/dtos/no-code-operator-details-dto';
import {ElementWithParents} from '../../../utils/flatten-elements';
import {isStringNullOrEmpty} from '../../../utils/string-utils';
import {NoCodeDataTypeMap} from '../data/no-code-data-type-map';
import {NoCodeEditorNode} from './no-code-editor-store';

const T = NoCodeDataType;

/**
 * Mirrors the casts of the backend NoCodeOperator (castToBoolean, castToNumber, castToDate, …). Values of these source
 * types cannot be converted to the target type and stop the evaluation. Text, Runtime parameters and Runtime values
 * never conflict, because only their content decides. DateTime to Time stays allowed: times are read from timestamps.
 */
const INCOMPATIBLE_SOURCES: Partial<Record<NoCodeDataType, NoCodeDataType[]>> = {
    [T.Boolean]: [T.Number, T.Date, T.DateTime, T.Time, T.List, T.Object],
    [T.Number]: [T.Boolean, T.Date, T.DateTime, T.Time, T.List, T.Object],
    [T.Date]: [T.Boolean, T.Number, T.Time, T.List, T.Object],
    [T.DateTime]: [T.Boolean, T.Number, T.Date, T.Time, T.List, T.Object],
    [T.Time]: [T.Boolean, T.Number, T.Date, T.List, T.Object],
    [T.List]: [T.Boolean, T.Number, T.Date, T.DateTime, T.Time, T.Object],
    [T.Object]: [T.Boolean, T.Number, T.Date, T.DateTime, T.Time, T.List],
};

// Text formats the backend parses, see NoCodeOperator and IsoTimestampUtils.
const NUMBER_TEXT = /^[+-]?(\d+(\.\d*)?|\.\d+)([eE][+-]?\d+)?$/;
const DECIMAL_COMMA_TEXT = /^[+-]?\d+,\d+$/;
const BOOLEAN_TEXT = /^(true|false|wahr|falsch)$/i;

export function getNoCodeEditorNodeType(
    node: NoCodeEditorNode,
    allElements: ElementWithParents[],
    allOperators: NoCodeOperatorDetailsDTO[],
): NoCodeDataType {
    switch (node.type) {
        case 'NoCodeExpression': {
            const operator = allOperators.find((op) => op.identifier === node.operatorIdentifier);
            return operator == null
                ? T.Runtime
                : resolveNoCodeSignature(operator, {operandCount: node.operands.length || undefined}).returnType;
        }
        case 'NoCodeReference': {
            const element = allElements.find((entry) => entry.element.id === node.elementId)?.element;
            return element == null ? T.Runtime : NoCodeDataTypeMap[element.type] ?? T.Runtime;
        }
        case 'NoCodeStaticValue':
            return T.String;
        default:
            return T.Runtime;
    }
}

/**
 * Returns a message when the node's value cannot be converted to the expected type, or null when it fits.
 */
export function getNoCodeEditorTypeConflict(
    node: NoCodeEditorNode,
    parameter: Pick<NoCodeParameter, 'type' | 'dateInput'>,
    allElements: ElementWithParents[],
    allOperators: NoCodeOperatorDetailsDTO[],
): string | null {
    const expectedType = parameter.type;
    const strictCalendar = parameter.dateInput === NoCodeDateInput.Calendar;
    const expected = NoCodeDataTypeLabels[expectedType];

    if (node.type === 'NoCodeStaticValue') {
        return isStringNullOrEmpty(node.value) || staticValueFits(node.value!, expectedType, strictCalendar)
            ? null
            : `Typkonflikt: Erwartet wird „${expected}“, „${node.value}“ lässt sich nicht umwandeln.`;
    }

    const actualType = getNoCodeEditorNodeType(node, allElements, allOperators);
    return (strictCalendar && actualType === T.DateTime) || INCOMPATIBLE_SOURCES[expectedType]?.includes(actualType)
        ? `Typkonflikt: Erwartet wird „${expected}“, geliefert wird „${NoCodeDataTypeLabels[actualType]}“.`
        : null;
}

function staticValueFits(value: string, expectedType: NoCodeDataType, strictCalendar: boolean): boolean {
    switch (expectedType) {
        case T.Boolean:
            return BOOLEAN_TEXT.test(value.trim());
        case T.Number:
            return NUMBER_TEXT.test(value.trim()) || DECIMAL_COMMA_TEXT.test(value.trim());
        case T.Date:
            return isLocalDateIso(value) || isYearMonthIso(value) || isYearIso(value) || (!strictCalendar && dateTimeFits(value));
        case T.DateTime:
            return dateTimeFits(value);
        case T.Time:
            return isLocalTimeIso(value) || dateTimeFits(value);
        case T.List:
            return Array.isArray(parseJson(value));
        case T.Object: {
            const parsed = parseJson(value);
            return parsed != null && typeof parsed === 'object' && !Array.isArray(parsed);
        }
        default:
            return true;
    }
}

function parseJson(value: string): unknown {
    try {
        return JSON.parse(value);
    } catch {
        return undefined;
    }
}

function dateTimeFits(value: string): boolean {
    return instantToDateTime(value, getApplicationTimeZone(), true) != null;
}
