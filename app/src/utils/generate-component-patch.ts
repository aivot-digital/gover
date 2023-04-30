import {AnyElement} from "../models/elements/any-element";
import {evaluateFunction} from "./evaluate-function";
import {CustomerInput} from "../models/customer-input";

export function generateComponentPatch(id: string, element: AnyElement, customerInput: CustomerInput): any {
    try {
        return evaluateFunction(element.patchElement, customerInput, element, id, false);
    } catch (err) {
        console.error('Failed to run patch of ID ' + id, err);
        return null;
    }
}
