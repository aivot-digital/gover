import {BaseInputElement} from '../base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';

export interface DataObjectSelectFieldElement extends BaseInputElement<ElementType.DataObjectSelect> {
    placeholder: string | null | undefined;
    dataModelKey: string | null | undefined;
    dataLabelAttributeKey: string | null | undefined;
}
