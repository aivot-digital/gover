import {BaseCrudApiService} from '../../../services/base-crud-api-service';
import {VDepartmentUserRoleAssignmentWithDetailsEntity} from '../entities/v-department-user-role-assignment-with-details-entity';
import {UserRoleAssignmentEntity} from "../../user-roles/entities/user-role-assignment-entity";

interface VDepartmentUserRoleAssignmentWithDetailsFilter {
    id: number;
    departmentId: number;
    name: string;
    userId: string;
    fullName: string;
    userRoleId: number;
}

export class VDepartmentUserRoleAssignmentWithDetailsService extends BaseCrudApiService<
    UserRoleAssignmentEntity,
    VDepartmentUserRoleAssignmentWithDetailsEntity,
    VDepartmentUserRoleAssignmentWithDetailsEntity,
    VDepartmentUserRoleAssignmentWithDetailsEntity,
    number,
    VDepartmentUserRoleAssignmentWithDetailsFilter
> {
    public constructor() {
        super('/api/department-user-role-assignments-with-details/');
    }

    public initialize(): VDepartmentUserRoleAssignmentWithDetailsEntity {
        return {
            created: '',
            departmentId: 0,
            depth: 0,
            id: 0,
            name: '',
            updated: '',
            userId: '',
        };
    }
}