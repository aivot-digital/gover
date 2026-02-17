export enum StorageProviderType {
    Assets = 'Assets',
    Attachments = 'Attachments',
}

export const StorageProviderTypeLabels: Record<StorageProviderType, string> = {
    [StorageProviderType.Assets]: 'Dokumente und Medien',
    [StorageProviderType.Attachments]: 'Prozessanlagen',
};

export const StorageProviderTypes = [
    StorageProviderType.Assets,
    StorageProviderType.Attachments,
];