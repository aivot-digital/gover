import {NoCodeEditorMetadata} from '../../../models/functions/no-code-editor-metadata';
import {v4 as uuid4} from 'uuid';
import {isNoCodeExpression, NoCodeExpression, NoCodeOperand} from '../../../models/functions/no-code-expression';

// IDs exist only inside the editor; the persisted NoCodeOperand JSON stays unchanged for the backend.
const originalEmptyOperands = Symbol('originalEmptyOperands');

type WithNodeId<T> = T & { id: string };

export type NoCodeEditorExpressionNode = WithNodeId<Omit<NoCodeExpression, 'operands'>> & {
    operands: (NoCodeEditorNode | null)[];
    [originalEmptyOperands]?: { present: boolean; value: null | undefined };
};

export type NoCodeEditorLeafNode = WithNodeId<Exclude<NoCodeOperand, NoCodeExpression>>;

export type NoCodeEditorNode = NoCodeEditorExpressionNode | NoCodeEditorLeafNode;

/** A slot is addressed by its parent expression and the operand index; `parentId` null is the root slot. */
export interface NoCodeEditorSlot {
    parentId: string | null;
    index: number;
}

export type NoCodeEditorAction =
    | { type: 'setEditorMetadata'; metadata: NoCodeEditorMetadata | undefined }
    /** Fills an empty slot. An index past the last operand appends; an occupied slot stays unchanged. */
    | { type: 'insertNode'; parentId: string | null; index: number; node: NoCodeEditorNode }
    | { type: 'removeNode'; nodeId: string }
    /** Replaces a node, keeping its ID so focus and popovers stay attached while its value is edited. */
    | { type: 'replaceNode'; nodeId: string; node: NoCodeEditorNode }
    /** Moves a node into a slot. An occupied target swaps places with the moved node. */
    | { type: 'moveNode'; nodeId: string; parentId: string | null; index: number }
    /** Encloses a node as the first operand of a new expression. */
    | { type: 'wrapNode'; nodeId: string; wrapperId: string; operatorIdentifier: string; parameterCount?: number };

export function toNoCodeEditorNode(operand: NoCodeOperand | null | undefined): NoCodeEditorNode | null {
    if (operand == null) {
        return null;
    }

    if (isNoCodeExpression(operand)) {
        return {
            ...operand,
            id: uuid4(),
            // Keep legacy null/omitted parameter lists when an unrelated part of the expression is edited.
            ...(operand.operands == null ? {
                [originalEmptyOperands]: {
                    present: Object.hasOwn(operand, 'operands'),
                    value: operand.operands,
                },
            } : {}),
            operands: (operand.operands ?? []).map((child) => toNoCodeEditorNode(child)),
        };
    }

    return {
        ...operand,
        id: uuid4(),
    };
}

export function toNoCodeOperand(node: NoCodeEditorNode | null): NoCodeOperand | null {
    if (node == null) {
        return null;
    }

    if (node.type === 'NoCodeExpression') {
        const {
            id: _id,
            [originalEmptyOperands]: original,
            ...expression
        } = node;
        if (expression.operands.length === 0 && original != null) {
            const {
                operands: _operands,
                ...fields
            } = expression;
            return {...fields, ...(original.present ? {operands: original.value} : {})} as NoCodeExpression;
        }
        return {
            ...expression,
            operands: expression.operands.map(toNoCodeOperand),
        };
    }

    const {
        id: _id,
        ...leaf
    } = node;
    // Omit is not distributive over the leaf union, so the discriminated type is restored here.
    return leaf as NoCodeOperand;
}

export function findNoCodeEditorNode(root: NoCodeEditorNode | null, nodeId: string): NoCodeEditorNode | null {
    if (root == null || root.id === nodeId) {
        return root;
    }

    if (root.type !== 'NoCodeExpression') {
        return null;
    }

    for (const child of root.operands) {
        const found = findNoCodeEditorNode(child, nodeId);
        if (found != null) {
            return found;
        }
    }

    return null;
}

export function findNoCodeEditorSlot(root: NoCodeEditorNode | null, nodeId: string): NoCodeEditorSlot | null {
    if (root == null) {
        return null;
    }

    if (root.id === nodeId) {
        return {
            parentId: null,
            index: 0,
        };
    }

    if (root.type !== 'NoCodeExpression') {
        return null;
    }

    for (let index = 0; index < root.operands.length; index++) {
        const child = root.operands[index];
        if (child?.id === nodeId) {
            return {
                parentId: root.id,
                index,
            };
        }

        const found = findNoCodeEditorSlot(child, nodeId);
        if (found != null) {
            return found;
        }
    }

    return null;
}

