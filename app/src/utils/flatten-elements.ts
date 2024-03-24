import {type AnyElement} from '../models/elements/any-element';
import {isAnyElementWithChildren} from '../models/elements/any-element-with-children';
import {ElementType} from '../data/element-type/element-type';

export function flattenElements(elem: AnyElement, skipReplicatingChildren?: boolean): AnyElement[] {
    const res = [
        elem,
    ];

    if (isAnyElementWithChildren(elem)) {
        for (const child of elem.children) {
            if (skipReplicatingChildren === true && child.type === ElementType.ReplicatingContainer) {

            } else {
                res.push(...flattenElements(child, skipReplicatingChildren));
            }
        }
    }

    return res;
}

export function flattenElementsWithParents(elem: AnyElement, parents: AnyElement[], skipReplicatingChildren?: boolean): Array<{ element: AnyElement, parents: AnyElement[] }> {
    const res = [
        {
            element: elem,
            parents,
        },
    ];

    if (isAnyElementWithChildren(elem)) {
        for (const child of elem.children) {
            if (skipReplicatingChildren === true && child.type === ElementType.ReplicatingContainer) {

            } else {
                res.push(...flattenElementsWithParents(child, [...parents, elem], skipReplicatingChildren));
            }
        }
    }

    return res;
}