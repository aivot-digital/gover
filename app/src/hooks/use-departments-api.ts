import {Api} from './use-api';
import {Department} from '../models/entities/department';

interface DepartmentsApi {
    list(filter?: { ids: number[] }): Promise<Department[]>;

    retrieve(id: number): Promise<Department>;

    save(department: Department): Promise<Department>;

    destroy(id: number): Promise<void>;
}

export function useDepartmentsApi(api: Api): DepartmentsApi {
    const list = async (filter?: { ids: number[] }) => {
        return await api.get<Department[]>('departments', {
            id: filter?.ids,
        });
    };

    const retrieve = async (id: number) => {
        return await api.getPublic<Department>(`departments/${id}`);
    };

    const save = async (department: Department) => {
        if (department.id === 0) {
            return await api.post<Department>('departments', department);
        } else {
            return await api.put<Department>(`departments/${department.id}`, department);
        }
    };

    const destroy = async (id: number) => {
        return await api.destroy<void>(`departments/${id}`);
    };

    return {
        list,
        retrieve,
        save,
        destroy,
    };
}
