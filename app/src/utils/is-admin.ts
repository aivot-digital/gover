import {User} from '../modules/users/models/user';
import {DepartmentMembershipWithRoles} from '../modules/departments/dtos/department-membership-response-dto';

export function isAdmin(user: User | undefined | null): boolean {
    return user != null && user.globalAdmin;
}

export function isDepartmentAdmin(departmentMemberships: DepartmentMembershipWithRoles[] | undefined | null, departmentId: number | undefined | null): boolean {
    if (departmentMemberships == null || departmentId == null) {
        return false;
    }

    const membership = departmentMemberships
        .find(membership => membership.orgUnitId === departmentId);

    return membership != null &&
        membership.roles.some(role => role.userRoleOrgUnitMemberPermissionEdit);
}