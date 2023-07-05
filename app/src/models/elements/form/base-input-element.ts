import {BaseFormElement} from "./base-form-element";
import {ElementType} from "../../../data/element-type/element-type";
import {Function} from "../../functions/function";

export interface BaseInputElement<V, T extends ElementType> extends BaseFormElement<T> {
    label?: string;
    hint?: string;
    required?: boolean;
    disabled?: boolean;

    validate?: Function;
    computeValue?: Function;

    computedValue?: V;
}
