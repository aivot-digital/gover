import {describe, expect, it} from 'vitest';
import {ElementType} from '../../../data/element-type/element-type';
import {keyById, type MergeChoices, type MergeSpec, threeWayMerge} from '../../../utils/three-way-merge';
import {mergeElementTreeStructure} from './element-tree-structure-merge';

interface Element {
    id: string;
    type: ElementType;
    name: string;
    children?: Element[];
}
const field = (id: string): Element => ({id, type: ElementType.Text, name: id});
const group = (id: string, children: Element[], name = id): Element => ({id, type: ElementType.GroupLayout, name, children});
const form = (...groups: Element[]): Element => ({id: 'root', type: ElementType.FormLayout, name: 'Formular', children: groups});
const spec: MergeSpec<Element> = {
    arrayKey: keyById, mergeSubtree: (input) => input.path.length === 0 ? mergeElementTreeStructure(input) : undefined,
};
const x = field('x'), y = field('y'), a = field('a'), b = field('b'), extra = field('extra');

describe('moving an element that the other editor deleted', () => {
    it.each(['mine', 'theirs'] as const)('keeps or removes exactly the chosen field when %s moved it', (movingSide) => {
        const base = form(group('A', [x]), group('B', [a, b]));
        const moved = {...form(group('A', []), group('B', [a, {...x, name: 'Moved'}, b])), name: 'New title'};
        // Deleting A also deletes x; the destination was independently edited.
        const savedB = {...b, name: 'Changed'};
        const deleted = form(group('B', [a, savedB, extra]));
        const mine = movingSide === 'mine' ? moved : deleted;
        const theirs = movingSide === 'theirs' ? moved : deleted;
        const merge = (choices: MergeChoices) => threeWayMerge(base, mine, theirs, spec, choices);
        const {conflicts} = merge({});
        expect(conflicts).toHaveLength(1);
        expect(conflicts[0].elementPlacement).toEqual({elementId: 'x', [movingSide]: 'New title › B'});
        for (const choice of ['mine', 'theirs'] as const) {
            const children = choice === movingSide ? [a, {...x, name: 'Moved'}, savedB, extra] : [a, savedB, extra];
            expect(merge({[conflicts[0].id]: choice})).toEqual({conflicts: [], merged: {...form(group('B', children)), name: 'New title'}});
        }
    });

    it('resolves several independent placements separately, ordering kept siblings as moved', () => {
        const base = form(group('A', [x, y, extra]), group('B', [a, b]));
        const mine = form(group('A', []), group('B', [a, y, x, extra, b]));
        const theirs = form(group('A', []), group('B', [a, b]));
        const {conflicts} = threeWayMerge(base, mine, theirs, spec);
        expect(conflicts.map((c) => c.elementPlacement?.elementId).sort()).toEqual(['extra', 'x', 'y']);
        const choices = Object.fromEntries(conflicts.map((c) => [c.id, c.elementPlacement?.elementId === 'extra' ? 'theirs' : 'mine'] as const));
        expect(threeWayMerge(base, mine, theirs, spec, choices)).toEqual({
            conflicts: [], merged: form(group('A', []), group('B', [a, y, x, b])),
        });
    });

    it('resolves a group as a unit and disambiguates equal container names', () => {
        const subtree = group('X', [x, y]);
        const base = form(group('A', [subtree], 'Gruppe'), group('B', [], 'Gruppe'));
        const mine = form(group('A', [], 'Gruppe'), group('B', [subtree], 'Gruppe'));
        const theirs = form(group('A', [], 'Gruppe'), group('B', [], 'Gruppe'));
        const {conflicts} = threeWayMerge(base, mine, theirs, spec);
        expect(conflicts).toHaveLength(1);
        expect(conflicts[0].elementPlacement).toEqual({elementId: 'X', mine: 'Formular › Gruppe (B)'});
        expect(threeWayMerge(base, mine, theirs, spec, {[conflicts[0].id]: 'mine'}).merged).toEqual(mine);
    });

    it('shows the target path from the merged form when the deleting editor relocated and renamed the target', () => {
        const base = form(group('A', [x]), group('B', []), group('C', []));
        const mine = form(group('A', []), group('B', [x]), group('C', []));
        const theirs = form(group('A', []), group('C', [group('B', [], 'New name')], 'New location'));
        const {conflicts} = threeWayMerge(base, mine, theirs, spec);
        expect(conflicts).toHaveLength(1);
        expect(conflicts[0].elementPlacement).toEqual({elementId: 'x', mine: 'Formular › New location › New name'});
        expect(threeWayMerge(base, mine, theirs, spec, {[conflicts[0].id]: 'mine'}).merged)
            .toEqual(form(group('A', []), group('C', [group('B', [x], 'New name')], 'New location')));
    });

    it('places a moved block between reversed surviving anchors in its selected order', () => {
        const base = form(group('A', [x, y]), group('B', [a, b]));
        const mine = form(group('A', []), group('B', [a, y, x, b]));
        const theirs = form(group('A', []), group('B', [b, a]));
        const {conflicts} = threeWayMerge(base, mine, theirs, spec);
        expect(conflicts).toHaveLength(2);
        const choices = Object.fromEntries(conflicts.map(({id}) => [id, 'mine'] as const));
        expect(threeWayMerge(base, mine, theirs, spec, choices))
            .toEqual({merged: form(group('A', []), group('B', [b, y, x, a])), conflicts: []});
    });

    it('keeps a newly added destination when deciding for the moved element', () => {
        const base = form(group('A', [x]));
        const mine = form(group('A', []), group('B', [x]));
        const theirs = form(group('A', []));
        const {conflicts} = threeWayMerge(base, mine, theirs, spec);
        expect(conflicts).toHaveLength(1);
        expect(threeWayMerge(base, mine, theirs, spec, {[conflicts[0].id]: 'mine'}).merged).toEqual(mine);
        expect(threeWayMerge(base, mine, theirs, spec, {[conflicts[0].id]: 'theirs'}).merged)
            .toEqual(form(group('A', []), group('B', [])));
    });

    it('keeps regular ordering and two identical deletions automatic', () => {
        const base = form(group('A', [a, x, b]), group('B', []));
        const reordered = form(group('A', [x, a, b]), group('B', []));
        const deleted = form(group('A', [a, b]), group('B', []));
        expect(threeWayMerge(base, reordered, deleted, spec)).toEqual({merged: deleted, conflicts: []});
        expect(threeWayMerge(base, deleted, deleted, spec)).toEqual({merged: deleted, conflicts: []});
    });

    it('recognizes a committed move even when unrelated content has since changed', () => {
        const base = form(group('A', [x]), group('B', []));
        const sent = form(group('A', []), group('B', [x]));
        const mine = {...form(group('A', []), group('B', [])), name: 'My change'};
        const theirs = form(group('A', []), group('B', [x, extra]));
        expect(threeWayMerge(base, mine, theirs, spec, {}, {value: sent})).toEqual({
            merged: {...form(group('A', []), group('B', [extra])), name: 'My change'}, conflicts: [],
        });
        const newer = form(group('A', []), group('B', [{...x, name: 'Edited after move'}, extra]));
        expect(threeWayMerge(base, mine, newer, spec, {}, {value: sent}).conflicts).toHaveLength(1);
    });

    it('keeps a child moved out locally before deletion of its previously saved group', () => {
        const subtree = group('X', [x]);
        const base = form(group('A', [subtree]), group('B', []), group('C', []));
        const sent = form(group('A', []), group('B', [subtree]), group('C', []));
        const theirs = {...sent, name: 'New title'};
        const mine = form(group('A', []), group('B', []), group('C', [x]));
        expect(threeWayMerge(base, mine, theirs, spec, {}, {value: sent})).toEqual({
            conflicts: [], merged: {...mine, name: 'New title'},
        });
    });

    it('considers a placement present only in the submitted value when the local edit reverted it', () => {
        const base = form(group('A', []), group('B', []));
        const sent = form(group('A', [x]), group('B', []));
        const mine = {...base, name: 'My new title'};
        const theirs = form(group('A', []), group('B', [x]));
        const {conflicts} = threeWayMerge(base, mine, theirs, spec, {}, {value: sent});
        expect(conflicts).toHaveLength(1);
        expect(conflicts[0].elementPlacement).toEqual({elementId: 'x', theirs: 'My new title › B'});
    });

    it.each(['missing destination', 'overlapping descendants'])('falls back to the embedded tree for %s and preserves other fields', (cause) => {
        const base = form(group('A', [group('X', [x])]), group('B', []));
        const mine = form(group('A', []), group('B', [group('X', [x])]));
        const theirs = cause === 'missing destination' ? form(group('A', [])) : form(group('A', [x]), group('B', []));
        const wrap = (tree: Element, note: string, title: string) => ({tree, note, title});
        const nestedSpec: MergeSpec<ReturnType<typeof wrap>> = {
            arrayKey: keyById, mergeSubtree: (input) => input.path.join('/') === 'tree' ? mergeElementTreeStructure(input) : undefined,
        };
        const merge = (choices: MergeChoices) => threeWayMerge(
            wrap(base, '', ''), wrap(mine, 'Note', ''), wrap(theirs, '', 'Title'), nestedSpec, choices,
        );
        expect(merge({}).conflicts.map((c) => c.path)).toEqual([['tree']]);
        for (const side of ['mine', 'theirs'] as const) {
            expect(merge({tree: side})).toEqual({merged: wrap(side === 'mine' ? mine : theirs, 'Note', 'Title'), conflicts: []});
        }
    });
});
