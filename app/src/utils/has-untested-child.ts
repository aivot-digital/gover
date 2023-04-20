import {isElementTested} from './is-element-tested';
import {AnyElement} from '../models/elements/any-element';
import {isLayoutElement} from '../models/elements/./form/./layout/base-layout-element';
import {AnyElementWithChildren} from '../models/elements/any-element-with-children';

export function hasUntestedChild(comp: AnyElementWithChildren): boolean {
    return comp.children.some((child: AnyElement) => {
        return !isElementTested(child) || (isLayoutElement(child) && hasUntestedChild(child));
    });
}
