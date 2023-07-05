import {UserRole, UserRoleLevels} from '../data/user-role';
import {User} from "../models/entities/user";

/**
 * TODO: Rework publish role
 * @param minRole
 * @param user
 */
export function checkUserRole(minRole: UserRole, user?: User): boolean {
    const minRoleLevel = UserRoleLevels[minRole];
    const userRoleLevel = 0;// TODO: FIX with Memberships user?.role != null ? UserRoleLevels[user?.role] : 0;
    return userRoleLevel >= minRoleLevel;
}
