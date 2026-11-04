import {NoCodeDataType, NoCodeDataTypeLabels} from '../../../data/no-code-data-type';
import {NoCodeOperatorDetailsDTO, resolveNoCodeSignature} from '../../../models/dtos/no-code-operator-details-dto';
import {ElementWithParents} from '../../../utils/flatten-elements';
import {isStringNullOrEmpty} from '../../../utils/string-utils';
import {NoCodeDataTypeMap} from '../data/no-code-data-type-map';
import {PillNoCodeNode} from './pill-no-code-store';

const T = NoCodeDataType;

/**
 * Mirrors the casts of the backend NoCodeOperator (castToBoolean, castToNumber, castToDate, …). The backend converts
 * almost every value, but for these source types the cast to the target type ignores the value and falls back to a
 * default such as 0, an empty list or today. Text, Runtime parameters and Runtime values never conflict.
 * DateTime to Time stays allowed: temporal operator results convert, only text from fields would fall back.
 */
const INCOMPATIBLE_SOURCES: Partial<Record<NoCodeDataType, NoCodeDataType[]>> = {
    [T.Boolean]: [T.Date, T.DateTime, T.Time],
    [T.Number]: [T.Boolean],
    [T.Date]: [T.Boolean, T.Number, T.Time, T.List, T.Object],
    [T.DateTime]: [T.Boolean, T.Number, T.Date, T.Time, T.List, T.Object],
    [T.Time]: [T.Boolean, T.Number, T.Date, T.List, T.Object],
    [T.List]: [T.Boolean, T.Number, T.Date, T.DateTime, T.Time, T.Object],
    [T.Object]: [T.Boolean, T.Number, T.Date, T.DateTime, T.Time, T.List],
};

// Text formats the backend parses, see NoCodeOperator and IsoTimestampUtils.
const NUMBER_TEXT = /^[+-]?(\d+(\.\d*)?|\.\d+)([eE][+-]?\d+)?$/;
const DATE_TEXT = /^\d{4}(-\d{2}(-\d{2})?)?$/;
const DATE_TIME_TEXT = /^\d{4}-\d{2}-\d{2}T([01]\d|2[0-3]):[0-5]\d:[0-5]\d(\.\d{1,9})?(Z|[+-]\d{2}:\d{2})?$/;
const TIME_TEXT = /^\d{2}:\d{2}(:\d{2})?$/;

export function getPillNoCodeNodeType(
    node: PillNoCodeNode,
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
export function getPillNoCodeTypeConflict(
    node: PillNoCodeNode,
    expectedType: NoCodeDataType,
    allElements: ElementWithParents[],
    allOperators: NoCodeOperatorDetailsDTO[],
): string | null {
    const expected = NoCodeDataTypeLabels[expectedType];

    if (node.type === 'NoCodeStaticValue') {
        return isStringNullOrEmpty(node.value) || staticValueFits(node.value!, expectedType)
            ? null
            : `Typkonflikt: Erwartet wird „${expected}“, „${node.value}“ lässt sich nicht umwandeln.`;
    }

    const actualType = getPillNoCodeNodeType(node, allElements, allOperators);
    return INCOMPATIBLE_SOURCES[expectedType]?.includes(actualType)
        ? `Typkonflikt: Erwartet wird „${expected}“, geliefert wird „${NoCodeDataTypeLabels[actualType]}“.`
        : null;
}

function staticValueFits(value: string, expectedType: NoCodeDataType): boolean {
    switch (expectedType) {
        case T.Number:
            // castToNumber also turns dates and times into numbers; other text would become its length.
            return NUMBER_TEXT.test(value) || DATE_TEXT.test(value) || DATE_TIME_TEXT.test(value) || TIME_TEXT.test(value);
        case T.Date:
            return DATE_TEXT.test(value) || DATE_TIME_TEXT.test(value);
        case T.DateTime:
            return DATE_TIME_TEXT.test(value);
        case T.Time:
            return TIME_TEXT.test(value);
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
