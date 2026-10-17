export type DynamicTextTokenKind = 'output' | 'directive' | 'comment' | 'raw';

interface DynamicTextTokenDelimiter {
    close: string;
    kind: DynamicTextTokenKind;
    open: string;
}

export interface DynamicTextTokenMatch {
    end: number;
    kind: DynamicTextTokenKind;
    start: number;
}

export interface DynamicTextSegment {
    kind?: DynamicTextTokenKind;
    type: 'plain' | 'token';
    value: string;
}

export type DynamicTextSyntaxRole =
    | 'plain'
    | 'comment'
    | 'constant'
    | 'function'
    | 'keyword'
    | 'number'
    | 'property'
    | 'punctuation'
    | 'string';

export interface DynamicTextSyntaxSegment {
    role: DynamicTextSyntaxRole;
    value: string;
}

const DYNAMIC_TEXT_TOKEN_DELIMITERS: Record<DynamicTextTokenKind, DynamicTextTokenDelimiter> = {
    output: {
        close: '}}',
        kind: 'output',
        open: '{{',
    },
    directive: {
        close: '%}',
        kind: 'directive',
        open: '{%',
    },
    comment: {
        close: '#}',
        kind: 'comment',
        open: '{#',
    },
    raw: {
        close: '!}',
        kind: 'raw',
        open: '{!',
    },
};

const EXPRESSION_KEYWORDS = new Set([
    'and',
    'as',
    'block',
    'else',
    'endblock',
    'endfor',
    'endif',
    'for',
    'if',
    'in',
    'is',
    'not',
    'or',
    'useblock',
]);
const EXPRESSION_CONSTANTS = new Set(['empty', 'false', 'null', 'true']);
const TWO_CHARACTER_OPERATORS = new Set(['&&', '||', '==', '!=', '>=', '<=', '??', '?.']);
const OPERATOR_CHARACTERS = new Set(['+', '-', '*', '/', '%', '>', '<', '=', '!', '?', ':', ',', '(', ')', '[', ']']);

function appendSyntaxSegment(
    segments: DynamicTextSyntaxSegment[],
    role: DynamicTextSyntaxRole,
    value: string,
) {
    if (value.length === 0) {
        return;
    }

    const previousSegment = segments[segments.length - 1];
    if (previousSegment?.role === role) {
        previousSegment.value += value;
        return;
    }

    segments.push({role, value});
}

function isIdentifierStart(value: string): boolean {
    return /[A-Za-z_$]/.test(value);
}

function isIdentifierCharacter(value: string): boolean {
    return /[A-Za-z0-9_$]/.test(value);
}

