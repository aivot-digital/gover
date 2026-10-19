import {ElementType} from '../../../../data/element-type/element-type';
import {BaseFormElement} from '../base-form-element';

export interface ImageElement extends BaseFormElement<ElementType.Image> {
    height: number | null | undefined;
    width: number | null | undefined;
    caption: string | null | undefined;
    src: string | null | undefined;
    alt: string | null | undefined;
}
