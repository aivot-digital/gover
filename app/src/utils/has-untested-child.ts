import {isElementTested} from './is-element-tested';
import {type AnyElement} from '../models/elements/any-element';
import {type AnyElementWithChildren, isAnyElementWithChildren} from '../models/elements/any-element-with-children';
import {ElementType} from '../data/element-type/element-type';
import {isRootElement} from '../models/elements/root-element';

export function hasUntestedChild(element: AnyElementWithChildren): boolean {
    if (element.type === ElementType.Container && element.storeLink != null) {
        return false;
    }

    if (isRootElement(element)) {
        if (!isElementTested(element.introductionStep)) {
            return true;
        }
        if (!isElementTested(element.summaryStep)) {
            return true;
        }
        if (!isElementTested(element.submitStep)) {
            return true;
        }
    }

    return element
        .children
        .some((child: AnyElement) => {
            return !isElementTested(child) || (isAnyElementWithChildren(child) && hasUntestedChild(child));
        });
}
