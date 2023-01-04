import {FunctionSet} from '../../components/_lib/function-set';
import {TestProtocol} from '../../components/_lib/test-protocol';
import {ElementType} from '../../data/element-type/element-type';

export interface BaseElement<T extends ElementType> {
    id: string;
    type: T;

    name?: string;

    visibility?: FunctionSet;
    validate?: FunctionSet;
    patch?: FunctionSet;

    technicalTest?: TestProtocol;
    professionalTest?: TestProtocol;
}

export function isBaseElement(obj: any): obj is BaseElement<ElementType> {
    return obj.type != null;
}
