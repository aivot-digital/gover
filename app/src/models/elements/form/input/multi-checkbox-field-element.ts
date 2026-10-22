import {type BaseInputElement} from '../base-input-element';
import {type ElementType} from '../../../../data/element-type/element-type';
import {type MultiChoicePresentation} from './multi-choice-presentation';
import {OptionsSourceType} from './options-source-type';

export interface MultiCheckboxFieldElementOption {
    value: string;
    label: string;
}

export interface MultiCheckboxFieldElement extends BaseInputElement<ElementType.MultiCheckbox> {
    presentation?: MultiChoicePresentation | null;
    options: MultiCheckboxFieldElementOption[] | null | undefined;
    optionsSource: OptionsSourceType | null | undefined;
    codeListKey: string | null | undefined;
    minimumRequiredOptions: number | null | undefined;
    displayInline: boolean | null | undefined;
}
