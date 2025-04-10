export interface Asset {
    key: string;
    filename: string;
    created: string;
    uploaderId: string;
    contentType?: string | null;
    isPrivate: boolean;
}
