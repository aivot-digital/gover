import {type BaseInputElement} from '../base-input-element';
import {type ElementType} from '../../../../data/element-type/element-type';

export interface RadioFieldElementOption {
    value: string;
    label: string;
}

export interface RadioFieldElement extends BaseInputElement<string | RadioFieldElementOption[], ElementType.Radio> {
    options?: string[] | RadioFieldElementOption[];
    displayInline?: boolean;
}
