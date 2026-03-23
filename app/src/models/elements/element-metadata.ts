import {UserInfoIdentifier} from '../../data/user-info-identifier';

export interface ElementMetadata {
    identityMappings?: Record<string, string>;
    userInfoIdentifier?: UserInfoIdentifier;
}
