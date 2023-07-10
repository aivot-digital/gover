export interface User {
    id: number;
    name: string;
    email: string;
    password: string;
    active: boolean;
    admin: boolean;
    created: string;
    updated: string;
}

export interface AnonymousUser extends User {
    id: -1;
}

export function isAnonymousUser(user: User): user is AnonymousUser {
    return user.id === -1;
}

export interface InvalidUser extends User {
    id: -2;
}

export function isInvalidUser(user: User): user is InvalidUser {
    return user.id === -2;
}
