import {act, renderHook} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {NoCodeOperand} from '../../../models/functions/no-code-expression';
import {toNoCodeOperand, toNoCodeEditorNode} from '../utils/no-code-editor-store';
import {useNoCodeEditorStore} from './use-no-code-editor-store';

const expression: NoCodeOperand = {
    type: 'NoCodeExpression',
    operatorIdentifier: 'equals',
    operands: [{
        type: 'NoCodeStaticValue',
        value: 'a',
    }, {
        type: 'NoCodeStaticValue',
        value: 'b',
    }],
};

describe('useNoCodeEditorStore', () => {
    it('restores deleted subtrees with the same IDs and preserves history on parent echoes', () => {
        const onChange = vi.fn();
        const {
            result,
            rerender,
        } = renderHook(({operand}) => useNoCodeEditorStore(operand, onChange), {
            initialProps: {operand: expression as NoCodeOperand | null},
        });
        const original = result.current[0]!;
        act(() => result.current[1]({
            type: 'removeNode',
            nodeId: original.id,
        }));
        expect(onChange).toHaveBeenLastCalledWith(null);
        rerender({operand: null});
        expect(result.current[2].canUndo).toBe(true);
        act(() => result.current[2].undo());
        expect(result.current[0]).toBe(original);
        expect(onChange).toHaveBeenLastCalledWith(expression);
        rerender({operand: structuredClone(expression)});
        expect(result.current[2].canRedo).toBe(true);
        act(() => result.current[2].redo());
        expect(result.current[0]).toBeNull();
        expect(onChange).toHaveBeenLastCalledWith(null);
    });

    it('clears redo after a new edit and resets history on external replacement', () => {
        const {
            result,
            rerender,
        } = renderHook(({operand}) => useNoCodeEditorStore(operand, vi.fn()), {
            initialProps: {operand: expression as NoCodeOperand},
        });
        act(() => result.current[1]({
            type: 'removeNode',
            nodeId: result.current[0]!.id,
        }));
        act(() => result.current[2].undo());
        act(() => result.current[1]({
            type: 'replaceNode',
            nodeId: result.current[0]!.id,
            node: toNoCodeEditorNode({
                type: 'NoCodeStaticValue',
                value: 'c',
            })!,
        }));
        expect(result.current[2].canRedo).toBe(false);
        const external: NoCodeOperand = {
            type: 'NoCodeStaticValue',
            value: 'external',
        };
        rerender({operand: external});
        expect(toNoCodeOperand(result.current[0])).toEqual(external);
        expect(result.current[2].canUndo).toBe(false);
        expect(result.current[2].canRedo).toBe(false);
    });

    it('blocks mutations and existing history callbacks immediately when editing is disabled', () => {
        const onChange = vi.fn();
        const {
            result,
            rerender,
        } = renderHook(({editable}) => useNoCodeEditorStore(expression, onChange, editable), {
            initialProps: {editable: true},
        });
        const nodeId = result.current[0]!.id;
        act(() => result.current[1]({
            type: 'removeNode',
            nodeId,
        }));
        const undo = result.current[2].undo;
        const dispatch = result.current[1];
        onChange.mockClear();
        rerender({editable: false});
        act(() => {
            undo();
            dispatch({
                type: 'insertNode',
                parentId: null,
                index: 0,
                node: toNoCodeEditorNode(expression)!,
            });
        });
        expect(result.current[0]).toBeNull();
        expect(result.current[2].canUndo).toBe(false);
        expect(onChange).not.toHaveBeenCalled();
        rerender({editable: true});
        act(() => result.current[2].undo());
        expect(toNoCodeOperand(result.current[0])).toEqual(expression);
    });
});

it('keeps authoring metadata at the root across wrapping, replacement, deletion and undo', () => {
    const metadata = {
        notes: 'Interner Hinweis',
        testData: {
            version: 1 as const,
            elementValues: {},
            sourceValues: {},
            visibility: {},
        },
    };
    const operand: NoCodeOperand = {
        ...expression,
        editorMetadata: metadata,
    };
    const onChange = vi.fn();
    const {result} = renderHook(() => useNoCodeEditorStore(operand, onChange));
    act(() => result.current[1]({
        type: 'wrapNode',
        nodeId: result.current[0]!.id,
        wrapperId: 'outer',
        operatorIdentifier: 'not',
    }));
    const wrapped = toNoCodeOperand(result.current[0]);
    expect(wrapped?.editorMetadata).toEqual(metadata);
    expect(wrapped?.type === 'NoCodeExpression' && wrapped.operands?.[0]?.editorMetadata).toBeUndefined();
    act(() => result.current[1]({
        type: 'replaceNode',
        nodeId: 'outer',
        node: toNoCodeEditorNode({
            type: 'NoCodeStaticValue',
            value: 'new',
        })!,
    }));
    expect(onChange).toHaveBeenLastCalledWith({
        type: 'NoCodeStaticValue',
        value: 'new',
        editorMetadata: metadata,
    });
    act(() => result.current[1]({
        type: 'setEditorMetadata',
        metadata: {
            ...metadata,
            notes: 'Geändert',
        },
    }));
    expect(result.current[0]?.editorMetadata?.notes).toBe('Geändert');
    act(() => result.current[2].undo());
    expect(result.current[0]?.editorMetadata).toEqual(metadata);
    act(() => result.current[1]({
        type: 'removeNode',
        nodeId: result.current[0]!.id,
    }));
    expect(onChange).toHaveBeenLastCalledWith(null);
    act(() => result.current[2].undo());
    expect(result.current[0]?.editorMetadata).toEqual(metadata);
});
