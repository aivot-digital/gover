import {Api} from './use-api';
import {
    DepartmentMembership,
    DepartmentMembershipWithDepartment,
    DepartmentMembershipWithUser,
} from '../models/entities/department-membership';
import {NewAnonymousUser, User} from '../models/entities/user';
import {useDepartmentsApi} from "./use-departments-api";
import {NewDepartment} from "../models/entities/department";
import {useUsersApi} from './use-users-api';

interface MembershipsApi {
    list(filter?: {user?: string, department?: number}): Promise<DepartmentMembership[]>;

    listWithDepartments(filter?: {user?: string, department?: number}): Promise<DepartmentMembershipWithDepartment[]>;

    listWithUsers(filter?: {user?: string, department?: number}): Promise<DepartmentMembershipWithUser[]>;

    retrieve(id: number): Promise<DepartmentMembership>;

    save(membership: DepartmentMembership): Promise<DepartmentMembership>;

    destroy(id: number): Promise<void>;
}

export function useMembershipsApi(api: Api): MembershipsApi {
    const list = async (filter?: {user?: string, department?: number}) => {
        return await api.get<DepartmentMembership[]>('department-memberships', filter);
    };

    const listWithDepartments = async (filter?: {user?: string, department?: number}) => {
        const memberships = await list(filter);
        const departmentIdSet = new Set(memberships.map(membership => membership.departmentId));
        const departments = await useDepartmentsApi(api).list({ids: Array.from(departmentIdSet)});
        const departmentsMap = new Map(departments.map(department => [department.id, department]));
        return memberships.map(membership => {
            return {
                ...membership,
                department: departmentsMap.get(membership.departmentId) ?? NewDepartment('Unbekannter Fachbereich'),
            };
        });
    };

    const listWithUsers = async (filter?: {user?: string, department?: number}) => {
        const memberships = await list(filter);
        const userIdSet = new Set(memberships.map(membership => membership.userId));
        const users = await useUsersApi(api).list({ids: Array.from(userIdSet)});
        const usersMap = new Map(users.map(department => [department.id, department]));
        return memberships.map(membership => {
            return {
                ...membership,
                user: usersMap.get(membership.userId) ?? NewAnonymousUser(),
            };
        });
    };

    const retrieve = async (id: number) => {
        return await api.get<DepartmentMembership>(`department-memberships/${id}`);
    };

    const save = async (membership: DepartmentMembership) => {
        if (membership.id === 0) {
            return await api.post<DepartmentMembership>(`department-memberships`, membership);
        } else {
            return await api.put<DepartmentMembership>(`department-memberships/${membership.id}`, membership);
        }
    };

    const destroy = async (id: number) => {
        return await api.destroy<void>(`department-memberships/${id}`);
    };

    return {
        list,
        listWithDepartments,
        listWithUsers,
        retrieve,
        save,
        destroy,
    };
}
