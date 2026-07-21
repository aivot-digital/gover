import {type BaseInputElement} from '../base-input-element';
import {type ElementType} from '../../../../data/element-type/element-type';
import {OptionsSourceType} from './options-source-type';

export interface ChipInputFieldElement extends BaseInputElement<ElementType.ChipInput> {
    placeholder: string | null | undefined;
    suggestions: string[] | null | undefined;
    optionsSource: OptionsSourceType | null | undefined;
    codeListId: number | null | undefined;
    minItems: number | null | undefined;
    maxItems: number | null | undefined;
    allowDuplicates: boolean | null | undefined;
}
