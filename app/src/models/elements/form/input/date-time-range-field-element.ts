import {BaseInputElement} from '../base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';
import {TimeFieldComponentModelMode} from './time-field-element';

export interface DateTimeRangeFieldElement extends BaseInputElement<ElementType.DateTimeRange> {
    placeholder: string | null | undefined;
    mode: TimeFieldComponentModelMode | null | undefined;
}

export interface DateTimeRangeValue {
    start: string | null | undefined;
    end: string | null | undefined;
}
