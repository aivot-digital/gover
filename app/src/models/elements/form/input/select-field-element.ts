import {type BaseInputElement} from '../base-input-element';
import {type ElementType} from '../../../../data/element-type/element-type';

export interface SelectFieldElementOption {
    value: string;
    label: string;
    group?: string | null | undefined;
}

export interface SelectFieldElement extends BaseInputElement<ElementType.Select> {
    autocomplete: string | null | undefined;
    placeholder: string | null | undefined;
    dependsOnSelectFieldId: string | null | undefined;
    options: Array<SelectFieldElementOption | string> | null | undefined;
}
