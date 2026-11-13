import {type FormLayoutElement} from '../../../models/elements/form-layout-element';
import {type AnyElement} from '../../../models/elements/any-element';
import {keyById, type MergeSpec, ORDER_SEGMENT} from '../../../utils/three-way-merge';
import {normalizeUiDefinitionForStorage} from '../../../utils/ui-definition-utils';
import {flattenElements} from '../../../utils/flatten-elements';
import {getElementName} from '../../../data/element-type/element-names';

export const formLayoutMergeSpec: MergeSpec<FormLayoutElement | null> = {
    // Elements are merged by their ID, so changes to different elements of the same form can be combined.
    arrayKey: keyById,
    normalize: normalizeUiDefinitionForStorage,
    validate: (layout) => layout == null || hasUniqueElementIds(layout),
    label: (path, {base, mine, theirs}) => describeElementPath(path, [mine, theirs, base], 'Gesamtes Formular'),
};

export function isElementTree(value: unknown): value is AnyElement {
    return value != null &&
        typeof value === 'object' &&
        typeof (value as Partial<AnyElement>).id === 'string' &&
        typeof (value as Partial<AnyElement>).type === 'number';
}

/** A merge must never produce an element twice, e.g. one that one side moved and the other side changed. */
export function hasUniqueElementIds(root: AnyElement): boolean {
    const ids = flattenElements(root).map((element) => element.id);
    return new Set(ids).size === ids.length;
}

/**
 * Names the element that a merge path within an element tree points to, e.g. `Textfeld „Vorname“`.
 * Paths that end at a property of the element name the property as well.
 */
export function describeElementPath(path: string[], trees: unknown[], rootLabel: string): string {
    const elements = trees
        .filter(isElementTree)
        .flatMap((tree) => flattenElements(tree));

    for (let index = path.length - 1; index >= 0; index--) {
        const itemId = /^\[(.+)]$/.exec(path[index])?.[1];
        const element = itemId == null ? undefined : elements.find((candidate) => candidate.id === itemId);
        if (element != null) {
            return describeRest(describeElement(element), path.slice(index + 1));
        }
    }

    return describeRest(rootLabel, path);
}

function describeElement(element: AnyElement): string {
    const label = (element as {label?: string | null}).label ?? element.name;
    const typeName = getElementName(element);
    return label != null && label.trim().length > 0 ? `${typeName} „${label.trim()}“` : typeName;
}

function describeRest(subject: string, rest: string[]): string {
    if (rest.length === 0) {
        return subject;
    }
    if (rest[rest.length - 1] === ORDER_SEGMENT) {
        return `Reihenfolge der Elemente in ${subject}`;
    }
    return `${subject}, Eigenschaft „${rest.filter((segment) => !segment.startsWith('[')).join('.')}“`;
}
