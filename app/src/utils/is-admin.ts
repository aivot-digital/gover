import {type User} from '../models/entities/user';

export function isAdmin(user: User | undefined | null): boolean {
    return user != null && user.roles.some((role) => role.name.toLowerCase() === 'admin');
}
