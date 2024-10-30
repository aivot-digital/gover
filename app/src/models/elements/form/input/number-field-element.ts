import {BaseInputElement} from '../base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';

export interface NumberFieldElement extends BaseInputElement<number, ElementType.Number> {
    placeholder?: string;
    decimalPlaces?: number;
    suffix?: string;
}
