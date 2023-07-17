import {AnyElement} from '../models/elements/any-element';
import {evaluateFunction} from "./evaluate-function";
import {CustomerInput} from "../models/customer-input";

export function isElementVisible(
    idPrefix: string | undefined,
    allElements: AnyElement[],
    id: string,
    element: AnyElement,
    customerInput: CustomerInput,
): boolean {
    if (element.isVisible == null) {
        return true;
    }

    return evaluateFunction(idPrefix, allElements, element.isVisible, customerInput, element, id, true);
}
