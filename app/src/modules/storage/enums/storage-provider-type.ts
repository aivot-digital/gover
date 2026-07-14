export enum StorageProviderType {
    Assets = 'Assets',
    Attachments = 'Attachments',
    External = 'External',
}

export const StorageProviderTypeLabels: Record<StorageProviderType, string> = {
    [StorageProviderType.Assets]: 'Dokumente und Medien',
    [StorageProviderType.Attachments]: 'Prozessanlagen',
    [StorageProviderType.External]: 'Externe Dokumentenablage',
};

export const StorageProviderTypes = [
    StorageProviderType.Assets,
    StorageProviderType.Attachments,
    StorageProviderType.External,
];
