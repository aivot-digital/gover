import {type BaseInputElement} from '../base-input-element';
import {type ElementType} from '../../../../data/element-type/element-type';
import {OptionsSourceType} from './options-source-type';

export interface RadioFieldElementOption {
    value: string;
    label: string;
}

export interface RadioFieldElement extends BaseInputElement<ElementType.Radio> {
    options: RadioFieldElementOption[] | null | undefined;
    optionsSource: OptionsSourceType | null | undefined;
    codeListId: number | null | undefined;
    displayInline: boolean | null | undefined;
    toggleButtons?: boolean | null | undefined;
}
