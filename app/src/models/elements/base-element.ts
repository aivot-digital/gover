import {ElementType} from '../../data/element-type/element-type';
import {TestProtocolSet} from "../lib/test-protocol-set";
import {Function} from "../functions/function";

export interface BaseElement<T extends ElementType> {
    type: T;
    id: string;
    appVersion: string;
    name?: string;
    isVisible?: Function;
    patchElement?: Function;
    testProtocolSet?: TestProtocolSet;
}
