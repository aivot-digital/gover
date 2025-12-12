export interface ProcessDefinitionEntity {
    id: number;
    name: string;
    departmentId: number;
    versionCount: number;
    draftedVersion: number | null;
    publishedVersion: number | null;
    created: string;
    updated: string;
}