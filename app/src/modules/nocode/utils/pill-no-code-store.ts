import {v4 as uuid4} from 'uuid';
import {isNoCodeExpression, NoCodeExpression, NoCodeOperand} from '../../../models/functions/no-code-expression';

// IDs exist only inside the editor; the persisted NoCodeOperand JSON stays unchanged for the backend and the old editor.
type WithNodeId<T> = T & {id: string};

export type PillNoCodeExpressionNode = WithNodeId<Omit<NoCodeExpression, 'operands'>> & {
    operands: (PillNoCodeNode | null)[];
};

export type PillNoCodeLeafNode = WithNodeId<Exclude<NoCodeOperand, NoCodeExpression>>;

export type PillNoCodeNode = PillNoCodeExpressionNode | PillNoCodeLeafNode;

/** A slot is addressed by its parent expression and the operand index; `parentId` null is the root slot. */
export interface PillNoCodeSlot {
    parentId: string | null;
    index: number;
}

export type PillNoCodeAction =
    /** Fills an empty slot. An index past the last operand appends; an occupied slot stays unchanged. */
    | {type: 'insertNode'; parentId: string | null; index: number; node: PillNoCodeNode}
    | {type: 'removeNode'; nodeId: string}
    /** Replaces a node, keeping its ID so focus and popovers stay attached while its value is edited. */
    | {type: 'replaceNode'; nodeId: string; node: PillNoCodeNode}
    /** Moves a node into a slot. An occupied target swaps places with the moved node. */
    | {type: 'moveNode'; nodeId: string; parentId: string | null; index: number}
    /** Encloses a node as the first operand of a new expression. */
    | {type: 'wrapNode'; nodeId: string; wrapperId: string; operatorIdentifier: string};

export function toPillNoCodeNode(operand: NoCodeOperand | null | undefined): PillNoCodeNode | null {
    if (operand == null) {
        return null;
    }

    if (isNoCodeExpression(operand)) {
        return {
            ...operand,
            id: uuid4(),
            operands: (operand.operands ?? []).map((child) => toPillNoCodeNode(child)),
        };
    }

    return {...operand, id: uuid4()};
}

export function toNoCodeOperand(node: PillNoCodeNode | null): NoCodeOperand | null {
    if (node == null) {
        return null;
    }

    if (node.type === 'NoCodeExpression') {
        const {id: _id, ...expression} = node;
        return {
            ...expression,
            operands: expression.operands.map(toNoCodeOperand),
        };
    }

    const {id: _id, ...leaf} = node;
    // Omit is not distributive over the leaf union, so the discriminated type is restored here.
    return leaf as NoCodeOperand;
}

export function findPillNoCodeNode(root: PillNoCodeNode | null, nodeId: string): PillNoCodeNode | null {
    if (root == null || root.id === nodeId) {
        return root;
    }

    if (root.type !== 'NoCodeExpression') {
        return null;
    }

    for (const child of root.operands) {
        const found = findPillNoCodeNode(child, nodeId);
        if (found != null) {
            return found;
        }
    }

    return null;
}

export function findPillNoCodeSlot(root: PillNoCodeNode | null, nodeId: string): PillNoCodeSlot | null {
    if (root == null) {
        return null;
    }

    if (root.id === nodeId) {
        return {parentId: null, index: 0};
    }

    if (root.type !== 'NoCodeExpression') {
        return null;
    }

    for (let index = 0; index < root.operands.length; index++) {
        const child = root.operands[index];
        if (child?.id === nodeId) {
            return {parentId: root.id, index};
        }

        const found = findPillNoCodeSlot(child, nodeId);
        if (found != null) {
            return found;
        }
    }

    return null;
}

export function pillNoCodeReducer(root: PillNoCodeNode | null, action: PillNoCodeAction): PillNoCodeNode | null {
    switch (action.type) {
        case 'insertNode': {
            if (getSlot(root, action.parentId, action.index) != null) {
                return root;
            }
            return setSlot(root, action.parentId, action.index, action.node);
        }

        case 'removeNode': {
            const slot = findPillNoCodeSlot(root, action.nodeId);
            return slot == null ? root : setSlot(root, slot.parentId, slot.index, null);
        }

        case 'replaceNode':
            return updateNode(root, action.nodeId, () => ({...action.node, id: action.nodeId}));

        case 'moveNode': {
            const node = findPillNoCodeNode(root, action.nodeId);
            const source = findPillNoCodeSlot(root, action.nodeId);
            if (node == null || source == null) {
                return root;
            }

            // A node cannot move into its own subtree, and a swapped occupant cannot land inside itself.
            if (action.parentId != null && findPillNoCodeNode(node, action.parentId) != null) {
                return root;
            }
            const occupant = getSlot(root, action.parentId, action.index);
            if (occupant != null && findPillNoCodeNode(occupant, action.nodeId) != null) {
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
                operands: [node],
            }));
    }
}

function getSlot(root: PillNoCodeNode | null, parentId: string | null, index: number): PillNoCodeNode | null {
    if (parentId == null) {
        return root;
    }

    const parent = findPillNoCodeNode(root, parentId);
    return parent?.type === 'NoCodeExpression' ? parent.operands[index] ?? null : null;
}

function setSlot(
    root: PillNoCodeNode | null,
    parentId: string | null,
    index: number,
    value: PillNoCodeNode | null,
): PillNoCodeNode | null {
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
        return {...parent, operands};
    });
}

/** Rebuilds only the path to the updated node, so untouched subtrees keep their identity for React. */
function updateNode(
    node: PillNoCodeNode | null,
    nodeId: string,
    update: (node: PillNoCodeNode) => PillNoCodeNode,
): PillNoCodeNode | null {
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

    return changed ? {...node, operands} : node;
}
