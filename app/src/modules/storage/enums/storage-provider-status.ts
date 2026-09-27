export enum StorageProviderStatus {
    SyncPending = 'SyncPending',
    Syncing = 'Syncing',
    Synced = 'Synced',
    SyncFailed = 'SyncFailed',
}

export const StorageProviderStatusLabels: Record<StorageProviderStatus, string> = {
    [StorageProviderStatus.SyncPending]: 'Synchronisation ausstehend',
    [StorageProviderStatus.Syncing]: 'Synchronisierung läuft',
    [StorageProviderStatus.Synced]: 'Synchronisiert',
    [StorageProviderStatus.SyncFailed]: 'Synchronisierung fehlgeschlagen',
};

export const StorageProviderStatusColors: Record<StorageProviderStatus, 'warning' | 'info' | 'success' | 'error'> = {
    [StorageProviderStatus.SyncPending]: 'warning',
    [StorageProviderStatus.Syncing]: 'info',
    [StorageProviderStatus.Synced]: 'success',
    [StorageProviderStatus.SyncFailed]: 'error',
};
