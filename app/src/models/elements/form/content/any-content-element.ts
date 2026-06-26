import {AlertElement} from './alert-element';
import {HeadlineElement} from './headline-element';
import {RichtextElement} from './richtext-element';
import {SpacerElement} from './spacer-element';
import {ImageElement} from './image-element';
import {ProcessAttachmentDisplayElement} from './process-attachment-display-element';
import {ElementType} from '../../../../data/element-type/element-type';

export type AnyContentElement =
    AlertElement |
    HeadlineElement |
    ImageElement |
    ProcessAttachmentDisplayElement |
    RichtextElement |
    SpacerElement;

export function isAnyContentElement(obj: any): obj is AnyContentElement {
    return obj != null && 'type' in obj && [
        ElementType.Alert,
        ElementType.Headline,
        ElementType.Image,
        ElementType.ProcessAttachmentDisplay,
        ElementType.RichText,
        ElementType.Spacer,
    ].includes(obj.type);
}
