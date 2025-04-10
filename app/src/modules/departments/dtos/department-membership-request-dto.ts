import {UserRole} from '../../../data/user-role';

export interface DepartmentMembershipRequestDTO {
    departmentId: number;
    userId: string;
    role: UserRole;
}
