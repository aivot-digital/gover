import {type AnyElement} from '../../models/elements/any-element';
import {isAnyElementWithChildren} from '../../models/elements/any-element-with-children';
import {ElementChildOptions, type ElementDisplayContext} from '../../data/element-type/element-child-options';
import {flattenElements, flattenElementsWithParents, type ElementWithParents} from '../../utils/flatten-elements';
import {deepEquals} from '../../utils/equality-utils';
import {keyById, threeWayMerge} from '../../utils/three-way-merge';

/** An edit becomes part of the form layout only when the element editor applies it. */
export interface ElementPanelDraft {
    base: AnyElement;
    mine: AnyElement;
    tab: string;
    automaticDataKey: boolean;
    dataKeyUnlocked: boolean;
    /** The choice to reinsert an element removed from the stored layout is pending until Apply. */
    detached?: boolean;
    /** Current bases for edits to descendants which have since moved to another branch of the form. */
    movedDescendantBases?: AnyElement[];
    parents: {element: AnyElement; childIndex: number}[];
}

type PanelPlacement<T> = {tree: T; affectedId: string} | {error: string};

/** Used for both the conflict preview and Apply. A deleted element is kept outside the layout until then. */
export function applyPanelDraftToTree<T extends AnyElement>(
    root: T,
    draft: ElementPanelDraft,
    updated: AnyElement,
    displayContext: ElementDisplayContext,
): PanelPlacement<T> {
    const entries = flattenElementsWithParents(root, []);
    const target = draft.parents.length === 0 ? entries[0] : entries.find(({element}) => element.id === draft.base.id);
    const canContain = (parent: AnyElement, child: AnyElement) => isAnyElementWithChildren(parent) &&
        ElementChildOptions[displayContext][parent.type]?.includes(child.type) === true;
    const invalidPlacement = {error: 'Ein Formularelement dieses Typs kann hier nicht eingefügt werden. Prüfen Sie den Elementtyp im Reiter „Elementstruktur“.'};
    const duplicateIds = {error: 'Mehrere Formularelemente haben dieselbe ID. Prüfen Sie die IDs im Reiter „Elementstruktur“.'};
    if (target == null && !draft.detached) return invalidPlacement;

    // Retain the server location of descendants moved outside the edited subtree, including when
    // the old container itself has been replaced with a different type.
    const outside = new Map(entries.filter((entry) => entry !== target &&
        (target == null || !entry.parents.includes(target.element))).map(({element}) => [element.id, element]));
    if (outside.has(updated.id)) return duplicateIds;
    const bases = new Map([...flattenElements(draft.base), ...draft.movedDescendantBases ?? []].map((item) => [item.id, item]));
    const movedUpdates: AnyElement[] = [];
    let movedConflict = false;
    function atOriginalLocation(element: AnyElement): AnyElement {
        if (!isAnyElementWithChildren(element) || element.children == null) return element;
        const children: AnyElement[] = [];
        for (const child of element.children) {
            const stored = outside.get(child.id);
            const base = bases.get(child.id);
            if (stored == null || base == null) {
                children.push(atOriginalLocation(child));
            } else if (!deepEquals(base, child)) {
                // An applied structure can edit the moved child itself. Keep those property edits at its new position.
                const merged = threeWayMerge(base, child, stored, {arrayKey: keyById});
                if (merged.conflicts.length > 0) movedConflict = true;
                else if (!deepEquals(merged.merged, stored)) movedUpdates.push(merged.merged);
            }
        }
        return {...element, children} as AnyElement;
    }
    let branch = atOriginalLocation(updated);
    if (movedConflict) return {error: 'Ein verschobenes Formularelement wurde zwischenzeitlich geändert. Prüfen Sie seine Einstellungen im Reiter „Elementstruktur“.'};
    let affectedId = branch.id;
    let anchor = target;
    if (anchor == null) {
        for (let index = draft.parents.length - 1; index >= 0; index--) {
            const {element: shell, childIndex} = draft.parents[index];
            const entry = index === 0 ? entries[0] : entries.find(({element}) => element.id === shell.id);
            if (entry != null && canContain(entry.element, branch) && isAnyElementWithChildren(entry.element)) {
                const children = [...entry.element.children ?? []];
                children.splice(childIndex, 0, branch);
                branch = {...entry.element, children} as AnyElement;
                anchor = entry;
                break;
            }
            if (!canContain(shell, branch)) return invalidPlacement;
            const children = entry != null && isAnyElementWithChildren(entry.element)
                ? (entry.element.children ?? []).filter((child) => canContain(shell, child)) : [];
            children.splice(childIndex, 0, branch);
            branch = {...shell, children} as AnyElement;
            affectedId = shell.id;
            if (entry != null) {
                anchor = entry;
                break;
            }
        }
    }
    if (anchor == null) return invalidPlacement;
    const parent = anchor.parents.at(-1);
    if (branch.type !== anchor.element.type && (parent == null || !canContain(parent, branch))) return invalidPlacement;
    let tree = replaceAtLocation(root, anchor, branch);
    for (const update of movedUpdates) {
        const moved = flattenElementsWithParents(tree, []).find(({element}) => element.id === update.id);
        if (moved == null) return invalidPlacement;
        tree = replaceAtLocation(tree, moved, update);
    }
    const ids = flattenElements(tree).map((element) => element.id);
    return new Set(ids).size === ids.length ? {tree, affectedId: movedUpdates.length > 0 ? tree.id : affectedId} : duplicateIds;
}

function replaceAtLocation<T extends AnyElement>(root: T, location: ElementWithParents, change: AnyElement): T {
    let before = location.element;
    let after = change;
    for (const parent of [...location.parents].reverse()) {
        if (!isAnyElementWithChildren(parent)) return root;
        after = {...parent, children: parent.children?.map((child) => child === before ? after : child)} as AnyElement;
        before = parent;
    }
    return after as T;
}

/** Keep only the ancestry needed to reinsert a removed element, without copying unrelated branches. */
export function getPanelDraftParents(parents: AnyElement[], targetId: string): ElementPanelDraft['parents'] {
    return parents.map((element, index) => {
        const childId = parents[index + 1]?.id ?? targetId;
        const childIndex = isAnyElementWithChildren(element)
            ? element.children?.findIndex((child) => child.id === childId) ?? 0 : 0;
        return {element: {...element, children: []} as AnyElement, childIndex: Math.max(0, childIndex)};
    });
}
