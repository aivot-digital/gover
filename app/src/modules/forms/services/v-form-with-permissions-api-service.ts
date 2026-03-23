import {BaseCrudApiService} from '../../../services/base-crud-api-service';
import {VFormWithPermissionsEntity} from '../entities/v-form-with-permissions-entity';

interface FormWithPermissionsFilter {
    id: number;
    slug: string;
    internalTitle: string;
    developingDepartmentId: number;
    developingDepartmentIdNot: number;
    publishedVersion: number;
    draftedVersion: number;
    userId: string;
    isDrafted: boolean;
    isPublished: boolean;
    isRevoked: boolean;
    formPermissionCreate: boolean;
    formPermissionRead: boolean;
    formPermissionEdit: boolean;
    formPermissionDelete: boolean;
    formPermissionAnnotate: boolean;
    formPermissionPublish: boolean;
}

export interface FormWithPermissionsEntityId {
    formId: number;
    userId: string;
}

export class VFormWithPermissionsApiService extends BaseCrudApiService<VFormWithPermissionsEntity, VFormWithPermissionsEntity, VFormWithPermissionsEntity, VFormWithPermissionsEntity, FormWithPermissionsEntityId, FormWithPermissionsFilter> {
    public constructor() {
        super('/api/form-with-permissions/');
    }

    public buildPath(id: FormWithPermissionsEntityId): string {
        return `${this.path}${id.formId}/${id.userId}/`;
    }

    public initialize(): VFormWithPermissionsEntity {
        return VFormWithPermissionsApiService.initialize();
    }

    public static initialize(): VFormWithPermissionsEntity {
        return {
            created: '',
            developingDepartmentId: 0,
            draftedVersion: 0,
            formPermissionAnnotate: false,
            formPermissionCreate: false,
            formPermissionDelete: false,
            formPermissionEdit: false,
            formPermissionPublish: false,
            formPermissionRead: false,
            id: 0,
            internalTitle: '',
            publishedVersion: 0,
            slug: '',
            updated: '',
            userId: '',
            versionCount: 0,

        };
    }
}