import {CrudApiService} from '../../services/crud-api-service';
import {Api} from '../../hooks/use-api';
import {User} from './models/user';

export interface UserFilter {
    name: string;
    deletedInIdp: boolean;
    disabledInIdp: boolean;
}

export class UsersApiService extends CrudApiService<User, User, User, User, User, string, UserFilter> {
    public constructor(api: Api) {
        super(api, 'users/');
    }

    public async retrieve(id: string): Promise<User> {
        try {
            return await super.retrieve(id);
        } catch (err: any) {
            if (err.response?.status === 404) {
                return {
                    id: id,
                    email: '',
                    firstName: '',
                    lastName: '',
                    fullName: '',
                    enabled: false,
                    verified: false,
                    globalAdmin: false,
                    deletedInIdp: false,
                };
            }
            throw err;
        }
    }

    public async retrieveSelf(): Promise<User> {
        return await this.retrieve('self');
    }

    public initialize(): User {
        return {
            id: '',
            email: '',
            firstName: '',
            lastName: '',
            fullName: '',
            enabled: false,
            verified: false,
            globalAdmin: false,
            deletedInIdp: false,
        };
    }
}