import {type AnyElement} from '../models/elements/any-element';
import {evaluateFunction} from './evaluate-function';
import {type CustomerInput} from '../models/customer-input';

export function generateComponentPatch(idPrefix: string | undefined, allElements: AnyElement[], id: string, element: AnyElement, customerInput: CustomerInput): any {
    if (element.patchElement == null) {
        return;
    }
    return evaluateFunction(idPrefix, allElements, element.patchElement, customerInput, element, id, false);
}
