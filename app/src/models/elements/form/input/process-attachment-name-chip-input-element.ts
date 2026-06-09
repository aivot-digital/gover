import {type BaseInputElement} from '../base-input-element';
import {type ElementType} from '../../../../data/element-type/element-type';

export interface ProcessAttachmentNameChipInputElement extends BaseInputElement<ElementType.ProcessAttachmentNameChipInput> {
    placeholder: string | null | undefined;
    minItems: number | null | undefined;
    maxItems: number | null | undefined;
}
