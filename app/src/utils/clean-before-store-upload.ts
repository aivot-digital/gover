import {type AnyElement} from '../models/elements/any-element';
import {isAnyElementWithChildren} from '../models/elements/any-element-with-children';

export function cleanBeforeStoreUpload<T extends AnyElement>(e: T): T {
    const cleaned: T = {
        ...e,
        testProtocolSet: undefined,
    };

    if (isAnyElementWithChildren(cleaned)) {
        // @ts-expect-error
        cleaned.children = cleaned.children.map(cleanBeforeStoreUpload);
    }

    return cleaned;
}
