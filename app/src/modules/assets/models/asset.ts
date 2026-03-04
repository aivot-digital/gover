export interface Asset {
    key: string;
    storageProviderId: number;
    storagePathFromRoot: string;
    filename: string;
    created: string;
    uploaderId: string;
    contentType?: string | null;
    isPrivate: boolean;
    metadata?: Record<string, unknown>;
}
