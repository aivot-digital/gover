export function isStringArray(obj: any): obj is string[] {
    return Array.isArray(obj) && obj.every((item) => typeof item === 'string');
}
