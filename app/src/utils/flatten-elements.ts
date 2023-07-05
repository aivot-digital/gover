import {AnyElement} from "../models/elements/any-element";
import {isAnyElementWithChildren} from "../models/elements/any-element-with-children";
import {ElementType} from "../data/element-type/element-type";

export function flattenElements(elem: AnyElement, skipReplicatingChildren?: boolean): AnyElement[] {
    const res = [
        elem,
    ];

    if (isAnyElementWithChildren(elem)) {
        for (const child of elem.children) {
            if (skipReplicatingChildren && child.type === ElementType.ReplicatingContainer) {

            } else {
                res.push(...flattenElements(child, skipReplicatingChildren));
            }
        }
    }

    return res;
}
