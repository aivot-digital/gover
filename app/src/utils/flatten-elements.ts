import {AnyElement} from "../models/elements/any-element";
import {isAnyElementWithChildren} from "../models/elements/any-element-with-children";

export function flattenElements(elem: AnyElement): AnyElement[] {
    const res = [
        elem,
    ];

    if (isAnyElementWithChildren(elem)) {
        for (const child of elem.children) {
            res.push(...flattenElements(child));
        }
    }

    return res;
}
