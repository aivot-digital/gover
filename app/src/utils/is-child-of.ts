import {type AnyElement} from '../models/elements/any-element';
import {type AnyElementWithChildren, isAnyElementWithChildren} from '../models/elements/any-element-with-children';

export function isChildOf(target: AnyElement, parent: AnyElementWithChildren): boolean {
    for (const c of parent.children) {
        if (c.id === target.id) {
            return true;
        }

        if (isAnyElementWithChildren(c)) {
            if (isChildOf(target, c)) {
                return true;
            }
        }
    }

    return false;
}
