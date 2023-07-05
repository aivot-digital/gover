import {BaseInputElement} from '../base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';

export interface SelectFieldElement extends BaseInputElement<string, ElementType.Select> {
    placeholder?: string;
    options?: string[];
}
