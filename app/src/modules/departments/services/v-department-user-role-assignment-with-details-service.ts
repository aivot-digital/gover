import {BaseCrudApiService} from '../../../services/base-crud-api-service';
import {VDepartmentUserRoleAssignmentWithDetailsEntity} from '../entities/v-department-user-role-assignment-with-details-entity';
import {UserRoleAssignmentEntity} from "../../user-roles/entities/user-role-assignment-entity";
import {DepartmentMembershipEntity} from '../entities/department-membership-entity';

interface VDepartmentUserRoleAssignmentWithDetailsFilter {
    id: number;
    departmentId: number;
    name: string;
    userId: string;
    fullName: string;
    userRoleId: number;
}

/**
 * @deprecated use VDepartmentMembershipWithDetailsService
 */
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

    /** Replaces all roles of the membership; an outdated `lockVersion` is rejected with a lock version conflict. */
    public async updateRoles(membershipId: number, lockVersion: number, roleIds: number[]): Promise<DepartmentMembershipEntity> {
        return await this.put(`${this.path}memberships/${membershipId}/`, {lockVersion, roleIds});
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