function isObject(value: unknown): value is Record<string, unknown> {
    return value != null && typeof value === 'object' && !Array.isArray(value);
}

function canonicalize(value: unknown): unknown {
    if (Array.isArray(value)) return value.map(canonicalize);
    if (isObject(value)) return Object.fromEntries(Object.keys(value).sort().map(key => [key, canonicalize(value[key])]));
    return value;
}

function equal(left: unknown, right: unknown): boolean {
    if (left === right) return true;
    if (Array.isArray(left) && Array.isArray(right)) return left.length === right.length && left.every((value, index) => equal(value, right[index]));
    if (isObject(left) && isObject(right)) {
        const keys = Object.keys(left);
        return keys.length === Object.keys(right).length && keys.every(key => Object.hasOwn(right, key) && equal(left[key], right[key]));
    }
    return false;
}

export function compareProcessData(previous: Record<string, unknown> | null, current: Record<string, unknown>) {
    const counts = {added: 0, removed: 0, changed: 0};
    const visit = (before: Record<string, unknown>, after: Record<string, unknown>) => {
        for (const key of new Set([...Object.keys(before), ...Object.keys(after)])) {
            if (!Object.hasOwn(before, key)) counts.added++;
            else if (!Object.hasOwn(after, key)) counts.removed++;
            else {
                const oldValue = before[key];
                const newValue = after[key];
                if (isObject(oldValue) && isObject(newValue)) visit(oldValue, newValue);
                // Arrays and newly added/removed objects count as one field, preserving their domain grouping.
                else if (!equal(oldValue, newValue)) counts.changed++;
            }
        }
    };
    if (previous != null) visit(previous, current);
    return {
        counts,
        unchanged: previous != null && Object.values(counts).every(count => count === 0),
        currentJson: JSON.stringify(canonicalize(current), null, 4),
        previousJson: previous == null ? undefined : JSON.stringify(canonicalize(previous), null, 4),
    };
}
