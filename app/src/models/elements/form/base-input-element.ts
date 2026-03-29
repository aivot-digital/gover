import {type BaseFormElement} from './base-form-element';
import {type ElementType} from '../../../data/element-type/element-type';
import {ElementValueFunction} from '../element-value-function';
import {ElementValidationFunction} from '../element-validation-function';

export interface BaseInputElement<T extends ElementType> extends BaseFormElement<T> {
    label: string | null | undefined;
    hint: string | null | undefined;
    required: boolean | null | undefined;
    disabled: boolean | null | undefined;
    technical: boolean | null | undefined;
    display: boolean | null | undefined;
    destinationKey: string | null | undefined;

    validation: ElementValidationFunction | null | undefined;
    value: ElementValueFunction | null | undefined;
}
