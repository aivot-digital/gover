import {describe, expect, it} from 'vitest';
import {type NoCodeOperand} from '../../../models/functions/no-code-expression';
import {
    findNoCodeEditorNode,
    type NoCodeEditorExpressionNode,
    type NoCodeEditorNode,
    noCodeEditorReducer,
    toNoCodeOperand,
    toNoCodeEditorNode,
} from './no-code-editor-store';

const value = (text: string): NoCodeOperand => ({
    type: 'NoCodeStaticValue',
    value: text,
});

// equals(a, add(b, c))
function createTree() {
    const root = toNoCodeEditorNode({
        type: 'NoCodeExpression',
        operatorIdentifier: 'equals',
        operands: [
            value('a'),
            {
                type: 'NoCodeExpression',
                operatorIdentifier: 'add',
                operands: [value('b'), value('c')],
            },
        ],
    }) as NoCodeEditorExpressionNode;
    const add = root.operands[1] as NoCodeEditorExpressionNode;
    return {
        root,
        a: root.operands[0]!,
        add,
        b: add.operands[0]!,
        c: add.operands[1]!,
    };
}

function operandOf(root: NoCodeEditorNode | null) {
    return toNoCodeOperand(root);
}

describe('No-Code store', () => {
    it('assigns unique IDs and strips them again for persistence', () => {
        const {
            root,
            a,
            add,
            b,
            c,
        } = createTree();

        expect(new Set([root.id, a.id, add.id, b.id, c.id]).size).toBe(5);
        expect(JSON.stringify(operandOf(root))).not.toContain('"id"');
        expect(operandOf(root)).toEqual({
            type: 'NoCodeExpression',
            operatorIdentifier: 'equals',
            operands: [value('a'), {
                type: 'NoCodeExpression',
                operatorIdentifier: 'add',
                operands: [value('b'), value('c')],
            }],
        });
    });

    it('fills empty slots, appends past the end and leaves occupied slots unchanged', () => {
        const {
            root,
            a,
            add,
        } = createTree();
        const emptied = noCodeEditorReducer(root, {
            type: 'removeNode',
            nodeId: a.id,
        });
        const inserted = toNoCodeEditorNode(value('x'))!;

        const filled = noCodeEditorReducer(emptied, {
            type: 'insertNode',
            parentId: root.id,
            index: 0,
            node: inserted,
        });
        expect(findNoCodeEditorNode(filled, inserted.id)).toBe(inserted);

        expect(noCodeEditorReducer(root, {
            type: 'insertNode',
            parentId: root.id,
            index: 0,
            node: inserted,
        })).toBe(root);

        const appended = noCodeEditorReducer(root, {
            type: 'insertNode',
            parentId: add.id,
            index: 3,
            node: inserted,
        });
        expect((findNoCodeEditorNode(appended, add.id) as NoCodeEditorExpressionNode).operands.map((o) => o?.id ?? null))
            .toEqual([add.operands[0]!.id, add.operands[1]!.id, null, inserted.id]);
    });

    it('removes a subtree and keeps untouched siblings by reference', () => {
        const {
            root,
            a,
            add,
        } = createTree();

        const next = noCodeEditorReducer(root, {
            type: 'removeNode',
            nodeId: add.id,
        }) as NoCodeEditorExpressionNode;

        expect(next.operands).toEqual([a, null]);
        expect(next.operands[0]).toBe(a);
        expect(noCodeEditorReducer(root, {
            type: 'removeNode',
            nodeId: root.id,
        })).toBeNull();
    });

    it('replaces a node and keeps its ID', () => {
        const {
            root,
            b,
        } = createTree();
        const replacement = toNoCodeEditorNode(value('B'))!;

        const next = noCodeEditorReducer(root, {
            type: 'replaceNode',
            nodeId: b.id,
            node: replacement,
        });

        expect(findNoCodeEditorNode(next, b.id)).toEqual({
            ...replacement,
            id: b.id,
        });
    });

    it('moves a node into an empty slot or swaps it with the occupant', () => {
        const {
            root,
            a,
            add,
            b,
            c,
        } = createTree();

        const swapped = noCodeEditorReducer(root, {
            type: 'moveNode',
            nodeId: b.id,
            parentId: add.id,
            index: 1,
        });
        expect((findNoCodeEditorNode(swapped, add.id) as NoCodeEditorExpressionNode).operands).toEqual([c, b]);

        const emptied = noCodeEditorReducer(root, {
            type: 'removeNode',
            nodeId: a.id,
        });
        const moved = noCodeEditorReducer(emptied, {
            type: 'moveNode',
            nodeId: c.id,
            parentId: root.id,
            index: 0,
        }) as NoCodeEditorExpressionNode;
        expect(moved.operands[0]).toBe(c);
        expect((moved.operands[1] as NoCodeEditorExpressionNode).operands).toEqual([b, null]);
    });

    it('rejects moves that would nest a node inside itself', () => {
        const {
            root,
            a,
            add,
            b,
        } = createTree();

        expect(noCodeEditorReducer(root, {
            type: 'moveNode',
            nodeId: add.id,
            parentId: add.id,
            index: 0,
        })).toBe(root);
        // b sits inside add, so swapping the two would nest add inside itself.
        expect(noCodeEditorReducer(root, {
            type: 'moveNode',
            nodeId: b.id,
            parentId: root.id,
            index: 1,
        })).toBe(root);
        expect(noCodeEditorReducer(root, {
            type: 'moveNode',
            nodeId: a.id,
            parentId: null,
            index: 0,
        })).toBe(root);
        expect(noCodeEditorReducer(root, {
            type: 'moveNode',
            nodeId: 'unknown',
            parentId: root.id,
            index: 0,
        })).toBe(root);
    });

    it('wraps a node as the first operand of a new expression', () => {
        const {
            root,
            a,
        } = createTree();

        const next = noCodeEditorReducer(root, {
            type: 'wrapNode',
            nodeId: a.id,
            wrapperId: 'wrapper',
            operatorIdentifier: 'not',
        });

        expect(findNoCodeEditorNode(next, 'wrapper')).toEqual({
            type: 'NoCodeExpression',
            id: 'wrapper',
            operatorIdentifier: 'not',
            operands: [a],
        });
    });

    it('rejects impossible moves identically when the expression has authoring metadata', () => {
        const {
            root,
            a,
            add,
            b,
        } = createTree();
        root.editorMetadata = {notes: 'Interne Notiz'};

        expect(noCodeEditorReducer(root, {
            type: 'moveNode',
            nodeId: add.id,
            parentId: add.id,
            index: 0,
        })).toBe(root);
        expect(noCodeEditorReducer(root, {
            type: 'moveNode',
            nodeId: b.id,
            parentId: root.id,
            index: 1,
        })).toBe(root);
        expect(noCodeEditorReducer(root, {
            type: 'moveNode',
            nodeId: a.id,
            parentId: null,
            index: 0,
        })).toBe(root);
        expect(noCodeEditorReducer(root, {
            type: 'insertNode',
            parentId: root.id,
            index: 0,
            node: b,
        })).toBe(root);
    });

    it('preserves the source when a move target has disappeared or is not an expression slot', () => {
        const {
            root,
            a,
            b,
        } = createTree();

        expect(noCodeEditorReducer(root, {
            type: 'moveNode',
            nodeId: b.id,
            parentId: 'removed-parent',
            index: 0,
        })).toBe(root);
        expect(noCodeEditorReducer(root, {
            type: 'moveNode',
            nodeId: b.id,
            parentId: a.id,
            index: 0,
        })).toBe(root);
        expect(noCodeEditorReducer(root, {
            type: 'moveNode',
            nodeId: b.id,
            parentId: root.id,
            index: -1,
        })).toBe(root);
        expect(noCodeEditorReducer(root, {
            type: 'moveNode',
            nodeId: b.id,
            parentId: root.id,
            index: 0.5,
        })).toBe(root);
    });
});


