import {type User} from '../models/entities/user';
import {Api} from './use-api';

interface UsersApi {
    list(filter?: { ids?: string[] }): Promise<User[]>;

    retrieve(id: string): Promise<User>;

    self(): Promise<User>;
}

export function useUsersApi(api: Api): UsersApi {
    const list = async (filter?: { ids?: string[] }) => {
        return await api.get<User[]>('users', {
            id: filter?.ids,
        });
    };

    const retrieve = async (id: string) => {
        return await api.get<User>(`users/${id}`);
    };

    const self = async () => {
        return await api.get<User>('users/self');
    };

    return {
        list,
        retrieve,
        self,
    };
}
