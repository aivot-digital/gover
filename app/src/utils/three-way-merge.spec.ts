import {describe, expect, it} from 'vitest';
import {keyById, ORDER_SEGMENT, pathToId, threeWayMerge} from './three-way-merge';

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
