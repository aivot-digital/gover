import {AlertElement} from './alert-element';
import {HeadlineElement} from './headline-element';
import {RichtextElement} from './richtext-element';
import {SpacerElement} from './spacer-element';
import {ImageElement} from './image-element';

export type AnyContentElement =
    AlertElement |
    HeadlineElement |
    ImageElement |
    RichtextElement |
    SpacerElement;
