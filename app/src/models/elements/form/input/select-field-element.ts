import {type BaseInputElement} from '../base-input-element';
import {type ElementType} from '../../../../data/element-type/element-type';

export interface SelectFieldElementOption {
    value: string;
    label: string;
}

export interface SelectFieldElement extends BaseInputElement<ElementType.Select> {
    autocomplete: string | null | undefined;
    placeholder: string | null | undefined;
    options: SelectFieldElementOption[] | null | undefined;
}
