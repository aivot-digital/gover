import {User} from '../modules/users/models/user';
import {VDepartmentMembershipWithDetailsEntityWithRoles} from '../modules/departments/entities/v-department-membership-with-details-entity';

export function isAdmin(user: User | undefined | null): boolean {
    return user != null && user.superAdmin;
}

export function isDepartmentAdmin(departmentMemberships: VDepartmentMembershipWithDetailsEntityWithRoles[] | undefined | null, departmentId: number | undefined | null): boolean {
    if (departmentMemberships == null || departmentId == null) {
        return false;
    }

    const membership = departmentMemberships
        .find(membership => membership.departmentId === departmentId);

    return membership != null &&
        membership.roles.some(role => role.departmentPermissionEdit);
}