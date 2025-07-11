import {BaseInputElement} from '../base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';

export interface NumberFieldElement extends BaseInputElement<ElementType.Number> {
    placeholder: string | null | undefined;
    decimalPlaces: number | null | undefined;
    suffix: string | null | undefined;
}
