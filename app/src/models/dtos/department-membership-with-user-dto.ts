import {type DepartmentMembershipBaseDto} from './department-membership-base-dto';
import {type User} from '../entities/user';

export interface DepartmentMembershipWithUserDto extends DepartmentMembershipBaseDto {
    department: number;
    user: User;
}
