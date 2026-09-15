// Keep this grammar aligned with ProcessDataValueUtils: properties use dots, array segments use brackets.
// Wildcards are mapping patterns, not executable JavaScript expressions.
const property = '[a-zA-Z_$][a-zA-Z0-9_$]*';
const index = '(?:0|[1-9][0-9]*)';

export function processDataPathPattern(allowWildcards: boolean): string {
    const arrayIndex = allowWildcards ? `(?:${index}|\\*)` : index;
    return `^\\s*${property}(?:\\s*\\.\\s*${property}|\\s*\\[\\s*${arrayIndex}\\s*\\])*\\s*$`;
}

export function parseProcessDataPath(path: string, allowArrayRoot = false): string[] | null {
    const input = path.trim();
    const segments: string[] = [];
    const firstProperty = new RegExp(property, 'y');
    const nextProperty = new RegExp(`\\.\\s*(${property})`, 'y');
    const array = /\[\s*(0|[1-9][0-9]*|\*)\s*\]/y;
    let offset = 0;
    while (offset < input.length) {
        if (/\s/.test(input[offset])) {
            offset++;
            continue;
        }
        array.lastIndex = offset;
        const arrayMatch = (allowArrayRoot || segments.length > 0) ? array.exec(input) : null;
        if (arrayMatch != null) {
            const segment = arrayMatch[1];
            if (segment !== '*' && Number(segment) > 2147483647) {
                return null;
            }
            segments.push(segment);
            offset = array.lastIndex;
            continue;
        }
        const matcher = segments.length === 0 ? firstProperty : nextProperty;
        matcher.lastIndex = offset;
        const match = matcher.exec(input);
        if (match == null) {
            return null;
        }
        segments.push(segments.length === 0 ? match[0] : match[1]);
        offset = matcher.lastIndex;
    }
    return segments;
}

export function formatProcessDataPath(segments: string[]): string {
    return segments.map((segment, i) => /^(?:[0-9]+|\*)$/.test(segment)
        ? `[${segment}]`
        : `${i === 0 ? '' : '.'}${segment}`).join('');
}

export function isValidProcessDataPath(path: string, allowWildcards = false, allowArrayRoot = false): boolean {
    const segments = parseProcessDataPath(path, allowArrayRoot);
    return segments != null && segments.length > 0 && (allowWildcards || !segments.includes('*'));
}
