import {ElementType} from '../../../../data/element-type/element-type';
import {BaseFormElement} from '../base-form-element';

export interface HeadlineElement extends BaseFormElement<ElementType.Headline> {
    content?: string;
    small?: boolean;
}
