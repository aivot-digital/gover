import {type AnyContentElement} from './content/any-content-element';
import {type AnyInputElement} from './input/any-input-element';
import {type AnyLayoutElement} from './layout/any-layout-element';

export type AnyFormElement =
    AnyContentElement |
    AnyInputElement |
    AnyLayoutElement;
