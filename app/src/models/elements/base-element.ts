import {ElementType} from '../../data/element-type/element-type';
import {TestProtocolSet} from "../lib/test-protocol-set";
import {FunctionCode} from "../functions/function-code";

export interface BaseElement<T extends ElementType> {
    type: T;
    id: string;

    name?: string;

    isVisible?: Function;
    patchElement?: FunctionCode;

    testProtocolSet?: TestProtocolSet;
}
