import {useCallback, useEffect, useMemo, useRef, useState} from 'react';

export interface ProcessChatEditor {
    nodeId: number;
    save: () => Promise<void>;
    refresh: () => Promise<void>;
}

/** Keeps the editor locked until the database state is known after a tool turn. */
export function useProcessAiChat(
    contextKey: string,
    refresh: (editor: ProcessChatEditor | null, isCurrent: () => boolean) => Promise<void>,
) {
    const editorRef = useRef<ProcessChatEditor | null>(null);
    const context = useMemo(() => ({key: contextKey}), [contextKey]);
    const activeContext = useRef<typeof context | null>(context);
    activeContext.current = context;
    const [busy, setBusy] = useState(false);
    const [reloadFailed, setReloadFailed] = useState(false);

    useEffect(() => {
        activeContext.current = context;
        setBusy(false);
        setReloadFailed(false);
        return () => { activeContext.current = null; };
    }, [context]);

    const registerEditor = useCallback((editor: ProcessChatEditor) => {
        editorRef.current = editor;
        return () => {
            if (editorRef.current === editor) editorRef.current = null;
        };
    }, []);

    const onThinking = useCallback((thinking: boolean) => {
        if (activeContext.current === context) setBusy(thinking);
    }, [context]);

    const beforeSend = useCallback(async () => {
        await editorRef.current?.save();
    }, []);

    const afterTurn = useCallback(async () => {
        const isCurrent = () => activeContext.current === context;
        try {
            await refresh(editorRef.current, isCurrent);
            if (isCurrent()) setReloadFailed(false);
        } catch (error) {
            if (isCurrent()) setReloadFailed(true);
            throw error;
        }
    }, [context, refresh]);

    const retry = useCallback(async () => {
        onThinking(true);
        try {
            await afterTurn();
        } catch {
            // Keep the retry action and editing lock until all data has been reloaded.
        } finally {
            onThinking(false);
        }
    }, [afterTurn, onThinking]);

    return {busy, reloadFailed, locked: busy || reloadFailed, registerEditor, beforeSend, afterTurn, onThinking, retry};
}
