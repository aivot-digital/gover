import {ElementType} from '../../../../data/element-type/element-type';
import {BaseFormElement} from '../base-form-element';


export interface RichtextElement extends BaseFormElement<ElementType.RichText> {
    content: string | null | undefined;
}
