import {type BaseFormElement} from './base-form-element';
import {type ElementType} from '../../../data/element-type/element-type';
import {type Function} from '../../functions/function';

export interface BaseInputElement<V, T extends ElementType> extends BaseFormElement<T> {
    label?: string;
    hint?: string;
    required?: boolean;
    technical?: boolean;
    disabled?: boolean;

    validate?: Function;
    computeValue?: Function;

    computedValue?: V;
    destinationKey?: string | null;
}
