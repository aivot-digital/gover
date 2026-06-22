import {type BaseFormElement} from '../base-form-element';
import {type ElementType} from '../../../../data/element-type/element-type';
import {type AnyFormElement} from '../any-form-element';

export interface ConfigLayoutElement extends BaseFormElement<ElementType.ConfigLayout> {
    children: AnyFormElement[];
}
