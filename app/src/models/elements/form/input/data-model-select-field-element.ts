import {BaseInputElement} from '../base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';

export interface DataModelSelectFieldElement extends BaseInputElement<ElementType.DataModelSelect> {
    placeholder: string | null | undefined;
}
