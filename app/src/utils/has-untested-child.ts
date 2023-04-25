import {isElementTested} from './is-element-tested';
import {AnyElement} from '../models/elements/any-element';
import {AnyElementWithChildren, isAnyElementWithChildren} from "../models/elements/any-element-with-children";

export function hasUntestedChild(comp: AnyElementWithChildren): boolean {
    return comp.children.some((child: AnyElement) => {
        return !isElementTested(child) || (isAnyElementWithChildren(child) && hasUntestedChild(child));
    });
}
