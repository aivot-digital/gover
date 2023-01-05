import {isBaseElement} from '../../base-element';
import {ElementType} from '../../../../data/element-type/element-type';
import {AnyInputElement} from './any-input-element';
import {BaseFormElement} from '../base-form-element';

export interface BaseInputElement<V, T extends ElementType> extends BaseFormElement<T> {
    label?: string;
    hint?: string;
    required?: boolean;
    disabled?: boolean;
    value?: V;
}

export function isInputElement(obj: any): obj is AnyInputElement {
    return isBaseElement(obj); // TODO
}
