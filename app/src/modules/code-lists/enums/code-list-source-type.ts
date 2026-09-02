export enum CodeListSourceType {
    Manual = 'Manual',
    Plugin = 'Plugin',
    XRepository = 'XRepository',
    Asset = 'Asset',
}

export const CodeListSourceTypeLabels: Record<CodeListSourceType, string> = {
    [CodeListSourceType.Manual]: 'Manuell',
    [CodeListSourceType.Plugin]: 'Plugin',
    [CodeListSourceType.XRepository]: 'XRepository',
    [CodeListSourceType.Asset]: 'CSV-Datei',
};

export function isCodeListSyncable(sourceType: CodeListSourceType): boolean {
    return sourceType === CodeListSourceType.XRepository || sourceType === CodeListSourceType.Asset;
}
