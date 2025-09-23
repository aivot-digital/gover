import {BaseInputElement} from '../base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';

export interface TextFieldElement extends BaseInputElement<ElementType.Text> {
    autocomplete: string | null | undefined;
    placeholder: string | null | undefined;
    isMultiline: boolean | null | undefined;
    maxCharacters: number | null | undefined;
    minCharacters: number | null | undefined;
    pattern: {
        regex: string;
        message: string;
    } | null | undefined;
    suggestions: string[] | null | undefined;
}
