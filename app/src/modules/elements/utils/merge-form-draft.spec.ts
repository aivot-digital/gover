import {describe, expect, it} from 'vitest';
import {applyPanelDraftToTree, getPanelDraftParents, type ElementPanelDraft} from '../../../components/element-tree-2/element-panel-draft';
import {ElementDisplayContext} from '../../../data/element-type/element-child-options';
import {ElementType} from '../../../data/element-type/element-type';
import {type AnyFormElement} from '../../../models/elements/form/any-form-element';
import {type FormLayoutElement} from '../../../models/elements/form-layout-element';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {flattenElements} from '../../../utils/flatten-elements';
import {mergeFormDraft, rebaseOpenElementDraft} from './merge-form-draft';

const text = {...generateElementWithDefaultValues(ElementType.Text), id: 'name', label: 'Vorname'};
const edited = {...text, label: 'Rufname'};
const step = {...generateElementWithDefaultValues(ElementType.Step), id: 'step', children: [text]};
const base: FormLayoutElement = {
    ...generateElementWithDefaultValues(ElementType.FormLayout) as FormLayoutElement,
    id: 'root', publicTitle: 'Kontaktformular', children: [step],
};
const panel: ElementPanelDraft = {
    base: text,
    mine: edited,
    parents: getPanelDraftParents([base, step], text.id),
    tab: 'properties',
    automaticDataKey: false,
    dataKeyUnlocked: false,
};

function mergeAgainst(target: AnyFormElement | undefined, draft = panel) {
    const theirs = {...base, children: [{...step, children: target ? [target] : []}]};
    return {base, mine: base, theirs, editableRoot: theirs, panel: draft};
}

function fieldReading(nodeDataKey: string, path: string) {
    return {
        ...text,
        value: {
            type: 'NoCode' as const, noCode: {type: 'NoCodeNodeDataReference' as const, nodeDataKey, path},
            requirements: null, javascriptCode: null, referencedIds: null,
        },
    };
}

