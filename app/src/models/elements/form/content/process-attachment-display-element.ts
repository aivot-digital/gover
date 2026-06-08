import {ElementType} from '../../../../data/element-type/element-type';
import {BaseFormElement} from '../base-form-element';

export interface ProcessAttachmentDisplayElement extends BaseFormElement<ElementType.ProcessAttachmentDisplay> {
    fileName: string | null | undefined;
}
