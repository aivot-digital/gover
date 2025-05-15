import {UserInfoIdentifier} from '../../data/user-info-identifier';

export interface ElementMetadata {
    bundIdMapping?: string; // Keep to support legacy forms
    bayernIdMapping?: string; // Keep to support legacy forms
    shIdMapping?: string; // Keep to support legacy forms
    mukMapping?: string; // Keep to support legacy forms
    identityMappings?: Record<string, string>;
    userInfoIdentifier?: UserInfoIdentifier;
}
