import {BaseFormElement} from "../base-form-element";
import {ElementType} from "../../../../data/element-type/element-type";

export interface GroupLayout extends BaseFormElement<ElementType.Container> {
    children: BaseFormElement<any>;
}
