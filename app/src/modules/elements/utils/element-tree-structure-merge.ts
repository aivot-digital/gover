import {getElementName} from '../../../data/element-type/element-names';
import type {AnyElement} from '../../../models/elements/any-element';
import {isAnyElementWithChildren} from '../../../models/elements/any-element-with-children';
import {flattenElementsWithParents, type ElementWithParents} from '../../../utils/flatten-elements';
import {keyById, type MergeChoice, type MergeConflict, type MergeResult, pathToId, type SubtreeMergeInput} from '../../../utils/three-way-merge';
import {editorValuesEqual, variableReferenceMergeRules} from '../../../utils/variable-reference-merge';

export const elementTreeMergeRules = {
    ...variableReferenceMergeRules,
    arrayKey: keyById,
    mergeSubtree: (input: SubtreeMergeInput) => input.path.length === 0 ? mergeElementTreeStructure(input) : undefined,
};

export function isElementTree(value: unknown): value is AnyElement {
    return value != null && typeof value === 'object' && 'id' in value && typeof value.id === 'string' &&
        'type' in value && typeof value.type === 'number';
}

function containerName(element: AnyElement): string {
    const titled = element as AnyElement & {label?: string; title?: string};
    return titled.label?.trim() || titled.title?.trim() || element.name?.trim() || getElementName(element) || element.id;
}

export function elementContainerPath(parents: readonly AnyElement[]): string {
    return parents.map((element, index) => {
        const name = containerName(element);
        const parent = parents[index - 1];
        if (parent && isAnyElementWithChildren(parent) && parent.children?.some((sibling) =>
            sibling.id !== element.id && containerName(sibling) === name)) {
            return `${name} (${element.id})`;
        }
        return name;
    }).join(' › ');
}

function indexTree(root: unknown) {
    const entries = isElementTree(root) ? flattenElementsWithParents(root, []) : [];
    const byId = new Map(entries.map((entry) => [entry.element.id, entry]));
    return {byId, duplicate: entries.length !== byId.size};
}

const parentOf = (entry?: ElementWithParents) => entry?.parents.at(-1);

function samePlacement(left?: ElementWithParents, right?: ElementWithParents) {
    return parentOf(left)?.id === parentOf(right)?.id && editorValuesEqual(left?.element, right?.element);
}

function removeElements(root: unknown, ids: Set<string>): unknown {
    if (!isElementTree(root)) return root;
    const visit = (element: AnyElement): AnyElement => {
        if (!isAnyElementWithChildren(element) || !element.children?.length) return element;
        const children = element.children.filter((child) => !ids.has(child.id)).map(visit);
        return children.length === element.children.length && children.every((child, i) => child === element.children![i])
            ? element : {...element, children} as AnyElement;
    };
    return visit(root);
}

interface Placement {
    entry: ElementWithParents;
    movingSide: MergeChoice;
    selected: MergeChoice;
    automaticRemoval: boolean;
    conflict?: MergeConflict;
}

/** Merge regular properties and sibling edits first, then anchor the chosen elements to their surviving neighbours. */
function insertElements(root: AnyElement, placements: Placement[]): AnyElement {
    const insertions = placements.filter(({movingSide, selected}) => movingSide === selected);
    const visit = (element: AnyElement): AnyElement => {
        if (!isAnyElementWithChildren(element)) return element;
        const original = element.children ?? [];
        const children = original.map(visit);
        const present = new Set(children.map((child) => child.id));
        for (const side of ['mine', 'theirs'] as const) {
            const atParent = insertions.filter((item) => item.movingSide === side && parentOf(item.entry)?.id === element.id);
            if (!atParent.length) continue;
            const source = parentOf(atParent[0].entry)!;
            if (!isAnyElementWithChildren(source)) continue;
            const siblings = source.children ?? [];
            const insertIds = new Set(atParent.map((item) => item.entry.element.id));
            let previous: string | undefined;
            const block: AnyElement[] = [];
            const insertBlock = (next?: string) => {
                if (!block.length) return;
                const left = children.findIndex((sibling) => sibling.id === previous);
                const right = children.findIndex((sibling) => sibling.id === next);
                // Even when another edit reverses the two surviving anchors, keep the moved block between them.
                let position = children.length;
                if (left >= 0) position = (right < 0 ? left : Math.min(left, right)) + 1;
                else if (right >= 0) position = right;
                children.splice(position, 0, ...block);
                for (const child of block) present.add(child.id);
                block.length = 0;
            };
            for (const child of siblings) {
                if (insertIds.has(child.id)) {
                    block.push(child);
                } else if (present.has(child.id)) {
                    insertBlock(child.id);
                    previous = child.id;
                }
            }
            insertBlock();
        }
        return children.length === original.length && children.every((child, i) => child === original[i])
            ? element : {...element, children} as AnyElement;
    };
    return visit(root);
}

