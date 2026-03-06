import {BaseInputElement} from '../base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';

export enum TimeFieldComponentModelMode {
    Minute = 'minute',
    Second = 'second',
}

export interface TimeFieldElement extends BaseInputElement<ElementType.Time> {
    mode: TimeFieldComponentModelMode | null | undefined;
}
