import {describe, expect, it} from 'vitest';
import {InputMode, InputVariableSource} from '../models/input-mode';
import {keyById, ORDER_SEGMENT, pathToId, threeWayMerge} from './three-way-merge';
import {variableReferenceMergeRules} from './variable-reference-merge';

describe('threeWayMerge', () => {
    it('takes changes that only one side made', () => {
        const base = {name: 'A', description: 'alt', notes: 'x'};
        const mine = {name: 'B', description: 'alt', notes: 'x'};
        const theirs = {name: 'A', description: 'neu', notes: 'x'};

        const result = threeWayMerge(base, mine, theirs);

        expect(result.conflicts).toEqual([]);
        expect(result.merged).toEqual({name: 'B', description: 'neu', notes: 'x'});
    });

    it('accepts the same change on both sides', () => {
        const result = threeWayMerge({name: 'A'}, {name: 'B'}, {name: 'B'});

        expect(result.conflicts).toEqual([]);
        expect(result.merged).toEqual({name: 'B'});
    });

    it('reports different changes of the same value and keeps the stored state until a choice is made', () => {
        const base = {config: {subject: 'A', body: 'x'}};
        const mine = {config: {subject: 'B', body: 'x'}};
        const theirs = {config: {subject: 'C', body: 'y'}};

        const result = threeWayMerge(base, mine, theirs);

        expect(result.conflicts).toEqual([{
            id: 'config/subject',
            path: ['config', 'subject'],
            base: 'A',
            mine: 'B',
            theirs: 'C',
        }]);
        expect(result.merged).toEqual({config: {subject: 'C', body: 'y'}});

        const resolved = threeWayMerge(base, mine, theirs, {}, {'config/subject': 'mine'});
        expect(resolved.conflicts).toEqual([]);
        expect(resolved.merged).toEqual({config: {subject: 'B', body: 'y'}});
    });

    it('removes properties that one side removed', () => {
        const result = threeWayMerge({a: 1, b: 2}, {a: 1}, {a: 3, b: 2});

        expect(result.conflicts).toEqual([]);
        expect(result.merged).toEqual({a: 3});
    });

    it('keeps explicit undefined fields when both editor models define the key', () => {
        const base = {label: 'A', hint: undefined};
        const mine = {...base, label: 'B'};
        const theirs = {...base, label: 'C'};
        expect(threeWayMerge(base, mine, theirs, {}, {label: 'theirs'}).merged).toStrictEqual(theirs);
    });

    it('takes server-managed fields from the stored state', () => {
        const base = {name: 'A', lockVersion: 1};
        const mine = {name: 'B', lockVersion: 1};
        const theirs = {name: 'A', lockVersion: 2};

        const result = threeWayMerge(base, mine, theirs, {serverManaged: ['lockVersion']});

        expect(result.conflicts).toEqual([]);
        expect(result.merged).toEqual({name: 'B', lockVersion: 2});
        expect(threeWayMerge(base, mine, theirs, {serverManaged: ['lockVersion']}, {[pathToId([])]: 'mine'}).merged)
            .toEqual({name: 'B', lockVersion: 2});
    });

    it('merges arrays without a key as a whole', () => {
        const result = threeWayMerge({tags: ['a']}, {tags: ['a', 'b']}, {tags: ['c']});

        expect(result.conflicts.map((conflict) => conflict.id)).toEqual(['tags']);
    });

    describe('variable references', () => {
        const variable = (nodeDataKey: string, path: string) => ({
            type: InputMode.Variable, reference: {source: InputVariableSource.ElementData, nodeDataKey, path},
        });
        const noCode = (nodeDataKey: string, path: string) => ({type: 'NoCodeNodeDataReference', nodeDataKey, path});
        const rules = variableReferenceMergeRules;

        it.each([
            {mode: 'Variable', reference: variable},
            {mode: 'NoCodeNodeDataReference', reference: noCode},
        ])('decides between complete $mode selections and still merges independent edits', ({reference}) => {
            const base = {selection: reference('review', 'email'), notes: '', name: ''};
            const mine = {...base, selection: reference('manager', 'email'), name: 'Local'};
            const theirs = {...base, selection: reference('review', 'name'), notes: 'Remote'};
            const result = threeWayMerge(base, mine, theirs, rules);
            expect(result.conflicts).toEqual([{
                id: 'selection', path: ['selection'], base: base.selection, mine: mine.selection, theirs: theirs.selection,
            }]);
            expect(result.merged).toStrictEqual({...theirs, name: 'Local'});
            for (const choice of ['mine', 'theirs'] as const) {
                const resolved = threeWayMerge(base, mine, theirs, rules, {selection: choice});
                expect(resolved.conflicts).toEqual([]);
                expect(resolved.merged).toStrictEqual({...theirs, name: 'Local', selection: choice === 'mine' ? mine.selection : theirs.selection});
            }
        });

        it('treats a missing optional key and Markdown flag as their server defaults inside an unkeyed array', () => {
            const selection = {type: InputMode.Variable, reference: {source: InputVariableSource.ProcessData, path: 'email'}};
            const stored = {...selection, reference: {...selection.reference, nodeDataKey: null, markdown: false}};
            const noCodeDraft = {type: 'NoCodeNodeDataReference', path: ''};
            const noCodeStored = {...noCodeDraft, nodeDataKey: null};
            const mine = {options: [selection, noCodeDraft]};
            const theirs = {options: [stored, noCodeStored]};
            const result = threeWayMerge({options: []}, mine, theirs, rules);
            expect(result.conflicts).toEqual([]);
            expect(result.merged).toBe(theirs);
        });

        it('recognizes the full submitted selection as a possible server baseline after an interrupted write', () => {
            const base = {selection: variable('review', 'email')};
            const submitted = {selection: variable('manager', 'email')};
            const theirs = {selection: {...submitted.selection, reference: {...submitted.selection.reference, markdown: false}}};
            const mine = {selection: variable('review', 'name')};
            expect(threeWayMerge(base, mine, theirs, rules, {}, {value: submitted})).toEqual({merged: mine, conflicts: []});

            const thirdChoice = {selection: variable('review', 'phone')};
            expect(threeWayMerge(base, mine, thirdChoice, rules, {}, {value: submitted}).conflicts)
                .toEqual([{id: 'selection', path: ['selection'], base: base.selection, mine: mine.selection, theirs: thirdChoice.selection}]);
        });

        it('retains the Markdown property of the chosen side even when the reference paths match', () => {
            const base = variable('review', 'email');
            const mine = variable('manager', 'email');
            const theirs = {...mine, reference: {...mine.reference, markdown: true}};
            const result = threeWayMerge(base, mine, theirs, rules);
            expect(result.conflicts).toEqual([{id: '', path: [], base, mine, theirs}]);
            expect(threeWayMerge(base, mine, theirs, rules, {'': 'mine'}).merged).toStrictEqual(mine);
            expect(threeWayMerge(base, mine, theirs, rules, {'': 'theirs'}).merged).toStrictEqual(theirs);
        });

        it('merges ordinary literal objects containing a key, reference and path field by field', () => {
            const value = {nodeDataKey: 'review', path: 'email', reference: 'local label'};
            const base = {type: InputMode.Literal, value};
            const mine = {...base, value: {...value, nodeDataKey: 'manager'}};
            const theirs = {...base, value: {...value, path: 'name'}};
            expect(threeWayMerge(base, mine, theirs, rules)).toEqual({
                merged: {...base, value: {...value, nodeDataKey: 'manager', path: 'name'}}, conflicts: [],
            });
        });
    });

    describe('keyed arrays', () => {
        const spec = {arrayKey: keyById};
        const item = (id: string, label: string) => ({id, label});

        it('merges changes to different items and keeps items added on both sides', () => {
            const base = {children: [item('a', 'A'), item('b', 'B')]};
            const mine = {children: [item('a', 'A1'), item('b', 'B'), item('m', 'M')]};
            const theirs = {children: [item('t', 'T'), item('a', 'A'), item('b', 'B2')]};

            const result = threeWayMerge(base, mine, theirs, spec);

            expect(result.conflicts).toEqual([]);
            expect(result.merged.children).toEqual([item('t', 'T'), item('a', 'A1'), item('b', 'B2'), item('m', 'M')]);
        });

        it('removes an item that one side removed and the other left unchanged', () => {
            const base = {children: [item('a', 'A'), item('b', 'B')]};

            const result = threeWayMerge(base, {children: [item('a', 'A')]}, {children: [item('a', 'A2'), item('b', 'B')]}, spec);

            expect(result.conflicts).toEqual([]);
            expect(result.merged.children).toEqual([item('a', 'A2')]);
        });

        it('reports an item that one side removed and the other changed', () => {
            const base = {children: [item('a', 'A')]};

            const result = threeWayMerge(base, {children: []}, {children: [item('a', 'A2')]}, spec);

            expect(result.conflicts.map((conflict) => conflict.id)).toEqual(['children/[a]']);
            expect(result.merged.children).toEqual([item('a', 'A2')]);
            expect(threeWayMerge(base, {children: []}, {children: [item('a', 'A2')]}, spec, {'children/[a]': 'mine'}).merged.children)
                .toEqual([]);
        });

        it('takes the order of the side that reordered the items', () => {
            const base = {children: [item('a', 'A'), item('b', 'B'), item('c', 'C')]};
            const mine = {children: [item('c', 'C'), item('a', 'A'), item('b', 'B')]};
            const theirs = {children: [item('a', 'A'), item('b', 'B2'), item('c', 'C')]};

            const result = threeWayMerge(base, mine, theirs, spec);

            expect(result.conflicts).toEqual([]);
            expect(result.merged.children).toEqual([item('c', 'C'), item('a', 'A'), item('b', 'B2')]);
        });

        it('reports the order when both sides reordered the items differently', () => {
            const base = {children: [item('a', 'A'), item('b', 'B'), item('c', 'C')]};
            const mine = {children: [item('c', 'C'), item('a', 'A'), item('b', 'B')]};
            const theirs = {children: [item('b', 'B'), item('a', 'A'), item('c', 'C')]};
            const orderId = pathToId(['children', ORDER_SEGMENT]);

            const result = threeWayMerge(base, mine, theirs, spec);

            expect(result.conflicts.map((conflict) => conflict.id)).toEqual([orderId]);
            expect(result.merged.children.map((child) => child.id)).toEqual(['b', 'a', 'c']);
            expect(threeWayMerge(base, mine, theirs, spec, {[orderId]: 'mine'}).merged.children.map((child) => child.id))
                .toEqual(['c', 'a', 'b']);
        });

        it('merges arrays whose items lack a unique key as a whole', () => {
            const base = {children: [item('a', 'A')]};

            const result = threeWayMerge(base, {children: [item('a', 'A'), item('a', 'A')]}, {children: [item('a', 'A2')]}, spec);

            expect(result.conflicts.map((conflict) => conflict.id)).toEqual(['children']);
        });
    });

    it('turns the whole object into one conflict when the merged object is invalid', () => {
        // One side moved the element into another group, the other side changed it at its old position.
        const base = {groups: [{id: 'g1', children: [{id: 'e', label: 'E'}]}, {id: 'g2', children: []}]};
        const mine = {groups: [{id: 'g1', children: []}, {id: 'g2', children: [{id: 'e', label: 'E'}]}]};
        const theirs = {groups: [{id: 'g1', children: [{id: 'e', label: 'E2'}]}, {id: 'g2', children: []}]};
        const validate = (value: typeof base) => {
            const ids = value.groups.flatMap((group) => group.children.map((child) => child.id));
            return new Set(ids).size === ids.length;
        };

        const result = threeWayMerge(base, mine, theirs, {arrayKey: keyById, validate});

        expect(result.conflicts.map((conflict) => conflict.id)).toEqual(['']);
        expect(result.merged).toEqual(theirs);
        expect(threeWayMerge(base, mine, theirs, {arrayKey: keyById, validate}, {'': 'mine'}).merged).toEqual(mine);
    });

    it('compares the normalized states', () => {
        const normalize = (value: {name: string | null | undefined}) => ({name: value.name ?? null});

        const result = threeWayMerge({name: undefined}, {name: null}, {name: 'neu'}, {normalize});

        expect(result.conflicts).toEqual([]);
        expect(result.merged).toEqual({name: 'neu'});
    });
});
