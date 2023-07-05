import {Department} from '../models/entities/department';
import {ApiService} from "./api-service";

class _DepartmentsService extends ApiService<Department, Department, number> {
    constructor() {
        super('departments');
    }

    public async retrieve(id: number): Promise<Department> {
        return await ApiService.get('public/departments/' + id);
    }
}

export const DepartmentsService = new _DepartmentsService();
