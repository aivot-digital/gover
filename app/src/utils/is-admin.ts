import {User} from '../modules/users/models/user';
import {
    VDepartmentMembershipWithDetailsEntity
} from "../modules/departments/entities/v-department-membership-with-details-entity";
import {Permission} from "../data/permissions/permission";

export function isAdmin(user: User | undefined | null): boolean {
    return user != null && user.isSuperAdmin;
}

export function isDepartmentAdmin(departmentMemberships: VDepartmentMembershipWithDetailsEntity[] | undefined | null, departmentId: number | undefined | null): boolean {
    if (departmentMemberships == null || departmentId == null) {
        return false;
    }

    const membership = departmentMemberships
        .find(membership => membership.departmentId === departmentId);

    return membership != null &&
        membership.domainRoles.some(role => role.permissions.includes(Permission.DEPARTMENT_UPDATE));
}