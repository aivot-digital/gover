export interface Asset {
    key: string;
    storageProviderId: number;
    storagePathFromRoot: string;
    directory?: boolean;
    missing?: boolean;
    sizeInBytes?: number;
    filename: string;
    created: string;
    uploaderId: string;
    contentType?: string | null;
    isPrivate: boolean;
    metadata?: Record<string, string>;
}