function tokenizeExpression(value: string): DynamicTextSyntaxSegment[] {
    // TODO(input-modes): Verify this lightweight highlighter against a shared backend/frontend corpus. Runtime
    // validation must remain authoritative when the expression grammar grows.
    const segments: DynamicTextSyntaxSegment[] = [];
    let index = 0;

    while (index < value.length) {
        const character = value[index];

        if (/\s/.test(character)) {
            const start = index++;
            while (index < value.length && /\s/.test(value[index])) {
                index++;
            }
            appendSyntaxSegment(segments, 'plain', value.slice(start, index));
            continue;
        }

        if (character === '"' || character === "'") {
            const quote = character;
            const start = index++;
            while (index < value.length) {
                if (value[index] === '\\') {
                    index += Math.min(2, value.length - index);
                    continue;
                }
                if (value[index++] === quote) {
                    break;
                }
            }
            appendSyntaxSegment(segments, 'string', value.slice(start, index));
            continue;
        }

        const isVariablePath = value.startsWith('$.', index) || value.startsWith('$$.', index) ||
            value.startsWith('_.', index);
        if (isVariablePath) {
            const start = index;
            index += value.startsWith('$$.', index) ? 3 : 2;
            while (index < value.length && /[A-Za-z0-9_$.[\]"'-]/.test(value[index])) {
                index++;
            }
            appendSyntaxSegment(segments, 'property', value.slice(start, index));
            continue;
        }

        if (/\d/.test(character)) {
            const start = index++;
            while (index < value.length && /[\d.]/.test(value[index])) {
                index++;
            }
            appendSyntaxSegment(segments, 'number', value.slice(start, index));
            continue;
        }

        if (isIdentifierStart(character)) {
            const start = index++;
            while (index < value.length && isIdentifierCharacter(value[index])) {
                index++;
            }
            const identifier = value.slice(start, index);
            const normalizedIdentifier = identifier.toLocaleLowerCase('en');
            const nextNonWhitespaceCharacter = value.slice(index).trimStart().charAt(0);
            const role = EXPRESSION_KEYWORDS.has(normalizedIdentifier)
                ? 'keyword'
                : EXPRESSION_CONSTANTS.has(normalizedIdentifier)
                    ? 'constant'
                    : nextNonWhitespaceCharacter === '('
                        ? 'function'
                        : 'plain';
            appendSyntaxSegment(segments, role, identifier);
            continue;
        }

        const twoCharacterOperator = value.slice(index, index + 2);
        if (TWO_CHARACTER_OPERATORS.has(twoCharacterOperator)) {
            appendSyntaxSegment(segments, 'punctuation', twoCharacterOperator);
            index += 2;
            continue;
        }
        if (OPERATOR_CHARACTERS.has(character)) {
            appendSyntaxSegment(segments, 'punctuation', character);
            index++;
            continue;
        }

        appendSyntaxSegment(segments, 'plain', character);
        index++;
    }

    return segments;
}

export function getDynamicTextSyntaxSegments(value: string): DynamicTextSyntaxSegment[] {
    const kind = getDynamicTextTokenKind(value);
    if (kind == null || findDynamicTextTokenEnd(value, 0) !== value.length) {
        return [{role: 'plain', value}];
    }

    const delimiter = DYNAMIC_TEXT_TOKEN_DELIMITERS[kind];
    const innerValue = value.slice(delimiter.open.length, -delimiter.close.length);
    const segments: DynamicTextSyntaxSegment[] = [{role: 'punctuation', value: delimiter.open}];

    if (kind === 'comment') {
        appendSyntaxSegment(segments, 'comment', innerValue);
    } else {
        for (const segment of tokenizeExpression(innerValue)) {
            appendSyntaxSegment(segments, segment.role, segment.value);
        }
    }
    appendSyntaxSegment(segments, 'punctuation', delimiter.close);
    return segments;
}

export function getDynamicTextTokenKind(value: string, index = 0): DynamicTextTokenKind | null {
    if (value[index] !== '{') {
        return null;
    }

    switch (value[index + 1]) {
        case '{':
            return 'output';
        case '%':
            return 'directive';
        case '#':
            return 'comment';
        case '!':
            return 'raw';
        default:
            return null;
    }
}

function findDynamicTextTokenEnd(value: string, startIndex: number): number | null {
    const rootKind = getDynamicTextTokenKind(value, startIndex);
    if (rootKind == null) {
        return null;
    }

    const stack: DynamicTextTokenKind[] = [rootKind];
    let index = startIndex + 2;

    while (index < value.length) {
        const nestedKind = getDynamicTextTokenKind(value, index);
        if (nestedKind != null) {
            stack.push(nestedKind);
            index += 2;
            continue;
        }

        const currentDelimiter = DYNAMIC_TEXT_TOKEN_DELIMITERS[stack[stack.length - 1]];
        if (value.startsWith(currentDelimiter.close, index)) {
            stack.pop();
            index += currentDelimiter.close.length;
            if (stack.length === 0) {
                return index;
            }
            continue;
        }

        index += 1;
    }

    return null;
}

export function findDynamicTextTokenMatch(value: string): DynamicTextTokenMatch | null {
    for (let index = 0; index < value.length; index += 1) {
        const kind = getDynamicTextTokenKind(value, index);
        if (kind == null) {
            continue;
        }

        const end = findDynamicTextTokenEnd(value, index);
        if (end != null) {
            return {start: index, end, kind};
        }
    }

    return null;
}

export function splitDynamicTextSegments(value: string): DynamicTextSegment[] {
    const segments: DynamicTextSegment[] = [];
    let remainingValue = value;

    while (remainingValue.length > 0) {
        const match = findDynamicTextTokenMatch(remainingValue);
        if (match == null) {
            segments.push({type: 'plain', value: remainingValue});
            break;
        }

        if (match.start > 0) {
            segments.push({type: 'plain', value: remainingValue.slice(0, match.start)});
        }
        segments.push({
            type: 'token',
            kind: match.kind,
            value: remainingValue.slice(match.start, match.end),
        });
        remainingValue = remainingValue.slice(match.end);
    }

    return segments;
}
