export enum PermissionLevel {
    None = 0,
    Read = 1,
    Write = 2,
}

export const PermissionLevelLabels: Record<PermissionLevel, string> = {
    [PermissionLevel.None]: 'Keine Berechtigung',
    [PermissionLevel.Read]: 'Lesen',
    [PermissionLevel.Write]: 'Lesen und Schreiben',
};


