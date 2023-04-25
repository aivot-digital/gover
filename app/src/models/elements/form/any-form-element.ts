import {AnyContentElement} from "./content/any-content-element";
import {AnyInputElement} from "./input/any-input-element";
import {AnyLayoutElement} from "./layout/any-layout-element";

export type AnyFormElement =
    AnyContentElement |
    AnyInputElement |
    AnyLayoutElement;