describe('mergeFormDraft', () => {
    it.each(['layout', 'panel'])('offers both complete element data references when restoring the %s', (layer) => {
        const layoutWith = (field: typeof text) => ({...base, children: [{...step, children: [field]}]});
        const original = fieldReading('review', 'email');
        const local = {...fieldReading('manager', 'email'), label: 'Rufname'};
        const saved = {...fieldReading('review', 'name'), placeholder: 'Gespeichert'};
        const theirs = layoutWith(saved);
        const request = {
            base: layoutWith(original), mine: layoutWith(layer === 'panel' ? original : local), theirs, editableRoot: theirs,
            panel: layer === 'panel' ? {...panel, base: original, mine: local} : undefined,
        };
        const {conflicts} = mergeFormDraft(request, {});
        expect(conflicts).toHaveLength(1);
        expect(conflicts[0]).toMatchObject({mine: local.value.noCode, theirs: saved.value.noCode});
        for (const choice of ['mine', 'theirs'] as const) {
            const restored = mergeFormDraft(request, {[conflicts[0].id]: choice});
            expect(restored.conflicts).toEqual([]);
            const expected = {...saved, label: local.label, value: choice === 'mine' ? local.value : saved.value};
            if (layer === 'panel') {
                expect(restored.value).toMatchObject({layout: theirs, panel: {base: saved, mine: expected}});
            } else {
                expect(restored.value.layout).toEqual(layoutWith(expected));
            }
        }
    });

    it('keeps the pending edit outside the layout and rebases it onto the new stored properties', () => {
        const target = {...text, placeholder: 'Ihr Vorname'};
        const request = mergeAgainst(target);
        const result = mergeFormDraft(request, {});
        expect(result).toMatchObject({
            conflicts: [], changed: true,
            value: {layout: request.theirs, panel: {base: target, mine: {...target, label: 'Rufname'}}},
        });
    });

    it('keeps an already applied form edit when the panel is discarded', () => {
        const request = {...mergeAgainst(text), mine: {...base, publicTitle: 'Eigener Formulartitel'}};
        const result = mergeFormDraft(request, {});
        expect(result.value.layout).toEqual(request.mine);
        expect(result.value.panel?.mine).toEqual(edited);
    });

    it('skips a panel edit already present in the stored layout', () => {
        const request = mergeAgainst(edited);
        expect(mergeFormDraft(request, {})).toEqual({conflicts: [], changed: false, value: {layout: request.theirs}});
    });

    it('presents the conflicting property with its element, and preserves the stored layout for both decisions', () => {
        const target = {...text, label: 'Taufname'};
        const request = mergeAgainst(target);
        const {conflicts} = mergeFormDraft(request, {});
        expect(conflicts).toHaveLength(1);
        expect(conflicts[0]).toMatchObject({
            group: {label: 'Offenes Formularelement › Text „Rufname“'}, propertyLabel: 'Titel', mine: 'Rufname', theirs: 'Taufname',
        });
        const mine = mergeFormDraft(request, {[conflicts[0].id]: 'mine'}).value;
        expect(mine).toMatchObject({layout: request.theirs, panel: {mine: edited, base: target}});
        const theirs = mergeFormDraft(request, {[conflicts[0].id]: 'theirs'}).value;
        expect(theirs).toEqual({layout: request.theirs});
    });

    it('separates two consecutive edits to the same property when it also changed on the server', () => {
        const target = {...text, label: 'Taufname'};
        const mine = {...base, children: [{...step, children: [edited]}]};
        const request = {...mergeAgainst(target), mine, panel: {...panel, base: edited, mine: text}};
        const {conflicts} = mergeFormDraft(request, {});
        const preferLayout = {[conflicts[0].id]: 'mine'} as const;
        expect(mergeFormDraft(request, preferLayout)).toMatchObject({
            conflicts: [], value: {layout: mine, panel: {base: edited, mine: text}},
        });
        const preferStored = {[conflicts[0].id]: 'theirs'} as const;
        const remaining = mergeFormDraft(request, preferStored).conflicts;
        const preview = mergeFormDraft(request, {}).dependentConflicts!;
        expect(preview(preferLayout)).toEqual([]);
        expect(preview(preferStored)).toEqual(remaining);
        expect(remaining).toHaveLength(1);
        expect(remaining[0].id).not.toBe(conflicts[0].id);
        expect(mergeFormDraft(request, {...preferStored, [remaining[0].id]: 'mine'})).toMatchObject({
            conflicts: [], value: {layout: request.theirs, panel: {base: target, mine: text}},
        });
    });

    it('offers independent layout and panel conflicts together for either form choice', () => {
        const original = mergeAgainst({...text, label: 'Server'});
        const request = {
            ...original,
            mine: {...base, publicTitle: 'Mein Titel'},
            theirs: {...original.theirs, publicTitle: 'Anderer Titel'},
        };
        const result = mergeFormDraft(request, {});
        expect(result.conflicts).toHaveLength(1);
        for (const choice of ['mine', 'theirs'] as const) {
            const decisions = {[result.conflicts[0].id]: choice};
            const additional = result.dependentConflicts!(decisions);
            expect(additional).toHaveLength(1);
            expect(additional[0]).toMatchObject({mine: edited.label, theirs: 'Server'});
            expect(mergeFormDraft(request, {...decisions, [additional[0].id]: 'mine'}).conflicts).toEqual([]);
        }
    });

    it('keeps reinsertion pending until apply, including across a second session timeout', () => {
        const request = mergeAgainst(undefined);
        const {conflicts} = mergeFormDraft(request, {});
        expect(conflicts[0]).toMatchObject({mine: edited, theirs: undefined, propertyLabel: 'Gesamtes Element'});
        const result = mergeFormDraft(request, {[conflicts[0].id]: 'mine'});
        expect(result).toMatchObject({
            conflicts: [], value: {layout: request.theirs, panel: {...panel, detached: true}},
        });
        const again = mergeFormDraft({...request, base: request.theirs, mine: request.theirs, panel: result.value.panel}, {});
        expect(again).toEqual(result);
        expect(mergeFormDraft(request, {[conflicts[0].id]: 'theirs'}).value).toEqual({layout: request.theirs});
    });

    it.each(['deleted', 'retyped'])('asks about a %s field on recovery after showing its unconfirmed preview during a save', (change) => {
        const replacement = change === 'retyped' ? {...generateElementWithDefaultValues(ElementType.Number), id: text.id} : undefined;
        const {theirs} = mergeAgainst(replacement);
        const preview = rebaseOpenElementDraft(panel, base, theirs);
        expect(preview.needsDecision).toBe(true);
        expect(preview.draft?.mine).toEqual(edited);
        const request = {base: theirs, mine: theirs, theirs, editableRoot: theirs, panel: preview.draft!};
        const {conflicts} = mergeFormDraft(request, {});
        expect(conflicts).toHaveLength(1);
        expect(conflicts[0]).toMatchObject({mine: edited, theirs: replacement});
        expect(mergeFormDraft(request, {[conflicts[0].id]: 'theirs'}).value.panel).toBeUndefined();
        const accepted = mergeFormDraft(request, {[conflicts[0].id]: 'mine'});
        expect(accepted.value.panel?.mine).toEqual(edited);
        expect(mergeFormDraft({...request, panel: accepted.value.panel}, {})).toEqual(accepted);
    });

    it('keeps the most recent field text without requiring a structural choice', () => {
        const {theirs} = mergeAgainst({...text, label: 'Gespeicherter Titel'});
        const preview = rebaseOpenElementDraft(panel, base, theirs);
        expect(preview).toMatchObject({needsDecision: false, draft: {mine: edited}});
        expect(mergeFormDraft({base: theirs, mine: theirs, theirs, editableRoot: theirs, panel: preview.draft!}, {}).conflicts).toEqual([]);
    });

    it('keeps an incompatible replacement in the layout while the original type is being edited', () => {
        const replacement = {...generateElementWithDefaultValues(ElementType.Number), id: text.id};
        const request = mergeAgainst(replacement);
        const {conflicts} = mergeFormDraft(request, {});
        const result = mergeFormDraft(request, {[conflicts[0].id]: 'mine'});
        expect(result).toMatchObject({
            conflicts: [], value: {layout: request.theirs, panel: {base: replacement, mine: edited}},
        });
    });

    it('finds an element even when it moved and remembers its current parent', () => {
        const parent = {...generateElementWithDefaultValues(ElementType.GroupLayout), children: [text]};
        const currentStep = {...step, children: [parent]};
        const theirs = {...base, children: [currentStep]};
        const result = mergeFormDraft({...mergeAgainst(text), theirs}, {});
        expect(result.value.panel?.parents).toEqual(getPanelDraftParents([theirs, currentStep, parent], text.id));
    });

    it('presents the full replacement of an incompatible ancestor before offering the detached panel', () => {
        const group = {...generateElementWithDefaultValues(ElementType.GroupLayout), children: [text]};
        const oldStep = {...step, children: [group]};
        const oldLayout = {...base, children: [oldStep]};
        const replacement = {...generateElementWithDefaultValues(ElementType.Number), id: group.id};
        const theirs = {...base, children: [{...step, children: [replacement]}]};
        const draft = {...panel, parents: getPanelDraftParents([oldLayout, oldStep, group], text.id)};
        const request = {base: oldLayout, mine: oldLayout, theirs, editableRoot: theirs, panel: draft};
        const {conflicts} = mergeFormDraft(request, {});
        expect(conflicts).toHaveLength(1);
        expect(conflicts[0]).toMatchObject({mine: {...group, children: [edited]}, theirs: replacement});
        const result = mergeFormDraft(request, {[conflicts[0].id]: 'mine'});
        expect(result.value).toMatchObject({layout: theirs, panel: {...draft, detached: true}});
        const newer = {...theirs, children: [{...step, children: [{...replacement, label: 'Neuer Titel'}]}]};
        const next = mergeFormDraft({...request, base: theirs, mine: theirs, theirs: newer, editableRoot: newer, panel: result.value.panel}, {});
        expect(next.conflicts).toHaveLength(1);
        expect(next.conflicts[0].theirs).toEqual({...replacement, label: 'Neuer Titel'});
    });

    it('offers a conflicting edit to a moved child and applies its resolved properties at the stored location', () => {
        const group = {...generateElementWithDefaultValues(ElementType.GroupLayout), children: [text]};
        const oldStep = {...step, children: [group]};
        const oldLayout = {...base, children: [oldStep]};
        const saved = {...text, label: 'Gespeichert', placeholder: 'Platzhalter'};
        const newStep = {...step, children: [{...group, children: []}, saved]};
        const theirs = {...base, children: [newStep]};
        const draft = {...panel, base: group, mine: {...group, children: [edited]}, parents: getPanelDraftParents([oldLayout, oldStep], group.id)};
        const request = {base: oldLayout, mine: oldLayout, theirs, editableRoot: theirs, panel: draft};
        const {conflicts} = mergeFormDraft(request, {});
        expect(conflicts).toHaveLength(1);
        expect(conflicts[0]).toMatchObject({propertyLabel: 'Titel', mine: edited.label, theirs: saved.label});
        expect(mergeFormDraft(request, {[conflicts[0].id]: 'theirs'}).value).toEqual({layout: theirs});
        const restored = mergeFormDraft(request, {[conflicts[0].id]: 'mine'}).value.panel!;
        expect(restored.movedDescendantBases).toEqual([saved]);
        const applied = applyPanelDraftToTree(theirs, restored, restored.mine, ElementDisplayContext.CustomerFacing);
        expect(applied).toMatchObject({tree: {...theirs, children: [{...newStep, children: [{...group, children: []}, {...saved, label: edited.label}]}]}});
        expect('tree' in applied && flattenElements(applied.tree).filter((item) => item.id === saved.id)).toHaveLength(1);
    });

    it('protects a moved child reference both during recovery and when applying an outdated panel', () => {
        const original = fieldReading('review', 'email');
        const local = fieldReading('manager', 'email');
        const saved = fieldReading('review', 'name');
        const group = {...generateElementWithDefaultValues(ElementType.GroupLayout), id: 'group', children: [original]};
        const oldStep = {...step, children: [group]};
        const oldLayout = {...base, children: [oldStep]};
        const emptyGroup = {...group, children: []};
        const theirs = {...base, children: [{...step, children: [emptyGroup, saved]}]};
        const draft = {...panel, base: group, mine: {...group, children: [local]}, parents: getPanelDraftParents([oldLayout, oldStep], group.id)};
        expect(applyPanelDraftToTree(theirs, draft, draft.mine, ElementDisplayContext.CustomerFacing)).toHaveProperty('error');
        const request = {base: oldLayout, mine: oldLayout, theirs, editableRoot: theirs, panel: draft};
        const {conflicts} = mergeFormDraft(request, {});
        expect(conflicts).toHaveLength(1);
        expect(conflicts[0]).toMatchObject({mine: local.value.noCode, theirs: saved.value.noCode});
        const resolved = mergeFormDraft(request, {[conflicts[0].id]: 'mine'}).value.panel!;
        const applied = applyPanelDraftToTree(theirs, resolved, resolved.mine, ElementDisplayContext.CustomerFacing);
        expect(applied).toMatchObject({tree: {children: [{children: [emptyGroup, local]}]}});
    });

    it('keeps removal of a child moved outside its group pending until Apply, including across another login', () => {
        const group = {...generateElementWithDefaultValues(ElementType.GroupLayout), id: 'source', children: [text]};
        const empty = {...group, children: []};
        const other = {...empty, id: 'other', title: 'Ansprechperson'};
        const oldStep = {...step, children: [group, other]};
        const original = {...base, children: [oldStep]};
        const sibling = {...text, id: 'sibling', label: 'Telefon'};
        const savedGroup = {...other, children: [sibling, text]};
        const withTarget = (children: AnyFormElement[]) => ({...base, children: [{...step, children: [empty, {...savedGroup, children}]}]});
        const stored = withTarget([sibling, text]);
        const draft = {...panel, base: group, mine: empty, parents: getPanelDraftParents([original, oldStep], group.id)};
        const req = {base: original, mine: original, theirs: stored, editableRoot: stored, panel: draft};
        const {conflicts} = mergeFormDraft(req, {});
        expect(conflicts).toHaveLength(1);
        expect(conflicts[0]).toMatchObject({
            propertyLabel: 'Position im Formular', mine: undefined, theirs: text,
            elementPlacement: {elementId: text.id, theirs: expect.any(String)},
        });
        expect(mergeFormDraft(req, {[conflicts[0].id]: 'theirs'}).value).toEqual({layout: stored});
        expect(applyPanelDraftToTree(stored, draft, empty, ElementDisplayContext.CustomerFacing)).toHaveProperty('error');
        const chosen = mergeFormDraft(req, {[conflicts[0].id]: 'mine'});
        const pending = chosen.value.panel!;
        expect(chosen.value.layout).toEqual(stored);
        expect(pending.pendingMovedRemovals).toEqual([{element: text, parentId: other.id}]);
        const next = {base: stored, mine: stored, theirs: stored, editableRoot: stored, panel: structuredClone(pending)};
        const restored = mergeFormDraft(next, {});
        expect(restored.conflicts).toEqual([]);
        expect(restored.value.layout).toEqual(stored);
        expect(applyPanelDraftToTree(stored, restored.value.panel!, restored.value.panel!.mine, ElementDisplayContext.CustomerFacing))
            .toEqual({affectedId: stored.id, tree: withTarget([sibling])});

        const changed = withTarget([sibling, edited]);
        const movedAgain = {...base, children: [{...step, children: [empty, {...savedGroup, children: [sibling]}, text]}]};
        const backInPanel = {...base, children: [{...step, children: [group, {...savedGroup, children: [sibling]}]}]};
        for (const later of [changed, movedAgain, backInPanel]) {
            expect(applyPanelDraftToTree(later, pending, pending.mine, ElementDisplayContext.CustomerFacing)).toHaveProperty('error');
            const request = {...next, theirs: later, editableRoot: later};
            const again = mergeFormDraft(request, {}).conflicts;
            expect(again).toHaveLength(1);
            expect(mergeFormDraft(request, {[again[0].id]: 'theirs'}).value).toEqual({layout: later});
            const renewed = mergeFormDraft(request, {[again[0].id]: 'mine'}).value.panel!;
            expect(applyPanelDraftToTree(later, renewed, renewed.mine, ElementDisplayContext.CustomerFacing))
                .toEqual({affectedId: stored.id, tree: withTarget([sibling])});
        }
        const alreadyRemoved = withTarget([sibling]);
        expect(mergeFormDraft({...next, theirs: alreadyRemoved, editableRoot: alreadyRemoved}, {}))
            .toEqual({changed: false, conflicts: [], value: {layout: alreadyRemoved}});
    });

    it('requires a real decision after previewing removal of a moved child during a form save', () => {
        const group = {...generateElementWithDefaultValues(ElementType.GroupLayout), id: 'group', children: [text]};
        const empty = {...group, children: []};
        const before = {...base, children: [{...step, children: [group]}]};
        const stored = {...base, children: [{...step, children: [empty, text]}]};
        const draft = {...panel, base: group, mine: empty, parents: getPanelDraftParents([before, step], group.id)};
        const preview = rebaseOpenElementDraft(draft, before, stored);
        expect(preview.needsDecision).toBe(true);
        expect(preview.draft?.pendingMovedRemovals).toEqual([{element: text, parentId: step.id}]);
        const req = {base: stored, mine: stored, theirs: stored, editableRoot: stored, panel: preview.draft!};
        const {conflicts} = mergeFormDraft(req, {});
        expect(conflicts).toHaveLength(1);
        expect(mergeFormDraft(req, {[conflicts[0].id]: 'theirs'}).value).toEqual({layout: stored});
    });

    it('resolves an internal move in an open step without applying the step or dropping its other edits', () => {
        const a = {...generateElementWithDefaultValues(ElementType.GroupLayout), id: 'a', children: [text]};
        const b = {...a, id: 'b', children: []};
        const original = {...step, children: [a, b]};
        const empty = {...a, children: []};
        const moving = {...step, name: 'Mein Abschnitt', children: [empty, {...b, children: [text]}]};
        const storedStep = {...step, children: [{...empty, name: 'Gespeicherte Gruppe'}, b]};
        const before = {...base, children: [original]};
        const stored = {...base, children: [storedStep]};
        const draft = {...panel, base: original, mine: moving, parents: getPanelDraftParents([before], original.id)};
        const request = {base: before, mine: before, theirs: stored, editableRoot: stored, panel: draft};
        const {conflicts} = mergeFormDraft(request, {});
        expect(conflicts).toHaveLength(1);
        expect(conflicts[0]).toMatchObject({propertyLabel: 'Position im Formular', elementPlacement: {elementId: text.id}});
        for (const choice of ['mine', 'theirs'] as const) {
            const resolved = mergeFormDraft(request, {[conflicts[0].id]: choice}).value;
            expect(resolved.layout).toEqual(stored);
            const result = applyPanelDraftToTree(stored, resolved.panel!, resolved.panel!.mine, ElementDisplayContext.CustomerFacing);
            expect(result).toMatchObject({tree: {children: [{
                name: 'Mein Abschnitt', children: [{name: 'Gespeicherte Gruppe', children: []}, {children: choice === 'mine' ? [text] : []}],
            }]}});
        }
    });

    it('previews layout, moved child and deleted group choices together before applying', () => {
        const group = {...generateElementWithDefaultValues(ElementType.GroupLayout), children: [text]};
        const oldStep = {...step, children: [group]};
        const oldLayout = {...base, children: [oldStep]};
        const serverChild = {...text, label: 'Gespeicherter Titel'};
        const theirs = {...base, publicTitle: 'Server', children: [{...step, children: [serverChild]}]};
        const draft = {...panel, base: group, mine: {...group, name: 'Eigene Gruppe', children: [edited]}, parents: getPanelDraftParents([oldLayout, oldStep], group.id)};
        const request = {base: oldLayout, mine: {...oldLayout, publicTitle: 'Eigenes Formular'}, theirs, editableRoot: theirs, panel: draft};
        const first = mergeFormDraft(request, {});
        const layout = first.conflicts[0].id;
        const preview = first.dependentConflicts!;
        const [child, reinsertWithChild] = preview({[layout]: 'mine'});
        expect(child).toMatchObject({mine: edited.label, theirs: serverChild.label});
        // Choosing the local child also changes its new location, so the structural choice covers the whole form.
        expect(reinsertWithChild.mine).toMatchObject({id: oldLayout.id});
        const selection = {[layout]: 'mine', [child.id]: 'theirs'} as const;
        const [sameChild, reinsertGroup] = preview(selection);
        expect(sameChild.id).toBe(child.id);
        expect(reinsertGroup).toMatchObject({mine: {id: group.id}, theirs: undefined});
        expect(preview({}, 'theirs').map(({id}) => id)).toEqual([child.id, reinsertGroup.id]);
        const result = mergeFormDraft(request, {...selection, [reinsertGroup.id]: 'mine'});
        expect(result).toMatchObject({
            conflicts: [], value: {layout: {publicTitle: 'Eigenes Formular'}, panel: {mine: {name: 'Eigene Gruppe', children: []}, detached: true}},
        });
    });

    it('maps an unconfigured root panel to the editable root created by the new visit', () => {
        const oldRoot = generateElementWithDefaultValues(ElementType.FormLayout) as FormLayoutElement;
        const newRoot = generateElementWithDefaultValues(ElementType.FormLayout) as FormLayoutElement;
        const draft = {...panel, base: oldRoot, mine: {...oldRoot, publicTitle: 'Titel'}, parents: []};
        const result = mergeFormDraft({base: null, mine: oldRoot, theirs: null, editableRoot: newRoot, panel: draft}, {});
        expect(result).toMatchObject({
            conflicts: [], changed: true,
            value: {layout: null, panel: {base: newRoot, mine: {...newRoot, publicTitle: 'Titel'}}},
        });
    });

    it('recognizes a stored empty default as the editable root', () => {
        const emptyRoot = generateElementWithDefaultValues(ElementType.FormLayout) as FormLayoutElement;
        const draft = {...panel, base: emptyRoot, mine: {...emptyRoot, publicTitle: 'Titel'}, parents: []};
        const result = mergeFormDraft({base: emptyRoot, mine: emptyRoot, theirs: emptyRoot, editableRoot: emptyRoot, panel: draft}, {});
        expect(result).toMatchObject({conflicts: [], changed: true, value: {layout: null, panel: {base: emptyRoot, mine: draft.mine}}});
    });

    it('does not mistake a deleted configured form for the default generated at a new visit', () => {
        const freshRoot = generateElementWithDefaultValues(ElementType.FormLayout) as FormLayoutElement;
        const draft = {...panel, base, mine: {...base, publicTitle: 'Titel'}, parents: []};
        const request = {base, mine: base, theirs: null, editableRoot: freshRoot, panel: draft};
        const {conflicts} = mergeFormDraft(request, {});
        expect(conflicts).toHaveLength(1);
        expect(mergeFormDraft(request, {[conflicts[0].id]: 'mine'})).toMatchObject({
            value: {layout: null, panel: {base, mine: draft.mine, detached: true}},
        });
    });
});
