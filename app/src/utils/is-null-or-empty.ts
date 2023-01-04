export function isNullOrEmpty(val?: string | null): boolean {
    return val == null || val.length === 0;
}