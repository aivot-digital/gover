import {BaseInputElement} from '../base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';

export interface RadioFieldElement extends BaseInputElement<string, ElementType.Radio> {
    options?: string[];
}
