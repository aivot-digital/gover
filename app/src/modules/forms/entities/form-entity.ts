export interface FormEntity {
    id: number;
    slug: string;
    internalTitle: string;
    developingDepartmentId: number;
    publishedVersion: number;
    draftedVersion: number;
    versionCount: number;
    created: string;
    updated: string;
}

export function isFormEntity(object: any): object is FormEntity {
    return (
        typeof object === 'object' &&
        object !== null &&
        typeof object.id === 'number' &&
        typeof object.slug === 'string' &&
        typeof object.internalTitle === 'string' &&
        typeof object.developingDepartmentId === 'number' &&
        typeof object.publishedVersion === 'number' &&
        typeof object.draftedVersion === 'number' &&
        typeof object.versionCount === 'number' &&
        typeof object.created === 'string' &&
        typeof object.updated === 'string'
    );
}