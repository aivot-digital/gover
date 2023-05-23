import {AnyElement} from "../models/elements/any-element";
import {evaluateFunction} from "./evaluate-function";
import {CustomerInput} from "../models/customer-input";

export function generateComponentPatch(allElements: AnyElement[], id: string, element: AnyElement, customerInput: CustomerInput): any {
    if (element.patchElement == null) {
        return;
    }
    return evaluateFunction(allElements, element.patchElement, customerInput, element, id, false);
}
