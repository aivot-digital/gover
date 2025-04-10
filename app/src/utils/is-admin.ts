import {User} from '../modules/users/models/user';

export function isAdmin(user: User | undefined | null): boolean {
    return user != null && user.globalAdmin;
}
