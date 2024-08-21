import {isStringNotNullOrEmpty, isStringNullOrEmpty} from '../../utils/string-utils';

export interface User {
    id: string;
    username: string;
    firstName?: string;
    lastName?: string;
    email?: string;
    enabled: boolean;
    roles: Array<{
        name: string;
        description: string;
    }>;
    groups: Array<{
        name: string;
        path: string;
    }>;
}

export function NewAnonymousUser(): User {
    return {
        id: '',
        username: '',
        firstName: '',
        lastName: '',
        enabled: false,
        email: '',
        roles: [],
        groups: [],
    };
}

export function getFullName(user?: User) {
    if (user == null || user.id === '') {
        return 'Gelöschte Mitarbeiter:in';
    }

    const isDisabled = !Boolean(user.enabled);

    if (isStringNullOrEmpty(user.firstName) && isStringNullOrEmpty(user.lastName)) {
        return 'Unbenannte Mitarbeiter:in' + (isDisabled ? ' (deaktiviert)' : '');
    }

    if (isStringNotNullOrEmpty(user.firstName) && isStringNullOrEmpty(user.lastName)) {
        return user.firstName + (isDisabled ? ' (deaktiviert)' : '');
    }

    if (isStringNullOrEmpty(user.firstName) && isStringNotNullOrEmpty(user.lastName)) {
        return user.lastName + (isDisabled ? ' (deaktiviert)' : '');
    }

    return (user.firstName ?? '') + ' ' + (user.lastName ?? '') + (isDisabled ? ' (deaktiviert)' : '');
}