import {type FormLayoutElement} from '../../../models/elements/form-layout-element';
import {type AnyElement} from '../../../models/elements/any-element';
import {type MergeConflictPresentation, type MergeSpec} from '../../../utils/three-way-merge';
import {normalizeUiDefinitionForStorage} from '../../../utils/ui-definition-utils';
import {flattenElements} from '../../../utils/flatten-elements';
import {getElementName} from '../../../data/element-type/element-names';
import {describeElementProperty} from './element-conflict-labels';
import {elementTreeMergeRules, isElementTree} from './element-tree-structure-merge';

export const formLayoutMergeSpec: MergeSpec<FormLayoutElement | null> = {
    ...elementTreeMergeRules,
    normalize: normalizeUiDefinitionForStorage,
    validate: (layout) => layout == null || hasUniqueElementIds(layout),
    label: (path, {base, mine, theirs}) => describeElementPath(path, [mine, theirs, base], 'Gesamtes Formular'),
    describeConflict: (path, {base, mine, theirs}) => describeElementConflict(path, [mine, theirs, base], 'Formular'),
};

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
    if (path.length === 0) return rootLabel;
    return formatElementConflictLabel(describeElementConflict(path, trees, rootLabel));
}

/** Groups by element IDs so equal or changed titles never merge unrelated decisions. */
export function describeElementConflict(
    path: string[],
    trees: unknown[],
    rootLabel: string,
    groupPrefix?: (string | number)[],
): MergeConflictPresentation {
    const roots = trees.filter(isElementTree);
    const elements = roots.flatMap((tree) => flattenElements(tree));
    const prefix = groupPrefix ?? ['form', roots[0]?.id ?? 'root'];

    for (let index = path.length - 1; index >= 0; index--) {
        if (path[index - 1] !== 'children') continue;
        const itemId = /^\[(.+)]$/.exec(path[index])?.[1];
        const element = itemId == null ? undefined : elements.find((candidate) => candidate.id === itemId);
        if (element != null) {
            const rest = path.slice(index + 1);
            return {
                group: {
                    id: JSON.stringify([...prefix, element.id]),
                    label: groupPrefix == null ? describeElement(element) : `${rootLabel} › ${describeElement(element)}`,
                },
                propertyLabel: rest.length === 0 ? 'Gesamtes Element' : describeElementProperty(rest, elements.filter((candidate) => candidate.id === element.id), element),
            };
        }
    }

    return {
        group: {id: JSON.stringify(prefix), label: rootLabel},
        propertyLabel: path.length === 0 ? 'Gesamtes Formular' : describeElementProperty(path, roots, roots[0]),
    };
}

export function formatElementConflictLabel(presentation: MergeConflictPresentation): string {
    return presentation.propertyLabel === 'Gesamtes Element' ? presentation.group.label :
        `${presentation.group.label}, Eigenschaft „${presentation.propertyLabel}“`;
}

function describeElement(element: AnyElement): string {
    const label = [(element as {label?: string | null}).label, element.name]
        .find((value) => value != null && value.trim().length > 0);
    const typeName = getElementName(element) ?? 'Formularelement';
    return label != null ? `${typeName} „${label.trim()}“` : typeName;
}
