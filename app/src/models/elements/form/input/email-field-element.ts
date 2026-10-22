import {BaseInputElement} from '../base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';

export interface EmailFieldElement extends BaseInputElement<ElementType.Email> {
    allowLocalDomains?: boolean | null;
    autocomplete?: string | null;
    placeholder?: string | null;
}
