import {ElementType} from '../../../data/element-type/element-type';
import {BaseElement} from '../base-element';

export interface BaseFormElement<T extends ElementType> extends BaseElement<T> {
    weight?: number;
}
