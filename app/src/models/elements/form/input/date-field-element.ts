import {BaseInputElement} from '../base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';

export enum DateFieldComponentModelMode {
    Day = 'day',
    Month = 'month',
    Year = 'year',
}

export interface DateFieldElement extends BaseInputElement<ElementType.Date> {
    autocomplete: string | null | undefined;
    placeholder: string | null | undefined;
    mode: DateFieldComponentModelMode | null | undefined;
}
