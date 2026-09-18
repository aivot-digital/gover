import {type BaseInputElement} from '../base-input-element';
import {type ElementType} from '../../../../data/element-type/element-type';

export interface ProcessInstanceAttachmentSetSelectElement extends BaseInputElement<ElementType.ProcessInstanceAttachmentSetSelect> {
    placeholder: string | null | undefined;
    minItems: number | null | undefined;
    maxItems: number | null | undefined;
}
