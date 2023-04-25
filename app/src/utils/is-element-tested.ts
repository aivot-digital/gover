import {hasElementFunction} from './has-element-function';
import {AnyElement} from '../models/elements/any-element';

export function isElementTested(element: AnyElement): boolean {
    return element.testProtocolSet != null && (!hasElementFunction(element) || element.testProtocolSet.technicalTest != null);
}
