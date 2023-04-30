import {BaseFormElement} from "./base-form-element";
import {ElementType} from "../../../data/element-type/element-type";
import {FunctionCode} from "../../functions/function-code";
import {FunctionNoCode} from "../../functions/function-no-code";

export interface BaseInputElement<V, T extends ElementType> extends BaseFormElement<T> {
    label?: string;
    hint?: string;
    required?: boolean;
    disabled?: boolean;

    validate?: FunctionCode | FunctionNoCode;
    computeValue?: FunctionCode;

    computedValue?: V;
}
