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
    // Keys of the code list columns for the values and the labels.
    codeListValueColumnKey?: string | null;
    codeListLabelColumnKey?: string | null;
    maximumOptions?: number | null;
    minimumRequiredOptions: number | null | undefined;
    displayInline: boolean | null | undefined;
}
