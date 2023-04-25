import {UserRole, UserRoleLevels} from '../data/user-role';
import {User} from "../models/entities/user";

export function checkUserRole(minRole: UserRole, user?: User): boolean {
    const minRoleLevel = UserRoleLevels[minRole];
    const userRoleLevel = user?.role != null ? UserRoleLevels[user?.role] : 0;
    return userRoleLevel >= minRoleLevel;
}
