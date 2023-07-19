import {type UserRole} from '../../data/user-role';

export interface DepartmentMembershipBaseDto {
    id: number;
    role: UserRole;
}
