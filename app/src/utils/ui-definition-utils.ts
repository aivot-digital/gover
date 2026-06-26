import {type AnyElement} from '../models/elements/any-element';
import {isAnyElementWithChildren} from '../models/elements/any-element-with-children';

export function normalizeUiDefinitionForStorage<T extends AnyElement>(
    value: T | null | undefined
): T | null {
    if (value == null) {
        return null;
    }

    if (isAnyElementWithChildren(value) && (value.children?.length ?? 0) === 0) {
        return null;
    }

    return value;
}
