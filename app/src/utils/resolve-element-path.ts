export function splitDiffPath(path: string): string[] {
    if (path.length === 0 || path === '/') {
        return [];
    }

    if (path.includes('/')) {
        return path.split('/').filter((segment) => segment.length > 0);
    }

    const segments: string[] = [];
    let currentSegment = '';

    for (let i = 0; i < path.length; i++) {
        const currentChar = path[i];

        if (currentChar === '.') {
            if (currentSegment.length > 0) {
                segments.push(currentSegment);
                currentSegment = '';
            }
            continue;
        }

        if (currentChar === '[') {
            if (currentSegment.length > 0) {
                segments.push(currentSegment);
                currentSegment = '';
            }

            const closingBracketIndex = path.indexOf(']', i);
            if (closingBracketIndex === -1) {
                break;
            }

            segments.push(path.slice(i + 1, closingBracketIndex));
            i = closingBracketIndex;
            continue;
        }

        currentSegment += currentChar;
    }

    if (currentSegment.length > 0) {
        segments.push(currentSegment);
    }

    return segments;
}

export function getLeafDiffPathSegment(path: string): string {
    const pathSegments = splitDiffPath(path);
    return pathSegments[pathSegments.length - 1] ?? '';
}
