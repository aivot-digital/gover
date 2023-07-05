import {getFunctionStatus} from './function-status-utils';
import {AnyElement} from '../models/elements/any-element';

export function isElementTested(element: AnyElement): boolean {
    return element.testProtocolSet != null && (getFunctionStatus(element).length === 0 || element.testProtocolSet.technicalTest != null);
}