export function noCodeEditorReducer(root: NoCodeEditorNode | null, action: NoCodeEditorAction): NoCodeEditorNode | null {
    if (action.type === 'setEditorMetadata') {
        if (root == null) return root;
        const {
            editorMetadata: _metadata,
            ...node
        } = root;
        return {...node, ...(action.metadata == null ? {} : {editorMetadata: action.metadata})} as NoCodeEditorNode;
    }
    // Root metadata belongs to the whole rule, not the operator that happens to be at its root.
    // Detaching it before tree edits also prevents wrapping from copying it into a child.
    if (root?.editorMetadata != null) {
        const {
            editorMetadata,
            ...node
        } = root;
        const next = reduceTree(node as NoCodeEditorNode, action);
        // Move-target checks use identity to distinguish rejected actions from valid edits.
        if (next === node) return root;
        return next == null ? null : {
            ...next,
            editorMetadata,
        };
    }
    return reduceTree(root, action);
}

function reduceTree(root: NoCodeEditorNode | null, action: Exclude<NoCodeEditorAction, {
    type: 'setEditorMetadata'
}>): NoCodeEditorNode | null {
    switch (action.type) {
        case 'insertNode': {
            if (getSlot(root, action.parentId, action.index) != null) {
                return root;
            }
            return setSlot(root, action.parentId, action.index, action.node);
        }

        case 'removeNode': {
            const slot = findNoCodeEditorSlot(root, action.nodeId);
            return slot == null ? root : setSlot(root, slot.parentId, slot.index, null);
        }

        case 'replaceNode':
            return updateNode(root, action.nodeId, () => ({
                ...action.node,
                id: action.nodeId,
            }));

        case 'moveNode': {
            const node = findNoCodeEditorNode(root, action.nodeId);
            const source = findNoCodeEditorSlot(root, action.nodeId);
            if (node == null || source == null) {
                return root;
            }

            // Validate the destination before removing the source; a stale target must never lose a subtree.
            if (!Number.isInteger(action.index) || action.index < 0 ||
                (action.parentId == null ? action.index !== 0 : findNoCodeEditorNode(root, action.parentId)?.type !== 'NoCodeExpression')) {
                return root;
            }

            // A node cannot move into its own subtree, and a swapped occupant cannot land inside itself.
            if (action.parentId != null && findNoCodeEditorNode(node, action.parentId) != null) {
                return root;
            }
            const occupant = getSlot(root, action.parentId, action.index);
            if (occupant != null && findNoCodeEditorNode(occupant, action.nodeId) != null) {
                return root;
            }

            const withoutNode = setSlot(root, source.parentId, source.index, occupant);
            return setSlot(withoutNode, action.parentId, action.index, node);
        }

        case 'wrapNode':
            return updateNode(root, action.nodeId, (node) => ({
                type: 'NoCodeExpression',
                id: action.wrapperId,
                operatorIdentifier: action.operatorIdentifier,
                operands: Array.from({length: Math.max(1, action.parameterCount ?? 1)}, (_, index) => index === 0 ? node : null),
            }));
    }
}

function getSlot(root: NoCodeEditorNode | null, parentId: string | null, index: number): NoCodeEditorNode | null {
    if (parentId == null) {
        return root;
    }

    const parent = findNoCodeEditorNode(root, parentId);
    return parent?.type === 'NoCodeExpression' ? parent.operands[index] ?? null : null;
}

function setSlot(
    root: NoCodeEditorNode | null,
    parentId: string | null,
    index: number,
    value: NoCodeEditorNode | null,
): NoCodeEditorNode | null {
    if (parentId == null) {
        return value;
    }

    if (index < 0) {
        return root;
    }

    return updateNode(root, parentId, (parent) => {
        if (parent.type !== 'NoCodeExpression') {
            return parent;
        }

        const operands = [...parent.operands];
        while (operands.length < index) {
            operands.push(null);
        }
        operands[index] = value;
        return {
            ...parent,
            operands,
        };
    });
}

/** Rebuilds only the path to the updated node, so untouched subtrees keep their identity for React. */
function updateNode(
    node: NoCodeEditorNode | null,
    nodeId: string,
    update: (node: NoCodeEditorNode) => NoCodeEditorNode,
): NoCodeEditorNode | null {
    if (node == null) {
        return node;
    }

    if (node.id === nodeId) {
        return update(node);
    }

    if (node.type !== 'NoCodeExpression') {
        return node;
    }

    let changed = false;
    const operands = node.operands.map((child) => {
        const updated = updateNode(child, nodeId, update);
        changed ||= updated !== child;
        return updated;
    });

    return changed ? {
        ...node,
        operands,
    } : node;
}

/** Excludes authoring-only data from evaluation requests and result freshness checks. */
export function withoutNoCodeEditorMetadata<T extends NoCodeOperand | NoCodeEditorNode>(operand: T): T {
    const {
        editorMetadata: _metadata,
        ...node
    } = operand;
    return (operand.type === 'NoCodeExpression'
        ? {
            ...node,
            operands: operand.operands?.map(child => child == null ? child : withoutNoCodeEditorMetadata(child)),
        }
        : node) as T;
}
