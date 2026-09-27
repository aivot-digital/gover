import {AnyFormElement} from '../models/elements/form/any-form-element';

export function normalizeSummaryLayoutElement(element: AnyFormElement): AnyFormElement {
    const normalizedElement = {
        ...element,
        weight: 12,
    } as AnyFormElement & { children?: AnyFormElement[] | null };

    if ('children' in element) {
        normalizedElement.children = Array.isArray(element.children)
            ? element.children.map(normalizeSummaryLayoutElement)
            : element.children;
    }

    return normalizedElement;
}
