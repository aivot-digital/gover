import {type DepartmentMembershipBaseDto} from './department-membership-base-dto';

export interface DepartmentMembershipDto extends DepartmentMembershipBaseDto {
    department: number;
    user: number;
}
