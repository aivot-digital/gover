import {User} from './models/user';
import {BaseCrudApiService} from '../../services/base-crud-api-service';

export interface UserFilter {
    name: string;
    deletedInIdp: boolean;
    disabledInIdp: boolean;
}

export class UsersApiService extends BaseCrudApiService<User, User, User, User, string, UserFilter> {
    public constructor() {
        super('/api/users/');
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
                    superAdmin: false,
                    systemAdmin: false,
                    globalRole: 0,
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
            superAdmin: false,
            systemAdmin: false,
            globalRole: 0,
            deletedInIdp: false,
        };
    }
}