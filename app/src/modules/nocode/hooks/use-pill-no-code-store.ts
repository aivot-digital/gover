import {useCallback, useEffect, useRef, useState} from 'react';
import {NoCodeOperand} from '../../../models/functions/no-code-expression';
import {
    PillNoCodeAction,
    PillNoCodeNode,
    pillNoCodeReducer,
    toNoCodeOperand,
    toPillNoCodeNode,
} from '../utils/pill-no-code-store';

/**
 * Holds the editor tree with stable node IDs and reports every change as a plain NoCodeOperand.
 */
export function usePillNoCodeStore(
    operand: NoCodeOperand | null | undefined,
    onChange: (operand: NoCodeOperand | null) => void,
): [PillNoCodeNode | null, (action: PillNoCodeAction) => void] {
    const [root, setRoot] = useState(() => toPillNoCodeNode(operand));
    const rootRef = useRef(root);
    const emittedRef = useRef(JSON.stringify(operand ?? null));
    const onChangeRef = useRef(onChange);
    onChangeRef.current = onChange;

    // Only a value that differs from the last emitted one rebuilds the tree. Parents that clone the
    // value would otherwise regenerate all IDs on every edit and drop focus.
    useEffect(() => {
        const serialized = JSON.stringify(operand ?? null);
        if (serialized !== emittedRef.current) {
            emittedRef.current = serialized;
            rootRef.current = toPillNoCodeNode(operand);
            setRoot(rootRef.current);
        }
    }, [operand]);

    const dispatch = useCallback((action: PillNoCodeAction) => {
        const next = pillNoCodeReducer(rootRef.current, action);
        if (next === rootRef.current) {
            return;
        }

        rootRef.current = next;
        setRoot(next);
        const nextOperand = toNoCodeOperand(next);
        emittedRef.current = JSON.stringify(nextOperand);
        onChangeRef.current(nextOperand);
    }, []);

    return [root, dispatch];
}
