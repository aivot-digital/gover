import {hasElementFunction} from './has-element-function';
import {AnyElement} from '../models/elements/any-element';

export function isElementTested(element: AnyElement): boolean {
    return element.professionalTest != null && (!hasElementFunction(element) || element.technicalTest != null);
}
