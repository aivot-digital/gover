import {BaseCrudApiService} from '../../../services/base-crud-api-service';
import {VDepartmentMembershipWithDetailsEntity} from '../entities/v-department-membership-with-details-entity';
import {VDepartmentMembershipWithPermissionsEntity} from '../entities/v-department-membership-with-permissions-entity';

interface VDepartmentMembershipWithPermissionsFilter {
    id: number;
    departmentId: number;
    userId: string;
    departmentPermissionEdit: boolean;
}

export class VDepartmentMembershipWithPermissionsService extends BaseCrudApiService<
    VDepartmentMembershipWithPermissionsEntity,
    VDepartmentMembershipWithPermissionsEntity,
    VDepartmentMembershipWithPermissionsEntity,
    VDepartmentMembershipWithPermissionsEntity,
    number,
    VDepartmentMembershipWithPermissionsFilter
> {
    public constructor() {
        super('/api/department-memberships-with-permissions/');
    }

    public initialize(): VDepartmentMembershipWithPermissionsEntity {
        return {
            created: '',
            departmentId: 0,
            departmentPermissionEdit: false,
            formPermissionAnnotate: false,
            formPermissionCreate: false,
            formPermissionDelete: false,
            formPermissionEdit: false,
            formPermissionPublish: false,
            formPermissionRead: false,
            id: 0,
            processInstancePermissionAnnotate: false,
            processInstancePermissionCreate: false,
            processInstancePermissionDelete: false,
            processInstancePermissionEdit: false,
            processInstancePermissionRead: false,
            processPermissionAnnotate: false,
            processPermissionCreate: false,
            processPermissionDelete: false,
            processPermissionEdit: false,
            processPermissionPublish: false,
            processPermissionRead: false,
            updated: '',
            userId: '',
        };
    }
}