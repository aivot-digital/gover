export enum StorageProviderType {
    Assets = 'Assets',
    Attachments = 'Attachments',
    External = 'External',
}

export const StorageProviderTypeLabels: Record<StorageProviderType, string> = {
    [StorageProviderType.Assets]: 'Dateien und Medien',
    [StorageProviderType.Attachments]: 'Vorgangsanlagen',
    [StorageProviderType.External]: 'Externe Dateiablage',
};

export const StorageProviderTypes = [
    StorageProviderType.Assets,
    StorageProviderType.Attachments,
    StorageProviderType.External,
];