describe('stored expression compatibility', () => {
    const samples: NoCodeOperand[] = [
        ...[null, '', '0', 'false', '„Zitat“'].map(value => ({
            type: 'NoCodeStaticValue' as const,
            value,
        })),
        {
            type: 'NoCodeReference',
            elementId: 'existing-field',
        },
        {
            type: 'NoCodeProcessDataReference',
            path: 'items[*].amount',
        },
        {
            type: 'NoCodeInstanceDataReference',
            path: 'caseNumber',
        },
        {
            type: 'NoCodeNodeDataReference',
            nodeDataKey: 'check',
            path: 'result.items[0]',
        },
        {
            type: 'NoCodeExpression',
            operatorIdentifier: 'extension:unknown',
            operands: null,
        },
        {
            type: 'NoCodeExpression',
            operatorIdentifier: 'extension:unknown',
        } as NoCodeOperand,
        {
            type: 'NoCodeExpression',
            operatorIdentifier: 'extension:unknown',
            operands: [null, value('old')],
        },
    ];
    it.each(samples)('round-trips stored values and unknown extension operators: %j', (stored) => {
        expect(toNoCodeOperand(toNoCodeEditorNode(stored))).toEqual(stored);
    });
    it('preserves the stored shape of an unknown sibling when editing another value', () => {
        const stored: NoCodeOperand = {
            type: 'NoCodeExpression',
            operatorIdentifier: 'equals',
            operands: [samples[9], value('before')],
        };
        const root = toNoCodeEditorNode(stored) as NoCodeEditorExpressionNode;
        const changed = noCodeEditorReducer(root, {
            type: 'replaceNode',
            nodeId: root.operands[1]!.id,
            node: toNoCodeEditorNode(value('after'))!,
        });
        expect(toNoCodeOperand(changed)).toEqual({
            ...stored,
            operands: [samples[9], value('after')],
        });
    });
    it('loads, edits and serializes a large tree without losing siblings', () => {
        const stored: NoCodeOperand = {
            type: 'NoCodeExpression',
            operatorIdentifier: 'many',
            operands: Array.from({length: 5000}, (_, i) => value(String(i))),
        };
        const start = performance.now();
        const root = toNoCodeEditorNode(stored) as NoCodeEditorExpressionNode;
        const changed = noCodeEditorReducer(root, {
            type: 'replaceNode',
            nodeId: root.operands[2500]!.id,
            node: toNoCodeEditorNode(value('changed'))!,
        });
        const saved = toNoCodeOperand(changed);
        expect(saved).toEqual({
            ...stored,
            operands: stored.operands!.map((child, i) => i === 2500 ? value('changed') : child),
        });
        expect(performance.now() - start).toBeLessThan(5000);
    });
});
