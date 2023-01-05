import {AnyContentElement} from './content-elements/any-content-element';
import {AnyInputElement} from './input-elements/any-input-element';
import {AnyLayoutElement} from './layout-elements/any-layout-element';

export type AnyFormElement = (
    AnyContentElement |
    AnyInputElement |
    AnyLayoutElement
);
