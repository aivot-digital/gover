export function isStringNullOrEmpty(val?: string | null): boolean {
    return val == null || typeof val != 'string'  || val.trim().length === 0;
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

export function stringOrUndefined(val: string | null | undefined): string | undefined {
    if (isStringNullOrEmpty(val)) {
        return undefined;
    }
    return val!;
}