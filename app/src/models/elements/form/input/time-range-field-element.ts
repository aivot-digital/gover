import {BaseInputElement} from '../base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';
import {TimeFieldComponentModelMode} from './time-field-element';

export interface TimeRangeFieldElement extends BaseInputElement<ElementType.TimeRange> {
    mode: TimeFieldComponentModelMode | null | undefined;
}

export interface TimeRangeValue {
    start: string | null | undefined;
    end: string | null | undefined;
}
