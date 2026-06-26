import type {ElementType} from '../../../../data/element-type/element-type';
import type {BaseFormElement} from '../base-form-element';

export interface ProcessAttachmentDisplayElement extends BaseFormElement<ElementType.ProcessAttachmentDisplay> {
    fileName: string | null | undefined;
    hint?: string | null;
}
