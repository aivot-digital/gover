export enum CodeListStatus {
    SyncPending = 'SyncPending',
    Syncing = 'Syncing',
    Synced = 'Synced',
    SyncFailed = 'SyncFailed',
}

export const CodeListStatusLabels: Record<CodeListStatus, string> = {
    [CodeListStatus.SyncPending]: 'Synchronisation ausstehend',
    [CodeListStatus.Syncing]: 'Synchronisierung läuft',
    [CodeListStatus.Synced]: 'Synchronisiert',
    [CodeListStatus.SyncFailed]: 'Synchronisierung fehlgeschlagen',
};

export const CodeListStatusColors: Record<CodeListStatus, 'warning' | 'info' | 'success' | 'error'> = {
    [CodeListStatus.SyncPending]: 'warning',
    [CodeListStatus.Syncing]: 'info',
    [CodeListStatus.Synced]: 'success',
    [CodeListStatus.SyncFailed]: 'error',
};
