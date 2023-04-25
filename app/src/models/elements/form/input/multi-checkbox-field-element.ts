import {BaseInputElement} from '../base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';

export interface MultiCheckboxFieldElement extends BaseInputElement<string[], ElementType.MultiCheckbox> {
    options?: string[];
    minimumRequiredOptions?: number;
}
