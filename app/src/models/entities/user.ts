import {User as BaseUser} from '../../modules/users/models/user';
import {resolveUserName} from '../../modules/users/utils/resolve-user-name';
export type User = BaseUser;

export const getFullName = resolveUserName;