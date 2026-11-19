import {useCallback, useLayoutEffect, useMemo} from 'react';
import {useAppSelector} from './use-app-selector';
import {selectUser} from '../slices/user-slice';
import {getEditorDraftReturnId, registerEditorDraftLoginHandler} from '../services/editor-draft-login';
import {type EditorDraft, EditorDraftStorage} from '../services/editor-draft-storage';

// Short enough that hardly any work is lost, long enough not to write on every keystroke.
const DRAFT_SAVE_DELAY = 1000;

interface EditorDraftProps<T, Context> {
    /** Identifies the edited object, e.g. `process-node/17/form-layout`. Must stay `null` until the object is loaded. */
    draftKey: string | null;
    /** The stored state the editor started from. May be `null`, e.g. for a form that was never saved. */
    base: T | null;
    baseLockVersion: number | null | undefined;
    edited: T | null;
    context?: Context;
    hasChanged: boolean;
    /**
     * Must stay false until the editor has decided about an existing draft. Otherwise the freshly loaded, unchanged
     * state would remove the draft before it could be offered.
     */
    enabled: boolean;
}

type DraftSnapshot<T, Context> = Pick<EditorDraft<T, Context>, 'base' | 'baseLockVersion' | 'mine' | 'context'>;

function createDraftSession<T, Context>(userId: string, key: string) {
    let pending: DraftSnapshot<T, Context> | undefined;
    let timer: number | undefined;
    let lastOperation: Promise<unknown> | undefined;
    let stored = false;
    let hadContext = false;
    let leaving = false;
    let revision = 0;

    // Opening the database, taking ownership of a draft, and committing writes are asynchronous. Keep them in
    // editor order even when a discard or a login redirect starts while a previous operation is still waiting.
    function inOrder<R>(operation: () => Promise<R>): Promise<R> {
        const current = lastOperation == null ? operation() : lastOperation.then(operation, operation);
        lastOperation = current;
        const finished = () => {
            if (lastOperation === current) {
                lastOperation = undefined;
            }
        };
        current.then(finished, finished);
        return current;
    }

    function cancelPending() {
        window.clearTimeout(timer);
        pending = undefined;
    }

    function flush() {
        const snapshot = pending;
        cancelPending();
        if (snapshot != null) {
            return inOrder(async () => {
                await EditorDraftStorage.save({...snapshot, userId, key, savedAt: Date.now()});
                stored = true;
            });
        }
    }

    function schedule(snapshot: DraftSnapshot<T, Context>) {
        if (leaving) {
            return;
        }
        revision++;
        cancelPending();
        pending = snapshot;
        const closedNestedEditor = hadContext && snapshot.context === undefined;
        hadContext = snapshot.context !== undefined;
        if (closedNestedEditor) {
            // Applying or discarding the nested edits invalidates their previous backup immediately.
            flush()?.catch(console.error);
        } else {
            timer = window.setTimeout(() => flush()?.catch(console.error), DRAFT_SAVE_DELAY);
        }
    }

    function remove() {
        return inOrder(async () => {
            try {
                await EditorDraftStorage.remove(userId, key);
                stored = false;
            } catch (error) {
                // A browser without working draft storage can still discard changes kept only in memory.
                if (stored) {
                    throw error;
                }
            }
        });
    }

    return {
        schedule,
        cancelPending,
        async clearDraft() {
            if (!leaving) {
                revision++;
                cancelPending();
                hadContext = false;
                await remove();
            }
        },
        async discardForNavigation() {
            const snapshot = pending;
            leaving = true;
            cancelPending();
            try {
                await remove();
            } catch (error) {
                leaving = false;
                if (snapshot != null) {
                    schedule(snapshot);
                }
                throw error;
            }
        },
        load: () => inOrder(async () => {
            if (leaving) {
                return null;
            }
            const draft = await EditorDraftStorage.load<T, Context>(userId, key, getEditorDraftReturnId());
            stored = draft != null;
            hadContext = draft?.context !== undefined;
            return draft;
        }),
        async reserve(returnId: string) {
            // An edit or a completed nested apply can arrive while a preceding transaction is committing.
            // In that case flush and reserve the newer snapshot before letting the document navigate away.
            for (;;) {
                const reservingRevision = revision;
                await flush();
                const reserved = await inOrder(async () => !leaving && await EditorDraftStorage.reserve(userId, key, returnId));
                if (leaving || revision === reservingRevision) {
                    return !leaving && reserved;
                }
            }
        },
        async stopForLogout() {
            leaving = true;
            cancelPending();
            await lastOperation?.catch(console.error);
        },
    };
}

/**
 * Keeps the unsaved changes of an editor in the browser while they exist and removes them once they are saved or
 * reverted, so they can be offered again after an expired session or a reload.
 */
export function useEditorDraft<T, Context = unknown>(props: EditorDraftProps<T, Context>) {
    const {draftKey, base, baseLockVersion, edited, context, hasChanged, enabled} = props;
    const userId = useAppSelector(selectUser)?.id;
    const session = useMemo(() => userId == null || draftKey == null ? null : createDraftSession<T, Context>(userId, draftKey), [userId, draftKey]);

    useLayoutEffect(() => {
        if (!enabled || session == null || edited == null) {
            return;
        }

        if (!hasChanged) {
            session.clearDraft().catch(console.error);
        } else {
            // A null base represents the empty stored state of an object that has never been saved.
            session.schedule({base: base as T, baseLockVersion: baseLockVersion ?? null, mine: edited, context});
        }
        return session.cancelPending;
    }, [enabled, session, base, baseLockVersion, edited, context, hasChanged]);

    useLayoutEffect(() => {
        if (session != null) {
            return registerEditorDraftLoginHandler(session.reserve, session.stopForLogout);
        }
    }, [session]);

    const loadDraft = useCallback(async (): Promise<EditorDraft<T, Context> | null> => {
        try {
            return await session?.load() ?? null;
        } catch (error) {
            console.error(error);
            return null;
        }
    }, [session]);

    const clearDraft = useCallback(async () => {
        await session?.clearDraft();
    }, [session]);

    const discardForNavigation = useCallback(async () => {
        await session?.discardForNavigation();
    }, [session]);

    return {
        loadDraft,
        clearDraft,
        discardForNavigation,
        isReady: userId != null,
    };
}
