import {useCallback, useEffect, useRef, useState} from 'react';
import {NoCodeOperand} from '../../../models/functions/no-code-expression';
import {
    NoCodeEditorAction,
    NoCodeEditorNode,
    noCodeEditorReducer,
    toNoCodeOperand,
    toNoCodeEditorNode,
} from '../utils/no-code-editor-store';

const HISTORY_LIMIT = 100;

interface EditorHistory {
    past: (NoCodeEditorNode | null)[];
    present: NoCodeEditorNode | null;
    future: (NoCodeEditorNode | null)[];
}

interface HistoryActions {
    canUndo: boolean;
    canRedo: boolean;
    undo: () => void;
    redo: () => void;
}

/** Local history retains node IDs; only plain operands cross the persistence boundary. */
export function useNoCodeEditorStore(
    operand: NoCodeOperand | null | undefined,
    onChange: (operand: NoCodeOperand | null) => void,
    editable = true,
): [NoCodeEditorNode | null, (action: NoCodeEditorAction) => void, HistoryActions] {
    const [history, setHistory] = useState<EditorHistory>(() => ({
        past: [],
        present: toNoCodeEditorNode(operand),
        future: [],
    }));
    const historyRef = useRef(history);
    const emittedRef = useRef(JSON.stringify(operand ?? null));
    const onChangeRef = useRef(onChange);
    const editableRef = useRef(editable);
    onChangeRef.current = onChange;
    editableRef.current = editable;

    // Cloned echoes from the parent must preserve focus and history. A genuinely external replacement starts a new history.
    useEffect(() => {
        const serialized = JSON.stringify(operand ?? null);
        if (serialized !== emittedRef.current) {
            emittedRef.current = serialized;
            historyRef.current = {
                past: [],
                present: toNoCodeEditorNode(operand),
                future: [],
            };
            setHistory(historyRef.current);
        }
    }, [operand]);

    const emit = useCallback((next: EditorHistory) => {
        historyRef.current = next;
        setHistory(next);
        const nextOperand = toNoCodeOperand(next.present);
        emittedRef.current = JSON.stringify(nextOperand);
        onChangeRef.current(nextOperand);
    }, []);

    const dispatch = useCallback((action: NoCodeEditorAction) => {
        if (!editableRef.current) {
            return;
        }
        const current = historyRef.current;
        const next = noCodeEditorReducer(current.present, action);
        if (next === current.present || JSON.stringify(toNoCodeOperand(next)) === emittedRef.current) {
            return;
        }
        emit({
            past: [...current.past, current.present].slice(-HISTORY_LIMIT),
            present: next,
            future: [],
        });
    }, [emit]);

    const undo = useCallback(() => {
        const current = historyRef.current;
        if (!editableRef.current || current.past.length === 0) {
            return;
        }
        emit({
            past: current.past.slice(0, -1),
            present: current.past[current.past.length - 1],
            future: [current.present, ...current.future],
        });
    }, [emit]);

    const redo = useCallback(() => {
        const current = historyRef.current;
        if (!editableRef.current || current.future.length === 0) {
            return;
        }
        emit({
            past: [...current.past, current.present],
            present: current.future[0],
            future: current.future.slice(1),
        });
    }, [emit]);

    return [history.present, dispatch, {
        canUndo: editable && history.past.length > 0,
        canRedo: editable && history.future.length > 0,
        undo,
        redo,
    }];
}
