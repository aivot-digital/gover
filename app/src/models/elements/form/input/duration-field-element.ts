import {BaseInputElement} from '../base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';

export enum DurationPrecision {
    Minute = 'minute',
    Second = 'second',
}

export enum DurationLargestUnit {
    Hour = 'hour',
    Day = 'day',
}

/** The value and bounds are total seconds, independent of input precision. */
export interface DurationFieldElement extends BaseInputElement<ElementType.Duration> {
    precision?: DurationPrecision | null;
    largestUnit?: DurationLargestUnit | null;
    minDuration?: number | null;
    maxDuration?: number | null;
}
