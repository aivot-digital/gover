import {AnyElement} from '../models/elements/any-element';
import {isAnyInputElement} from '../models/elements/form/input/any-input-element';

export function isElementChangedByTrigger(
    element: AnyElement,
    triggeringElementId: string,
): boolean {
    if (element.visibility?.referencedIds?.includes(triggeringElementId)) {
        return true;
    }

    if (element.override?.referencedIds?.includes(triggeringElementId)) {
        return true;
    }

    if (isAnyInputElement(element)) {
        if (element.value?.referencedIds?.includes(triggeringElementId)) {
            return true;
        }
    }

    return false;
}