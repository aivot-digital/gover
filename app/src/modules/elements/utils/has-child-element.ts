import {AnyElementWithChildren, isAnyElementWithChildren} from '../../../models/elements/any-element-with-children';
import {AnyElement} from '../../../models/elements/any-element';

export function hasChildElement(element: AnyElementWithChildren, childElement: AnyElement): boolean {
    if (element.children.includes(childElement as any)) {
        return true;
    }

    for (const child of element.children) {
        if (isAnyElementWithChildren(child) && hasChildElement(child, childElement)) {
            return true;
        }
    }

    return false;
}