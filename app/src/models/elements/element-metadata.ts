import {UserInfoIdentifier} from '../../data/user-info-identifier';

export interface ElementMetadata {
    identitySourceId?: string;
    identityMappings?: Record<string, string>;
    userInfoIdentifier?: UserInfoIdentifier;
}
