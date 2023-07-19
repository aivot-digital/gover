import {type Department} from '../models/entities/department';
import {ApiService} from './api-service';
import {type ListApplication} from '../models/entities/list-application';
import {type DepartmentMembershipWithUserDto} from '../models/dtos/department-membership-with-user-dto';

class _DepartmentsService extends ApiService<Department, Department, number> {
    constructor() {
        super('departments');
    }

    public async retrieve(id: number): Promise<Department> {
        return await ApiService.get(`public/departments/${id}`);
    }

    public async listApplications(id: number): Promise<ListApplication[]> {
        return await ApiService.get(`departments/${id}/applications`);
    }

    public async listMemberships(id: number): Promise<DepartmentMembershipWithUserDto[]> {
        return await ApiService.get(`departments/${id}/memberships`);
    }
}

export const DepartmentsService = new _DepartmentsService();
