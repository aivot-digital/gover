import {BaseFormElement} from "../base-form-element";
import {ElementType} from "../../../../data/element-type/element-type";
import {AnyFormElement} from "../any-form-element";

export interface GroupLayout extends BaseFormElement<ElementType.Container> {
    children: AnyFormElement[];
}
