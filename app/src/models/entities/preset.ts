export interface Preset {
    key: string;
    title: string;
    storeId: string | null;
    currentVersion: string;
    currentStoreVersion: string | null;
    currentPublishedVersion: string | null;
    created: string;
    updated: string;
}
