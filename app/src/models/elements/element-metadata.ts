import {UserInfoIdentifier} from '../../data/user-info-identifier';
import {BundIdAttribute} from '../../data/bund-id-attributes';
import {BayernIdAttribute} from '../../data/bayern-id-attributes';
import {ShIdAttribute} from '../../data/sh-id-attributes';
import {MukAttribute} from '../../data/muk-attributes';

export interface ElementMetadata {
    bundIdMapping?: BundIdAttribute;
    bayernIdMapping?: BayernIdAttribute;
    shIdMapping?: ShIdAttribute;
    mukMapping?: MukAttribute;
    identityMappings?: Record<string, string>;
    userInfoIdentifier?: UserInfoIdentifier;
}
