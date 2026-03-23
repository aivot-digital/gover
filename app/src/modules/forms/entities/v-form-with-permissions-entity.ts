export interface VFormWithPermissionsEntity {
    id: number;
    slug: string;
    internalTitle: string;
    developingDepartmentId: number;
    created: string; // ISO date string
    updated: string; // ISO date string
    publishedVersion: number;
    draftedVersion: number;
    versionCount: number;
    userId: string;
    formPermissionCreate: boolean;
    formPermissionRead: boolean;
    formPermissionEdit: boolean;
    formPermissionDelete: boolean;
    formPermissionAnnotate: boolean;
    formPermissionPublish: boolean;
}