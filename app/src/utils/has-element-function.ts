import {AnyElement} from '../models/elements/any-element';
import {isStringNotNullOrEmpty} from "./string-utils";

export function hasElementFunction(comp: AnyElement): boolean {
    return (
        (comp.isVisible != null && isStringNotNullOrEmpty(comp.isVisible.requirements)) ||
        (comp.patchElement != null && isStringNotNullOrEmpty(comp.patchElement.requirements))
        // TODO: Check func
    );
}
