import {type LegacyNoCodeMigrations} from '../../modules/nocode/hooks/use-legacy-no-code-migration';
import {type AnyElement} from '../../models/elements/any-element';
import {isAnyElementWithChildren} from '../../models/elements/any-element-with-children';
import {ElementChildOptions, type ElementDisplayContext} from '../../data/element-type/element-child-options';
import {flattenElements, flattenElementsWithParents, type ElementWithParents} from '../../utils/flatten-elements';
import {threeWayMerge} from '../../utils/three-way-merge';
import {editorValuesEqual} from '../../utils/variable-reference-merge';
import {elementTreeMergeRules} from '../../modules/elements/utils/element-tree-structure-merge';

/** An edit becomes part of the form layout only when the element editor applies it. */
export interface ElementPanelDraft {
    base: AnyElement;
    mine: AnyElement;
    tab: string;
    automaticDataKey: boolean;
    dataKeyUnlocked: boolean;
    /** Retain the old rule for cancellation while the replacement is still an unapplied draft. */
    legacyMigrations?: LegacyNoCodeMigrations;
    /** The choice to reinsert an element removed from the stored layout is pending until Apply. */
    detached?: boolean;
    /** Current bases for edits to descendants which have since moved to another branch of the form. */
    movedDescendantBases?: AnyElement[];
    /** Keep an agreed removal outside the open group pending until Apply. */
    pendingMovedRemovals?: {element: AnyElement; parentId: string}[];
    parents: {element: AnyElement; childIndex: number}[];
    /** The comparison base of a structural save conflict shown provisionally in the open editor. */
    unconfirmedSaveBase?: Pick<ElementPanelDraft, 'base' | 'parents' | 'detached' | 'movedDescendantBases' | 'pendingMovedRemovals'>;
}

type PanelPlacement<T> = {tree: T; affectedId: string} | {error: string};

/** Detect children removed by the open edit but still present elsewhere, including earlier confirmed removals. */
export function findMovedPanelRemovals(draft: ElementPanelDraft, updated: AnyElement, tree: ElementWithParents[]) {
    if (!isAnyElementWithChildren(draft.base) && !draft.movedDescendantBases?.length && !draft.pendingMovedRemovals?.length) return [];
    const retained = new Set(flattenElements(updated).map((element) => element.id));
    const originalParents = new Map(flattenElementsWithParents(draft.base, []).slice(1)
        .map(({element, parents}) => [element.id, parents.at(-1)!.id]));
    const moved = new Set(draft.movedDescendantBases?.map((element) => element.id));
    const pending = new Map(draft.pendingMovedRemovals?.map((removal) => [removal.element.id, removal]));
    return tree.flatMap(({element, parents}) => {
        const parentId = parents.at(-1)?.id;
        if (parentId == null || retained.has(element.id)) return [];
        const prior = pending.get(element.id);
        if (prior == null) {
            if (parents.some((parent) => parent.id === draft.base.id)) return [];
            const originalParent = originalParents.get(element.id);
            if (!moved.has(element.id) && (originalParent == null || originalParent === parentId)) return [];
        }
        return [{element, parents, parentId, confirmed: prior?.parentId === parentId && editorValuesEqual(element, prior.element)}];
    });
}

/** Replace or remove children by ID without changing the identities of unrelated branches. */
export function updatePanelDescendants<T extends AnyElement>(element: T, changes: Map<string, AnyElement | undefined>): T {
    if (changes.size === 0 || !isAnyElementWithChildren(element) || element.children == null) return element;
    const children = element.children.flatMap((item) => {
        const next = changes.has(item.id) ? changes.get(item.id) : updatePanelDescendants(item, changes);
        return next == null ? [] : [next];
    });
    return children.length === element.children.length && children.every((child, i) => child === element.children![i])
        ? element : {...element, children};
}

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
    const removals = new Map<string, undefined>();
    let movedConflict = false;
    if (target && draft.base.type === updated.type && updated.type === target.element.type &&
        isAnyElementWithChildren(updated) && !editorValuesEqual(draft.base, target.element)) {
        // A stale Apply must not replace the entire group before its internal placement conflict was resolved.
        const {conflicts} = threeWayMerge(draft.base, updated, target.element, elementTreeMergeRules);
        movedConflict = conflicts.some((conflict) => conflict.elementPlacement != null || conflict.path.length === 0);
    }
    for (const removal of findMovedPanelRemovals(draft, updated, entries)) {
        if (removal.parents.some((parent) => removals.has(parent.id))) continue;
        if (!removal.confirmed) movedConflict = true;
        else removals.set(removal.element.id, undefined);
    }
    function atOriginalLocation(element: AnyElement): AnyElement {
        if (!isAnyElementWithChildren(element) || element.children == null) return element;
        const children: AnyElement[] = [];
        for (const child of element.children) {
            const stored = outside.get(child.id);
            const base = bases.get(child.id);
            if (stored == null || base == null) {
                children.push(atOriginalLocation(child));
            } else if (!editorValuesEqual(base, child)) {
                // An applied structure can edit the moved child itself. Keep those property edits at its new position.
                const merged = threeWayMerge(base, child, stored, elementTreeMergeRules);
                if (merged.conflicts.length > 0) movedConflict = true;
                else if (!editorValuesEqual(merged.merged, stored)) movedUpdates.push(merged.merged);
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
    if (removals.size > 0) tree = updatePanelDescendants(tree, removals);
    const ids = flattenElements(tree).map((element) => element.id);
    return new Set(ids).size === ids.length ? {tree, affectedId: movedUpdates.length > 0 || removals.size > 0 ? tree.id : affectedId} : duplicateIds;
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
