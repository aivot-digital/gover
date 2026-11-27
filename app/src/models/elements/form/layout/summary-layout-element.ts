import {type BaseFormElement} from '../base-form-element';
import {ElementType} from '../../../../data/element-type/element-type';
import {type AnyFormElement} from '../any-form-element';

export interface SummaryLayoutElement extends BaseFormElement<ElementType.SummaryLayout> {
    children: AnyFormElement[];
}
