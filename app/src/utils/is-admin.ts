import {User} from '../modules/users/models/user';
import {DepartmentMembership} from '../modules/departments/models/department-membership';
import {UserRole} from '../data/user-role';

export function isAdmin(user: User | undefined | null): boolean {
    return user != null && user.globalAdmin;
}

export function isDepartmentAdmin(departmentMemberships: DepartmentMembership[] | undefined | null, departmentId: number | undefined | null): boolean {
    if (departmentMemberships == null || departmentId == null) {
        return false;
    }

    const membership = departmentMemberships
        .find(membership => membership.departmentId === departmentId);

    return membership != null && membership.role === UserRole.Admin;
}