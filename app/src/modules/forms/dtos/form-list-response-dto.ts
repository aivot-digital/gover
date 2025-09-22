export interface FormListResponseDTO {
    id: number;
    slug: string;
    internalTitle: string;
    publicTitle: string;
    developingDepartmentId: number;
    managingDepartmentId: number;
    responsibleDepartmentId: number;
    created: string;
    updated: string;
    publishedVersion: number;
    draftedVersion: number;
    versionCount: number;
}