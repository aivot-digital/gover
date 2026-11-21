import {describe, expect, it} from 'vitest';
import {ElementDisplayContext} from '../../data/element-type/element-child-options';
import {ElementType} from '../../data/element-type/element-type';
import {generateElementWithDefaultValues} from '../../utils/generate-element-with-default-values';
import {applyPanelDraftToTree, getPanelDraftParents, type ElementPanelDraft} from './element-panel-draft';

const child = {...generateElementWithDefaultValues(ElementType.Text), id: 'child', label: 'Name'};
const group = {...generateElementWithDefaultValues(ElementType.GroupLayout), id: 'group', children: [child]};
const step = {...generateElementWithDefaultValues(ElementType.Step), id: 'step', children: [group]};
const draft: ElementPanelDraft = {
    base: group, mine: {...group, name: 'Geänderte Gruppe'}, tab: 'properties',
    parents: getPanelDraftParents([step], group.id), automaticDataKey: false, dataKeyUnlocked: false,
};
const forForm = ElementDisplayContext.CustomerFacing;

describe('applying an open element to its current form', () => {
    it('rejects a stale subtree edit when an internal move conflicts with a removal', () => {
        const second = {...group, id: 'second', children: []};
        const original = {...step, children: [group, second]};
        const removed = {...step, children: [{...group, children: []}, second]};
        const moved = {...step, children: [{...group, children: []}, {...second, children: [child]}]};
        for (const [mine, stored] of [[removed, moved], [moved, removed]]) {
            const panel = {...draft, base: original, mine, parents: []};
            expect(applyPanelDraftToTree(stored, panel, mine, forForm)).toHaveProperty('error');
        }
    });

    it('keeps a child moved elsewhere on the server when restoring a group over a different element type', () => {
        const replacement = {...child, id: group.id};
        const moved = {...child, label: 'Neuer Titel'};
        const current = {...step, children: [replacement, moved]};
        const result = applyPanelDraftToTree(current, draft, draft.mine, forForm);
        expect(result).toEqual({
            affectedId: group.id,
            tree: {...step, children: [{...draft.mine, children: []}, moved]},
        });
    });

    it('applies the chosen child properties at the moved location while preserving newer independent properties', () => {
        const replacement = {...child, id: group.id};
        const moved = {...child, placeholder: 'Gespeicherter Hinweis'};
        const current = {...step, children: [replacement, moved]};
        const localChild = {...child, label: 'Rufname'};
        const mine = {...group, name: 'Geänderte Gruppe', children: [localChild]};
        const rebased = {...draft, base: replacement, mine, movedDescendantBases: [child]};
        expect(applyPanelDraftToTree(current, rebased, mine, forForm)).toEqual({
            affectedId: step.id,
            tree: {...step, children: [{...mine, children: []}, {...moved, label: 'Rufname'}]},
        });
    });

    it('rebuilds the missing group in its former slot and leaves unrelated branches intact', () => {
        const earlier = {...child, id: 'sibling'};
        const prior = {...step, children: [earlier, group]};
        const current = {...prior, children: [earlier]};
        const childDraft = {...draft, detached: true, base: child, mine: {...child, label: 'Entwurf'},
            parents: getPanelDraftParents([prior, group], child.id)};
        expect(applyPanelDraftToTree(current, childDraft, childDraft.mine, forForm)).toEqual({
            affectedId: group.id,
            tree: {...step, children: [earlier, {...group, children: [childDraft.mine]}]},
        });
    });

    it('rejects newly authored conflicting IDs within the panel, on its root, and in other branches', () => {
        const sibling = {...child, id: 'sibling'};
        const current = {...step, children: [group, sibling]};
        for (const invalid of [{...group, id: sibling.id}, {...group, children: [child, child]}, {...group, children: [sibling]}]) {
            expect(applyPanelDraftToTree(current, draft, invalid, forForm)).toEqual({
                error: 'Mehrere Formularelemente haben dieselbe ID. Prüfen Sie die IDs im Reiter „Elementstruktur“.',
            });
        }
    });
});
