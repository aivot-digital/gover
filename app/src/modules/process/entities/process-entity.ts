export interface ProcessEntity {
    id: number;
    internalTitle: string;
    departmentId: number;
    versionCount: number;
    draftedVersion: number | null;
    publishedVersion: number | null;
    created: string;
    updated: string;
}