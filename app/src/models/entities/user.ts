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

export function NewAnonymousUser(): AnonymousUser {
    return {
        id: -1,
        name: 'Anonymous',
        email: '',
        password: '',
        active: false,
        admin: false,
        created: '',
        updated: '',
    };
}

export function isAnonymousUser(user: User): user is AnonymousUser {
    return user.id === -1;
}

export type AnyUser = User | AnonymousUser;
