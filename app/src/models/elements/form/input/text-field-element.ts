import {BaseInputElement} from '../base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';
import {type DynamicTextPolicy} from '../../../input-mode';

export interface TextFieldElement extends BaseInputElement<ElementType.Text> {
    dynamicTextPolicy?: DynamicTextPolicy | null;
    autocomplete: string | null | undefined;
    placeholder: string | null | undefined;
    isMultiline: boolean | null | undefined;
    maxCharacters: number | null | undefined;
    minCharacters: number | null | undefined;
    pattern: {
        regex: string;
        message: string;
    } | null | undefined;
    prefix: string | null | undefined;
    copyable: boolean | null | undefined;
    copyValueTemplate: string | null | undefined;
    suggestions: string[] | null | undefined;
}
