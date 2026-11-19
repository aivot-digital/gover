import {applyPanelDraftToTree, getPanelDraftParents, type ElementPanelDraft} from '../../../components/element-tree-2/element-panel-draft';
import {ElementDisplayContext} from '../../../data/element-type/element-child-options';
import {prepareDependentMerge, prepareMerge, type PreparedMerge} from '../../../hooks/use-merge-conflict-resolution';
import {type AnyElement} from '../../../models/elements/any-element';
import {isAnyElementWithChildren} from '../../../models/elements/any-element-with-children';
import {type FormLayoutElement} from '../../../models/elements/form-layout-element';
import {deepEquals} from '../../../utils/equality-utils';
import {flattenElements, flattenElementsWithParents} from '../../../utils/flatten-elements';
import {normalizeElementDefinition} from '../../../utils/normalize-element-definition';
import {keyById, type MergeChoices, type MergeSpec} from '../../../utils/three-way-merge';
import {normalizeUiDefinitionForStorage} from '../../../utils/ui-definition-utils';
import {describeElementConflict, formatElementConflictLabel, formLayoutMergeSpec, hasUniqueElementIds} from './form-layout-merge-spec';

interface FormRecoveryState {
    layout: FormLayoutElement | null;
    panel?: ElementPanelDraft;
}

interface FormDraftMergeRequest {
    base: FormLayoutElement | null;
    mine: FormLayoutElement | null;
    theirs: FormLayoutElement | null;
    editableRoot: FormLayoutElement;
    panel?: ElementPanelDraft;
}

// Decisions about an applied form property and a later, still open edit to it have separate identities.
function mergeLayer<T>(layer: string, base: T, mine: T, theirs: T, spec: MergeSpec<T>, choices: MergeChoices): PreparedMerge<T> {
    const prefix = layer + '/';
    const layerChoices = Object.fromEntries(Object.entries(choices)
        .filter(([id]) => id.startsWith(prefix))
        .map(([id, choice]) => [id.slice(prefix.length), choice]));
    const result = prepareMerge({base, mine, theirs, spec}, layerChoices);
    return {...result, conflicts: result.conflicts.map((conflict) => ({...conflict, id: prefix + conflict.id}))};
}

function findInLayout(layout: FormLayoutElement | null, id: string) {
    return layout == null ? undefined : flattenElements(layout).find((item) => item.id === id);
}

function elementMergeSpec(base: AnyElement, mine: AnyElement, theirs: AnyElement): MergeSpec<AnyElement | undefined> {
    return {
        arrayKey: keyById,
        normalize: normalizeElementDefinition,
        validate: (value) => value == null || hasUniqueElementIds(value),
        describeConflict: (path) => describeElementConflict(
            ['children', '[' + base.id + ']', ...path], [mine, theirs, base], 'Offenes Formularelement', ['panel', base.id]),
    };
}

/** Replace or remove moved children without changing the identities of unrelated element subtrees. */
function updateDescendants(element: AnyElement, changes: Map<string, AnyElement | undefined>): AnyElement {
    if (!isAnyElementWithChildren(element) || element.children == null) return element;
    const children = element.children.flatMap((item) => {
        const next = changes.has(item.id) ? changes.get(item.id) : updateDescendants(item, changes);
        return next == null ? [] : [next];
    });
    return children.length === element.children.length && children.every((child, i) => child === element.children![i])
        ? element : {...element, children} as AnyElement;
}

/** A move on the server keeps the child at its new location. Its pending property changes follow it on Apply. */
function rebaseMovedChildren(panel: ElementPanelDraft, root: FormLayoutElement | null, target: AnyElement | undefined, choices: MergeChoices) {
    const conflicts: PreparedMerge<unknown>['conflicts'] = [];
    if (root == null || !isAnyElementWithChildren(panel.mine)) return {panel, conflicts};
    const originals = new Map([...flattenElements(panel.base).slice(1), ...panel.movedDescendantBases ?? []].map((item) => [item.id, item]));
    const outside = new Map(flattenElements(root).map((item) => [item.id, item]));
    if (target != null) for (const item of flattenElements(target)) outside.delete(item.id);
    const remove = new Map<string, undefined>();
    const edits = new Map<string, AnyElement | undefined>();
    const movedDescendantBases = [];
    for (const {element, parents} of flattenElementsWithParents(panel.mine, []).slice(1)) {
        // The merge of a moved subtree includes its descendants.
        if (parents.some((parent) => remove.has(parent.id))) continue;
        const original = originals.get(element.id);
        const saved = outside.get(element.id);
        if (original == null || saved == null) continue;
        remove.set(element.id, undefined);
        const result = mergeLayer('element/moved/' + element.id, original, element, saved, elementMergeSpec(original, element, saved), choices);
        conflicts.push(...result.conflicts);
        edits.set(element.id, result.changed ? result.value : undefined);
        if (result.changed) movedDescendantBases.push(saved);
    }
    if (remove.size === 0) return {panel, conflicts};
    return {
        panel: {...panel, base: updateDescendants(panel.base, remove), mine: updateDescendants(panel.mine, edits), movedDescendantBases},
        conflicts,
    };
}

