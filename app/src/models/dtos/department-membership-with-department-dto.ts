import {type DepartmentMembershipBaseDto} from './department-membership-base-dto';
import {type Department} from '../entities/department';

export interface DepartmentMembershipWithDepartmentDto extends DepartmentMembershipBaseDto {
    department: Department;
    user: number;
}
