import {isNullOrEmpty} from './is-null-or-empty';
import {AnyElement} from '../models/elements/any-element';

export function hasElementFunction(comp: AnyElement): boolean {
    return (
        (comp.visibility != null && !isNullOrEmpty(comp.visibility.functionName)) ||
        (comp.validate != null && !isNullOrEmpty(comp.validate.functionName)) ||
        (comp.patch != null && !isNullOrEmpty(comp.patch.functionName))
    );
}
