import {Department} from '../models/entities/department';
import {CrudService} from './crud.service';
import {ApiDetailsResponse} from '../models/api-details-response';
import axios from 'axios';
import {ApiConfig} from '../api-config';

class _DepartmentsService extends CrudService<Department, 'departments', number> {
    constructor() {
        super('departments');
    }

    async retrieve(id: number): Promise<ApiDetailsResponse<Department>> {
        return await axios.get(ApiConfig.address + '/public/departments/' + id)
            .then(response => response.data);
    }
}

export const DepartmentsService = new _DepartmentsService();
