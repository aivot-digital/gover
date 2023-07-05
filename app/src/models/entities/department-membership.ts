import {UserRole} from "../../data/user-role";

export interface DepartmentMembership {
    id: number;
    department: number;
    user: number;
    role: UserRole;
}
