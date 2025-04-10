import {type BaseInputElement} from '../base-input-element';
import {type ElementType} from '../../../../data/element-type/element-type';

export interface MultiCheckboxFieldElementOption {
    value: string;
    label: string;
}

export interface MultiCheckboxFieldElement extends BaseInputElement<string[] | MultiCheckboxFieldElementOption[], ElementType.MultiCheckbox> {
    options?: string[] | MultiCheckboxFieldElementOption[];
    minimumRequiredOptions?: number;
    displayInline?: boolean;
}
