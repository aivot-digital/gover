import {BaseInputElement} from './base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';

export interface TextFieldElement extends BaseInputElement<string, ElementType.Text> {
    placeholder?: string;
    isMultiline?: boolean;
    maxCharacters?: number;
}
