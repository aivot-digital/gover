import {BaseElement} from '../../base-element';
import {ElementType} from '../../../../data/element-type/element-type';
import {AnyLayoutElement} from './any-layout-element';
import {BaseFormElement} from '../base-form-element';

export interface BaseLayoutElement<C extends BaseElement<any>, T extends ElementType> extends BaseFormElement<T> {
    children: C[];
}

export function isLayoutElement(obj: any): obj is AnyLayoutElement {
    return 'children' in obj;
}
