import {isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {ElementWithParents} from './flatten-elements';

export function isElementChangedByTrigger(
    {element, parents}: ElementWithParents,
    triggeringElementId: string,
): boolean {
    console.log('isElementChangedByTrigger', {element, parents, triggeringElementId});
    if (parents.some(p => p.id === triggeringElementId || triggeringElementId.startsWith(p.id + '.'))) {
        return true;
    }

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