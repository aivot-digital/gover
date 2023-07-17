import {type Department} from '../models/entities/department';
import {ApiService} from './api-service';
import {ListApplication} from '../models/entities/list-application';

class _DepartmentsService extends ApiService<Department, Department, number> {
    constructor() {
        super('departments');
    }

    public async retrieve(id: number): Promise<Department> {
        return await ApiService.get('public/departments/' + id);
    }

    public async listApplications(id: number): Promise<ListApplication[]> {
        return await ApiService.get('departments/' + id + '/applications');
    }
}

export const DepartmentsService = new _DepartmentsService();
