import {BaseInputElement} from '../base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';

export enum DateFieldComponentModelMode {
    Day = 'day',
    Month = 'month',
    Year = 'year',
}

export interface DateFieldElement extends BaseInputElement<string, ElementType.Date> {
    autocomplete?: string;
    placeholder?: string;
    mode?: DateFieldComponentModelMode;
}
