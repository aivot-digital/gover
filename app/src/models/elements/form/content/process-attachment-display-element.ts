import type {ElementType} from '../../../../data/element-type/element-type';
import type {BaseFormElement} from '../base-form-element';

export interface ProcessAttachmentDisplayElement extends BaseFormElement<ElementType.ProcessAttachmentDisplay> {
    attachmentSetKey: string | null | undefined;
    label?: string | null;
    hint?: string | null;
}
