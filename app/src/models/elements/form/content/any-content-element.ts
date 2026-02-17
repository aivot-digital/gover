import {AlertElement} from './alert-element';
import {HeadlineElement} from './headline-element';
import {RichtextElement} from './richtext-element';
import {SpacerElement} from './spacer-element';
import {ImageElement} from './image-element';
import {ElementType} from '../../../../data/element-type/element-type';
import {AnyInputElement} from '../input/any-input-element';

export type AnyContentElement =
    AlertElement |
    HeadlineElement |
    ImageElement |
    RichtextElement |
    SpacerElement;

export function isAnyContentElement(obj: any): obj is AnyInputElement {
    return obj != null && 'type' in obj && [
        ElementType.Alert,
        ElementType.Headline,
        ElementType.Image,
        ElementType.RichText,
        ElementType.Spacer,
    ].includes(obj.type);
}