function restoreRemovedElement(
    form: PreparedMerge<FormRecoveryState>,
    request: FormDraftMergeRequest,
    panel: ElementPanelDraft,
    choices: MergeChoices,
): PreparedMerge<FormRecoveryState> {
    if (!panel.detached && deepEquals(panel.mine, panel.base)) return form;
    const detached = {...panel, detached: true};
    // Reuse the exact Apply projection to present the full structural choice, e.g. when reinserting
    // the field would also replace an ancestor that now has an incompatible type.
    const projection = applyPanelDraftToTree(form.value.layout ?? request.editableRoot, detached, panel.mine, ElementDisplayContext.CustomerFacing);
    const id = 'error' in projection ? panel.base.id : projection.affectedId;
    const mine = 'error' in projection ? panel.mine : findInLayout(projection.tree, id) ?? panel.mine;
    const theirs = findInLayout(form.value.layout, id);
    const choiceId = 'element/reinsert/' + id;
    const previouslyConfirmed = panel.detached && deepEquals(theirs, findInLayout(request.mine, id));
    if (!previouslyConfirmed && choices[choiceId] == null) {
        const presentation = describeElementConflict(['children', '[' + id + ']'], [mine, theirs, panel.base], 'Offenes Formularelement', ['panel', panel.base.id]);
        return {
            ...form,
            conflicts: [{id: choiceId, ...presentation, label: formatElementConflictLabel(presentation), mine, theirs}],
        };
    }
    if (choices[choiceId] === 'theirs') return form;
    return {value: {...form.value, panel: detached}, changed: true, conflicts: []};
}

/** Rebase the open edit on the form as it will be *before* the person applies that edit. */
export function mergeFormDraft(request: FormDraftMergeRequest, choices: MergeChoices): PreparedMerge<FormRecoveryState> {
    return prepareDependentMerge((decisions) => mergeFormDraftLayers(request, decisions), choices);
}

function mergeFormDraftLayers(request: FormDraftMergeRequest, choices: MergeChoices): PreparedMerge<FormRecoveryState> {
    const {base, mine, theirs, editableRoot} = request;
    const draft = request.panel;
    const layout = mergeLayer('layout', base, mine, theirs, formLayoutMergeSpec, choices);
    const formResult = {...layout, value: {layout: layout.value}};
    if (draft == null || layout.conflicts.length > 0) return formResult;

    const isRoot = draft.parents.length === 0;
    // An unconfigured form gets a fresh editable root ID on every visit. A *deleted* stored layout
    // still needs a structural conflict, including when the open edit was on its root.
    const root = layout.value ?? (isRoot && (
        (theirs != null && normalizeUiDefinitionForStorage(theirs) == null) ||
        (!draft.detached && normalizeUiDefinitionForStorage(base) == null)
    ) ? editableRoot : null);
    const located = root == null ? undefined : isRoot ? {element: root, parents: []} :
        flattenElementsWithParents(root, []).find(({element}) => element.id === draft.base.id);
    const moved = rebaseMovedChildren(draft, root, located?.element, choices);
    if (moved.conflicts.length > 0) return {...formResult, conflicts: moved.conflicts};
    const panel = moved.panel;
    if (located == null) return restoreRemovedElement(formResult, request, panel, choices);
    const target = located.element;
    let panelBase = panel.base;
    let panelMine = panel.mine;
    if (isRoot && panelBase.id !== target.id) {
        if (panelMine.id === panelBase.id) panelMine = {...panelMine, id: target.id};
        panelBase = {...panelBase, id: target.id};
    }

    // After confirming a deletion conflict, continued editing represents a pending insertion over an absent base.
    const element = mergeLayer('element', panel.detached ? undefined : panelBase, panelMine, target, elementMergeSpec(panelBase, panelMine, target), choices);
    if (element.value == null || !element.changed) {
        return {...formResult, conflicts: element.conflicts};
    }
    return {
        value: {
            layout: layout.value,
            panel: {
                ...panel,
                base: target,
                mine: element.value,
                parents: getPanelDraftParents(located.parents, located.element.id),
                detached: false,
            },
        },
        changed: true,
        conflicts: element.conflicts,
    };
}
