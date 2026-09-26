export const duplicatePageWarningRouteHandle = {
    duplicatePageWarning: true,
} as const;

export function hasDuplicatePageWarningRouteHandle(handle: unknown): boolean {
    return (
        handle != null &&
        typeof handle === 'object' &&
        'duplicatePageWarning' in handle &&
        handle.duplicatePageWarning === true
    );
}