function treeChoice(input: SubtreeMergeInput): MergeResult<unknown> {
    const {path, base, mine, theirs} = input;
    const id = pathToId(path);
    const choice = input.choices[id];
    return {merged: choice === 'mine' ? mine : theirs, conflicts: choice ? [] : [{id, path, base, mine, theirs}]};
}

/** Resolve cross-parent moves competing with a deletion only inside this declared UI definition. */
export function mergeElementTreeStructure(input: SubtreeMergeInput): MergeResult<unknown> | undefined {
    const {base, mine, theirs, interruptedWrite} = input;
    if (!isElementTree(mine) || !isElementTree(theirs)) return undefined;
    // Entire matching versions already select the right side, including a PUT that actually committed.
    if (editorValuesEqual(mine, theirs) || editorValuesEqual(theirs, base) ||
        (interruptedWrite && editorValuesEqual(theirs, interruptedWrite.value)) ||
        (editorValuesEqual(mine, base) && (!interruptedWrite || editorValuesEqual(base, interruptedWrite.value)))) return undefined;

    const original = indexTree(base);
    const local = indexTree(mine);
    const stored = indexTree(theirs);
    const submitted = indexTree(interruptedWrite?.value);
    const indexes = [original, local, stored, submitted];
    const placements = new Map<string, Placement>();
    for (const id of new Set([...original.byId.keys(), ...submitted.byId.keys()])) {
        const own = local.byId.get(id);
        const remote = stored.byId.get(id);
        if ((own == null) === (remote == null)) continue;
        const entry = (own ?? remote)!;
        const destination = parentOf(entry);
        if (!destination) continue;
        const before = original.byId.get(id);
        const sent = submitted.byId.get(id);
        const pastParents = [parentOf(before), parentOf(sent)];
        if (!pastParents.some((parent) => parent && parent.id !== destination.id)) continue;

        const movingSide = own ? 'mine' : 'theirs';
        let selected: MergeChoice | undefined;
        if (samePlacement(own, before) && (!interruptedWrite || samePlacement(sent, before))) selected = 'theirs';
        else if (samePlacement(remote, before) || (interruptedWrite && samePlacement(remote, sent))) selected = 'mine';
        const automaticRemoval = selected != null && selected !== movingSide;

        let conflict: MergeConflict | undefined;
        if (selected == null) {
            const otherTree = own ? stored : local;
            const otherDestination = otherTree.byId.get(destination.id)?.element;
            // Keeping the move would also resurrect or retype its target. That requires a choice for the tree.
            if (otherDestination ? otherDestination.type !== destination.type :
                original.byId.has(destination.id) || submitted.byId.has(destination.id)) return treeChoice(input);
            const choiceId = JSON.stringify(['element-placement', input.path, id]);
            const path = [...input.path, ...[...entry.parents.slice(1), entry.element].flatMap((element) => ['children', `[${element.id}]`])];
            selected = input.choices[choiceId];
            if (!selected) conflict = {
                id: choiceId, path, base: before?.element, mine: own?.element, theirs: remote?.element,
            };
        }
        placements.set(id, {entry, selected: selected ?? 'theirs', movingSide, automaticRemoval, conflict});
    }
    if (!placements.size) return undefined;
    const ids = new Set(placements.keys());
    if (indexes.some((index) => index.duplicate) || [base, theirs, interruptedWrite?.value].some((root) =>
        isElementTree(root) && (root.id !== mine.id || root.type !== mine.type))) return treeChoice(input);

    // A descendant that also moved independently cannot belong to a whole-subtree choice and another location.
    for (const {byId} of indexes) {
        for (const {element, parents} of byId.values()) {
            const ancestor = parents.find((parent) => placements.get(parent.id)?.automaticRemoval === false);
            if (ancestor && (ids.has(element.id) || indexes.some((index) => {
                const other = index.byId.get(element.id);
                return other && !other.parents.some((parent) => parent.id === ancestor.id);
            }))) return treeChoice(input);
        }
    }

    const rest = input.mergeRest({
        base: removeElements(base, ids), mine: removeElements(mine, ids), theirs: removeElements(theirs, ids),
        interruptedWrite: interruptedWrite && {value: removeElements(interruptedWrite.value, ids)},
    });
    if (!isElementTree(rest.merged)) return treeChoice(input);
    const remaining = indexTree(rest.merged);
    if (remaining.duplicate) return treeChoice(input);
    const conflicts = [...rest.conflicts];
    for (const {entry, selected, movingSide, conflict} of placements.values()) {
        if (selected !== movingSide && !conflict) continue;
        const target = parentOf(entry)!;
        const destination = remaining.byId.get(target.id);
        if (destination?.element.type !== target.type) return treeChoice(input);
        if (conflict) {
            const container = elementContainerPath([...destination.parents, destination.element]);
            conflicts.push({...conflict, elementPlacement: {elementId: entry.element.id, [movingSide]: container}});
        }
    }
    return {
        merged: insertElements(rest.merged, [...placements.values()]),
        conflicts,
    };
}
