import {ElementType} from '../../../../data/element-type/element-type';
import {BaseFormElement} from '../base-form-element';

export interface ImageElement extends BaseFormElement<ElementType.Image> {
    height?: number;
    width?: number;
    caption?: string;
    src: string;
    alt: string;
}
