import {BaseInputElement} from '../base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';
import {DateFieldComponentModelMode} from './date-field-element';

export interface DateRangeFieldElement extends BaseInputElement<ElementType.DateRange> {
    placeholder: string | null | undefined;
    mode: DateFieldComponentModelMode | null | undefined;
}

export interface DateRangeValue {
    start: string | null | undefined;
    end: string | null | undefined;
}
