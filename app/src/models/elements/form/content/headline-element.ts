import {ElementType} from '../../../../data/element-type/element-type';
import {BaseFormElement} from '../base-form-element';

export interface HeadlineElement extends BaseFormElement<ElementType.Headline> {
    content: string | null | undefined;
    small: boolean | null | undefined;
    uppercase: boolean | null | undefined;
}
