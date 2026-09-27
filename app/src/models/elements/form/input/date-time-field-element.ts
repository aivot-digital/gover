import {BaseInputElement} from '../base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';
import {TimeFieldComponentModelMode} from './time-field-element';

export interface DateTimeFieldElement extends BaseInputElement<ElementType.DateTime> {
    placeholder: string | null | undefined;
    mode: TimeFieldComponentModelMode | null | undefined;
}
