import {isStringNotNullOrEmpty, isStringNullOrEmpty} from '../../../utils/string-utils';
import {User} from '../models/user';

export function resolveUserName(user?: User) {
    if (user == null || user.id === '') {
        return 'Gelöschte Mitarbeiter:in';
    }

    return resolveName(user) + resolveStatus(user);
}

function resolveName(user: User) {
    if (isStringNullOrEmpty(user.firstName) && isStringNullOrEmpty(user.lastName)) {
        return 'Unbenannte Mitarbeiter:in';
    }

    if (isStringNotNullOrEmpty(user.firstName) && isStringNullOrEmpty(user.lastName)) {
        return user.firstName;
    }

    if (isStringNullOrEmpty(user.firstName) && isStringNotNullOrEmpty(user.lastName)) {
        return user.lastName;
    }

    return user.firstName + ' ' + user.lastName;
}

function resolveStatus(user: User): string {
    if (user.deletedInIdp) {
        return ' (gelöscht)';
    }

    if (!Boolean(user.enabled)) {
        return ' (inaktiv)';
    }

    return '';
}