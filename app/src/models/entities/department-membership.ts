import {UserRole} from "../../data/user-role";
import {Department} from "./department";
import {User} from './user';

export interface DepartmentMembership {
    id: number;
    departmentId: number;
    userId: string;
    role: UserRole;
}

export type DepartmentMembershipWithDepartment = DepartmentMembership & {
    department: Department;
}

export type DepartmentMembershipWithUser = DepartmentMembership & {
    user: User;
}