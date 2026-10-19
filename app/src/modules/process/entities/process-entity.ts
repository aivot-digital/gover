export interface ProcessEntity {
    id: number;
    internalTitle: string;
    departmentId: number;
    accessKey: string;
    slug: string;
    versionCount: number;
    draftedVersion: number | null;
    publishedVersion: number | null;
    created: string;
    updated: string;
}
