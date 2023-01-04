import {AnyElement} from '../models/elements/any-element';
import {generateElementIdForType} from './generate-element-id';
import {isAnyElementWithChildren} from '../models/elements/any-element-with-children';

export function regenerateIdsForElement(elem: AnyElement): AnyElement {
    const updatedElem = {
        ...elem,
        id: generateElementIdForType(elem.type),
    };

    if (isAnyElementWithChildren(updatedElem)) {
        updatedElem.children = updatedElem.children.map(child => regenerateIdsForElement(child)) as any;
    }

    return updatedElem;
}
