import {Api} from './use-api';
import {User} from '../modules/users/models/user';
import {Page} from '../models/dtos/page';

interface UsersApi {
    list(filter?: { ids?: string[] }): Promise<User[]>;

    retrieve(id: string): Promise<User>;

    self(): Promise<User>;
}

/**
 * @deprecated Use UsersApiService instead
 * @param api
 */
export function useUsersApi(api: Api): UsersApi {
    const list = async (filter?: { ids?: string[] }) => {
        var res = await api.get<Page<User>>('users/', {
            queryParams: {
                id: filter?.ids,
                page: 0,
                size: 1000,
            },
        });

        return res.content;
    };

    const retrieve = async (id: string) => {
        return await api.get<User>(`users/${id}/`);
    };

    const self = async () => {
        return await api.get<User>('users/self/');
    };

    return {
        list,
        retrieve,
        self,
    };
}
