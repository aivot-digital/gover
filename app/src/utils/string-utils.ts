export function isStringNullOrEmpty(val?: string | null): boolean {
    return val == null || val.length === 0;
}

export function isStringNotNullOrEmpty(val?: string | null): boolean {
    return !isStringNullOrEmpty(val);
}

export function stringOrDefault(val: string | null | undefined, def: string): string {
    if (isStringNullOrEmpty(val)) {
        return def;
    }
    return val!;
}
