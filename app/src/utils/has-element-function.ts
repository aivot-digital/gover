import {isNullOrEmpty} from './is-null-or-empty';
import {AnyElement} from '../models/elements/any-element';

export function hasElementFunction(comp: AnyElement): boolean {
    return (
        (comp.isVisible != null && !isNullOrEmpty(comp.isVisible.functionName)) ||
        (comp.validate != null && !isNullOrEmpty(comp.validate.functionName)) ||
        (comp.patchElement != null && !isNullOrEmpty(comp.patchElement.functionName))
    );
}
