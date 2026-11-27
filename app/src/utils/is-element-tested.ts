import {getFunctionStatus} from './function-status-utils';
import {type AnyElement} from '../models/elements/any-element';

export function isElementTested(element: AnyElement | null | undefined): boolean {
    if (!element) {
        return false;
    }
    return element.testProtocolSet?.professionalTest != null && (getFunctionStatus(element).length === 0 || element.testProtocolSet.technicalTest != null);
}
