import {applyPanelDraftToTree, getPanelDraftParents, type ElementPanelDraft} from '../../../components/element-tree-2/element-panel-draft';
import {ElementDisplayContext} from '../../../data/element-type/element-child-options';
import {prepareDependentMerge, prepareMerge, prepareScopedMerge, type PreparedMerge} from '../../../hooks/use-merge-conflict-resolution';
import {type AnyElement} from '../../../models/elements/any-element';
import {isAnyElementWithChildren} from '../../../models/elements/any-element-with-children';
import {type FormLayoutElement} from '../../../models/elements/form-layout-element';
import {deepEquals} from '../../../utils/equality-utils';
import {flattenElements, flattenElementsWithParents} from '../../../utils/flatten-elements';
import {normalizeElementDefinition} from '../../../utils/normalize-element-definition';
import {keyById, type MergeChoices, type MergeSpec} from '../../../utils/three-way-merge';
import {normalizeUiDefinitionForStorage} from '../../../utils/ui-definition-utils';
import {type EditorSaveCheckpoint, prepareEditorDraftRecovery, preparePostSaveMerge} from '../../../utils/editor-save-rebase';
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
    saveCheckpoint?: EditorSaveCheckpoint<FormLayoutElement | null>;
}

function mergePanelLayer<T>(layer: string, base: T, mine: T, theirs: T, spec: MergeSpec<T>, choices: MergeChoices, afterSave: boolean): PreparedMerge<T> {
    return prepareScopedMerge(layer, choices, (decisions) => afterSave
        ? preparePostSaveMerge(base, mine, theirs, spec, decisions)
        : prepareMerge({base, mine, theirs, spec}, decisions));
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
function rebaseMovedChildren(panel: ElementPanelDraft, root: FormLayoutElement | null, target: AnyElement | undefined, choices: MergeChoices, afterSave: boolean) {
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
        const result = mergePanelLayer('element/moved/' + element.id, original, element, saved, elementMergeSpec(original, element, saved), choices, afterSave);
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
    const previouslyConfirmed = panel.detached && !request.panel?.unconfirmedSaveBase && deepEquals(theirs, findInLayout(request.mine, id));
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

/** An acknowledgement can change the layout behind a drawer opened after the save click. */
export function rebaseOpenElementDraft(draft: ElementPanelDraft | null, previous: FormLayoutElement, current: FormLayoutElement) {
    if (draft == null || deepEquals(previous, current)) return {draft, needsDecision: draft?.unconfirmedSaveBase != null};
    const request = {base: previous, mine: previous, theirs: current, editableRoot: current, panel: draft};
    const choices: MergeChoices = {};
    let result = mergeFormDraftLayers(request, choices, true);
    const needsDecision = result.conflicts.length > 0;
    while (result.conflicts.length > 0) {
        for (const conflict of result.conflicts) choices[conflict.id] = 'mine';
        result = mergeFormDraftLayers(request, choices, true);
    }
    const panel = result.value.panel;
    if (panel == null || !needsDecision) return {draft: panel ?? null, needsDecision: false};
    const {base, parents, detached, movedDescendantBases} = draft;
    return {draft: {...panel, unconfirmedSaveBase: draft.unconfirmedSaveBase ?? {base, parents, detached, movedDescendantBases}}, needsDecision};
}

function mergeFormDraftLayers(request: FormDraftMergeRequest, choices: MergeChoices, afterSave = false): PreparedMerge<FormRecoveryState> {
    const {base, mine, theirs, editableRoot} = request;
    const layout = prepareScopedMerge('layout', choices, (decisions) =>
        prepareEditorDraftRecovery(request, theirs, formLayoutMergeSpec, decisions));
    const formResult = {...layout, value: {layout: layout.value}};
    if (request.panel == null || layout.conflicts.length > 0) return formResult;

    // A local preview keeps the drawer editable. Only an explicit choice during recovery
    // (or saving the applied panel) confirms replacing the stored structure.
    const {unconfirmedSaveBase, ...visible} = request.panel;
    const draft = unconfirmedSaveBase == null ? visible : {
        ...visible,
        base: unconfirmedSaveBase.base,
        parents: unconfirmedSaveBase.parents,
        detached: unconfirmedSaveBase.detached,
        movedDescendantBases: unconfirmedSaveBase.movedDescendantBases,
    };

    const isRoot = draft.parents.length === 0;
    // An unconfigured form gets a fresh editable root ID on every visit. A *deleted* stored layout
    // still needs a structural conflict, including when the open edit was on its root.
    const root = layout.value ?? (isRoot && (
        (theirs != null && normalizeUiDefinitionForStorage(theirs) == null) ||
        (!visible.detached && normalizeUiDefinitionForStorage(base) == null)
    ) ? editableRoot : null);
    const located = root == null ? undefined : isRoot ? {element: root, parents: []} :
        flattenElementsWithParents(root, []).find(({element}) => element.id === draft.base.id);
    const usePostSaveMerge = afterSave || (unconfirmedSaveBase != null && deepEquals(request.theirs, request.base));
    const moved = rebaseMovedChildren(draft, root, located?.element, choices, usePostSaveMerge);
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
    const element = mergePanelLayer('element', panel.detached ? undefined : panelBase, panelMine, target, elementMergeSpec(panelBase, panelMine, target), choices, usePostSaveMerge);
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
