import {isNullOrEmpty} from './is-null-or-empty';
import {AnyElement} from '../models/elements/any-element';

export function hasElementFunctionRequirement(comp: AnyElement): boolean {
    return (
        (comp.visibility != null && !isNullOrEmpty(comp.visibility.requirements) && isNullOrEmpty(comp.visibility.functionName)) ||
        (comp.validate != null && !isNullOrEmpty(comp.validate.requirements) && isNullOrEmpty(comp.validate.functionName)) ||
        (comp.patch != null && !isNullOrEmpty(comp.patch.requirements) && isNullOrEmpty(comp.patch.functionName))
    );
}
