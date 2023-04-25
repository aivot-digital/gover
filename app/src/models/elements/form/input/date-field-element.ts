import {BaseInputElement} from '../base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';

export enum DateFieldComponentModelMode {
    Date = 'day',
    Month = 'month',
    Year = 'year',
}

export interface DateFieldElement extends BaseInputElement<string, ElementType.Date> {
    placeholder?: string;
    mode?: DateFieldComponentModelMode;
    mustBePast?: boolean;
    mustBeFuture?: boolean;
}
