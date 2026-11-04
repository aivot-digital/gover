import {describe, expect, it} from 'vitest';
import {type NoCodeOperand} from '../../../models/functions/no-code-expression';
import {
    findPillNoCodeNode,
    type PillNoCodeExpressionNode,
    type PillNoCodeNode,
    pillNoCodeReducer,
    toNoCodeOperand,
    toPillNoCodeNode,
} from './pill-no-code-store';

const value = (text: string): NoCodeOperand => ({type: 'NoCodeStaticValue', value: text});

// equals(a, add(b, c))
function createTree() {
    const root = toPillNoCodeNode({
        type: 'NoCodeExpression',
        operatorIdentifier: 'equals',
        operands: [
            value('a'),
            {type: 'NoCodeExpression', operatorIdentifier: 'add', operands: [value('b'), value('c')]},
        ],
    }) as PillNoCodeExpressionNode;
    const add = root.operands[1] as PillNoCodeExpressionNode;
    return {root, a: root.operands[0]!, add, b: add.operands[0]!, c: add.operands[1]!};
}

function operandOf(root: PillNoCodeNode | null) {
    return toNoCodeOperand(root);
}

describe('pill no-code store', () => {
    it('assigns unique IDs and strips them again for persistence', () => {
        const {root, a, add, b, c} = createTree();

        expect(new Set([root.id, a.id, add.id, b.id, c.id]).size).toBe(5);
        expect(JSON.stringify(operandOf(root))).not.toContain('"id"');
        expect(operandOf(root)).toEqual({
            type: 'NoCodeExpression',
            operatorIdentifier: 'equals',
            operands: [value('a'), {type: 'NoCodeExpression', operatorIdentifier: 'add', operands: [value('b'), value('c')]}],
        });
    });

    it('fills empty slots, appends past the end and leaves occupied slots unchanged', () => {
        const {root, a, add} = createTree();
        const emptied = pillNoCodeReducer(root, {type: 'removeNode', nodeId: a.id});
        const inserted = toPillNoCodeNode(value('x'))!;

        const filled = pillNoCodeReducer(emptied, {type: 'insertNode', parentId: root.id, index: 0, node: inserted});
        expect(findPillNoCodeNode(filled, inserted.id)).toBe(inserted);

        expect(pillNoCodeReducer(root, {type: 'insertNode', parentId: root.id, index: 0, node: inserted})).toBe(root);

        const appended = pillNoCodeReducer(root, {type: 'insertNode', parentId: add.id, index: 3, node: inserted});
        expect((findPillNoCodeNode(appended, add.id) as PillNoCodeExpressionNode).operands.map((o) => o?.id ?? null))
            .toEqual([add.operands[0]!.id, add.operands[1]!.id, null, inserted.id]);
    });

    it('removes a subtree and keeps untouched siblings by reference', () => {
        const {root, a, add} = createTree();

        const next = pillNoCodeReducer(root, {type: 'removeNode', nodeId: add.id}) as PillNoCodeExpressionNode;

        expect(next.operands).toEqual([a, null]);
        expect(next.operands[0]).toBe(a);
        expect(pillNoCodeReducer(root, {type: 'removeNode', nodeId: root.id})).toBeNull();
    });

    it('replaces a node and keeps its ID', () => {
        const {root, b} = createTree();
        const replacement = toPillNoCodeNode(value('B'))!;

        const next = pillNoCodeReducer(root, {type: 'replaceNode', nodeId: b.id, node: replacement});

        expect(findPillNoCodeNode(next, b.id)).toEqual({...replacement, id: b.id});
    });

    it('moves a node into an empty slot or swaps it with the occupant', () => {
        const {root, a, add, b, c} = createTree();

        const swapped = pillNoCodeReducer(root, {type: 'moveNode', nodeId: b.id, parentId: add.id, index: 1});
        expect((findPillNoCodeNode(swapped, add.id) as PillNoCodeExpressionNode).operands).toEqual([c, b]);

        const emptied = pillNoCodeReducer(root, {type: 'removeNode', nodeId: a.id});
        const moved = pillNoCodeReducer(emptied, {type: 'moveNode', nodeId: c.id, parentId: root.id, index: 0}) as PillNoCodeExpressionNode;
        expect(moved.operands[0]).toBe(c);
        expect((moved.operands[1] as PillNoCodeExpressionNode).operands).toEqual([b, null]);
    });

    it('rejects moves that would nest a node inside itself', () => {
        const {root, a, add, b} = createTree();

        expect(pillNoCodeReducer(root, {type: 'moveNode', nodeId: add.id, parentId: add.id, index: 0})).toBe(root);
        // b sits inside add, so swapping the two would nest add inside itself.
        expect(pillNoCodeReducer(root, {type: 'moveNode', nodeId: b.id, parentId: root.id, index: 1})).toBe(root);
        expect(pillNoCodeReducer(root, {type: 'moveNode', nodeId: a.id, parentId: null, index: 0})).toBe(root);
        expect(pillNoCodeReducer(root, {type: 'moveNode', nodeId: 'unknown', parentId: root.id, index: 0})).toBe(root);
    });

    it('wraps a node as the first operand of a new expression', () => {
        const {root, a} = createTree();

        const next = pillNoCodeReducer(root, {type: 'wrapNode', nodeId: a.id, wrapperId: 'wrapper', operatorIdentifier: 'not'});

        expect(findPillNoCodeNode(next, 'wrapper')).toEqual({
            type: 'NoCodeExpression',
            id: 'wrapper',
            operatorIdentifier: 'not',
            operands: [a],
        });
    });
});
