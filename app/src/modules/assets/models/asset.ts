export interface Asset {
    key: string;
    storageProviderId: number;
    storagePathFromRoot: string;
    directory?: boolean;
    missing?: boolean;
    sizeInBytes?: number;
    filename: string;
    created: string;
    /** Changes with every content or metadata write; sent back so that a stale save gets a conflict. */
    updated?: string;
    uploaderId: string;
    contentType?: string | null;
    isPrivate: boolean;
    metadata?: Record<string, string>;
}
