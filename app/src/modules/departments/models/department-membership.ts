import {UserRole} from '../../../data/user-role';

export interface DepartmentMembership {
    id: number;
    departmentId: number;
    departmentName: string;
    userId: string;
    role: UserRole;
    userEmail: string;
    userFirstName: string;
    userLastName: string;
}
