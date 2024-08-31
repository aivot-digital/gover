import {type ElementType} from '../../data/element-type/element-type';
import {type TestProtocolSet} from '../lib/test-protocol-set';
import {type Function as AppFunction} from '../functions/function';
import {ElementMetadata} from "./element-metadata";
import {UserInfoIdentifier} from '../../data/user-info-identifier';

export interface BaseElement<T extends ElementType> {
    type: T;
    id: string;
    appVersion: string;
    name?: string;
    isVisible?: AppFunction;
    patchElement?: AppFunction;
    testProtocolSet?: TestProtocolSet;
    metadata?: ElementMetadata;
}
