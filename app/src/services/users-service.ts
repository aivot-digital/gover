import { type User } from '../models/entities/user';
import { ApiService } from './api-service';

class _UserService extends ApiService<User, User, number> {
    constructor() {
        super('users');
    }

    public async login(email: string, password: string): Promise<{
        jwtToken: string;
    }> {
        return await ApiService.post('login', {
            email,
            password,
        });
    }

    public async getProfile(): Promise<User> {
        return await ApiService.get('users/self');
    }
}

export const UsersService = new _UserService();
