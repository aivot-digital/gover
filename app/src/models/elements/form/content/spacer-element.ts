import {ElementType} from '../../../../data/element-type/element-type';
import {BaseFormElement} from '../base-form-element';

export interface SpacerElement extends BaseFormElement<ElementType.Spacer> {
    height?: string;
}
