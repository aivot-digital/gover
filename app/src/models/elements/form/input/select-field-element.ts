import {type BaseInputElement} from '../base-input-element';
import {type ElementType} from '../../../../data/element-type/element-type';

export interface SelectFieldElementOption {
    value: string;
    label: string;
}

export interface SelectFieldElement extends BaseInputElement<string | SelectFieldElementOption[], ElementType.Select> {
    autocomplete?: string;
    placeholder?: string;
    options?: string[] | SelectFieldElementOption[];
}
