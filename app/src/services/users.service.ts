import axios from 'axios';
import {User} from '../models/user';
import {CrudService} from './crud.service';

class _UserService extends CrudService<User, 'users', number> {
    constructor() {
        super('users');
    }

    async login(email: string, password: string): Promise<{
        jwtToken: string;
    }> {
        const response = await axios.post(this.basePath + 'login', {
            email,
            password,
        });
        return response.data;
    }

    async setPassword(password: string, userId?: number): Promise<void> {
        await axios.post(this.basePath + 'profile/set-password', {
            userId,
            password,
        }, CrudService.getConfig());
    }

    async getProfile(): Promise<User> {
        return await axios.get(this.basePath + 'profile', CrudService.getConfig()).then(resp => resp.data);
    }
}

export const UsersService = new _UserService();
