import {AnyElement} from '../models/elements/any-element';
import {evaluateFunction} from "./evaluate-function";
import {CustomerInput} from "../models/customer-input";

export function isElementVisible(id: string, element: AnyElement, customerInput: CustomerInput): boolean {
    if (element.isVisible == null) {
        return true;
    }

    try {
        return evaluateFunction(element.isVisible, customerInput, element, id, true);
    } catch (err) {
        console.error(`Failed to run visibility function of ID ${id}`, element, err);
        return false;
    }
}
