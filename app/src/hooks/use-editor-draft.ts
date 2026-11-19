import {useCallback, useLayoutEffect, useMemo, useReducer} from 'react';
import {useAppSelector} from './use-app-selector';
import {selectUser} from '../slices/user-slice';
import {getEditorDraftReturnId, registerEditorDraftLoginHandler} from '../services/editor-draft-login';
import {type EditorDraft, EditorDraftStorage} from '../services/editor-draft-storage';
import {createEditorDraftAccess, type EditorDraftManualSave} from './editor-draft-access';

export type {EditorDraftManualSave} from './editor-draft-access';

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
    /** Advances only when the user edits the nested working copy, including an edit made during a save. */
    contextRevision?: number;
    hasChangesWithoutContext?: boolean;
    saveCheckpoint?: EditorDraft<T>['saveCheckpoint'];
    hasChanged: boolean;
    /**
     * Must stay false until the editor has decided about an existing draft. Otherwise the freshly loaded, unchanged
     * state would remove the draft before it could be offered.
     */
    enabled: boolean;
}

type DraftSnapshot<T, Context> = Pick<EditorDraft<T, Context>, 'base' | 'baseLockVersion' | 'mine' | 'context' | 'saveCheckpoint'>;

function createDraftSession<T, Context>(userId: string, key: string) {
    let pending: DraftSnapshot<T, Context> | undefined;
    let timer: number | undefined;
    let lastOperation: Promise<unknown> | undefined;
    let stored = false;
    let readFailed = false;
    let hadContext = false;
    let leaving = false;
    let mounted = false;
    let revision = 0;
    let preparingSave: AbortController | undefined;
    let contextRevision: number | undefined;
    let revokedContextRevision: number | undefined;
    const access = createEditorDraftAccess(userId, () => {
        cancelPending();
        stored = false;
        hadContext = false;
        revokedContextRevision = contextRevision;
    });

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
        const signal = access.signal();
        const snapshot = pending;
        cancelPending();
        if (snapshot != null) {
            return inOrder(async () => {
                const generation = await access.current(signal);
                if (readFailed || generation == null) return;
                await EditorDraftStorage.save({...snapshot, userId, key, savedAt: Date.now()}, generation);
                if (access.active(signal)) stored = true;
            });
        }
    }

    function schedule(snapshot: DraftSnapshot<T, Context>) {
        if (leaving || readFailed || !access.active(access.signal())) {
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

    function remove(automatic = false) {
        const signal = access.signal();
        if (!access.active(signal)) return Promise.resolve();
        return inOrder(async () => {
            // A failed lookup can hide an existing draft. Only an explicit discard may remove it.
            if (automatic && readFailed) return;
            const generation = await access.current(signal);
            if (generation == null) return;
            try {
                await EditorDraftStorage.remove(userId, key, generation);
                if (access.active(signal)) {
                    stored = false;
                    readFailed = false;
                }
            } catch (error) {
                // A browser without working draft storage can still discard changes kept only in memory.
                if (stored) {
                    throw error;
                }
            }
        });
    }

    function cancelSavePreparation() {
        preparingSave?.abort();
        preparingSave = undefined;
    }

    async function isRecoveryCurrent(signal = access.signal()) {
        if (!mounted || leaving || access.recoverySignal.aborted) return false;
        const generation = await access.current(signal);
        if (generation == null) return false;
        try {
            if (await EditorDraftStorage.isCurrentGeneration(generation)) return mounted && !leaving && access.active(signal);
            if (access.active(signal)) access.revoke();
        } catch (error) {
            // Leave the existing row alone if we cannot verify it before publishing or applying a restore decision.
            if (access.active(signal)) {
                readFailed = true;
                cancelPending();
            }
            console.error(error);
        }
        return false;
    }

    return {
        schedule,
        cancelPending,
        observeContext(revision?: number) {
            contextRevision = revision;
            return revision !== undefined && revision === revokedContextRevision;
        },
        draftLogoutSignal: access.recoverySignal,
        register() {
            mounted = true;
            void access.current(access.signal());
            return () => {
                mounted = false;
                cancelSavePreparation();
            };
        },
        isRecoveryCurrent,
        async beginManualSave(): Promise<EditorDraftManualSave | null> {
            if (!mounted || leaving) return null;
            cancelSavePreparation();
            const preparation = new AbortController();
            preparingSave = preparation;
            try {
                const cancelled = new Promise<EditorDraftManualSave>((resolve) => {
                    preparation.signal.addEventListener('abort', () => resolve({
                        signal: preparation.signal, generation: null, resumesDrafts: false,
                    }), {once: true});
                });
                return await Promise.race([cancelled, access.beginManualSave(preparation.signal)]);
            } finally {
                if (preparingSave === preparation) preparingSave = undefined;
            }
        },
        async confirmManualSave(token: EditorDraftManualSave) {
            if (!mounted || leaving || !await access.confirmManualSave(token)) return false;
            readFailed = false;
            return mounted && !leaving;
        },
        async clearDraft(automatic = false) {
            if (!leaving) {
                revision++;
                cancelPending();
                hadContext = false;
                await remove(automatic);
            }
        },
        async discardForNavigation() {
            cancelSavePreparation();
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
        load() {
            const signal = access.signal();
            if (!access.active(signal)) return Promise.resolve(null);
            return inOrder(async () => {
                const generation = await access.current(signal);
                if (leaving || generation == null) return null;
                try {
                    const draft = await EditorDraftStorage.load<T, Context>(userId, key, getEditorDraftReturnId(), generation);
                    if (!access.active(signal)) return null;
                    if (draft && !await isRecoveryCurrent(signal)) return null;
                    readFailed = false;
                    stored = draft != null;
                    hadContext = draft?.context !== undefined;
                    return draft;
                } catch (error) {
                    if (access.active(signal)) {
                        readFailed = true;
                        cancelPending();
                    }
                    throw error;
                }
            });
        },
        async reserve(returnId: string) {
            // A PUT still verifying its account has no submitted checkpoint to reserve for the redirect.
            cancelSavePreparation();
            // An edit or a completed nested apply can arrive while a preceding transaction is committing.
            // In that case flush and reserve the newer snapshot before letting the document navigate away.
            const signal = access.signal();
            if (!access.active(signal)) return false;
            for (;;) {
                const reservingRevision = revision;
                await flush();
                const reserved = await inOrder(async () => {
                    const generation = await access.current(signal);
                    return !leaving && !readFailed && generation != null && await EditorDraftStorage.reserve(userId, key, returnId, generation);
                });
                if (leaving || !access.active(signal) || revision === reservingRevision) {
                    return !leaving && access.active(signal) && reserved;
                }
            }
        },
        async stopForLogout(id?: string) {
            if (access.revoke(id)) cancelSavePreparation();
            await lastOperation?.catch(console.error);
        },
    };
}

/**
 * Keeps the unsaved changes of an editor in the browser while they exist and removes them once they are saved or
 * reverted, so they can be offered again after an expired session or a reload.
 */
export function useEditorDraft<T, Context = unknown>(props: EditorDraftProps<T, Context>) {
    const {draftKey, base, baseLockVersion, edited, context, contextRevision, hasChangesWithoutContext,
        saveCheckpoint, hasChanged, enabled} = props;
    const userId = useAppSelector(selectUser)?.id;
    const session = useMemo(() => userId == null || draftKey == null ? null : createDraftSession<T, Context>(userId, draftKey), [userId, draftKey]);
    const [resumed, onResume] = useReducer((value: number) => value + 1, 0);

    useLayoutEffect(() => {
        if (session != null) {
            const leave = session.register();
            const unregister = registerEditorDraftLoginHandler(session.reserve, session.stopForLogout);
            return () => {
                leave();
                unregister();
            };
        }
    }, [session]);

    useLayoutEffect(() => {
        const suppressContext = session?.observeContext(contextRevision);
        if (!enabled || session == null || edited == null) {
            return;
        }

        const changed = suppressContext ? hasChangesWithoutContext ?? hasChanged : hasChanged;
        if (!changed && saveCheckpoint?.phase !== 'pending') {
            session.clearDraft(true).catch(console.error);
        } else {
            // A null base represents the empty stored state of an object that has never been saved.
            session.schedule({base: base as T, baseLockVersion: baseLockVersion ?? null, mine: edited,
                context: suppressContext ? undefined : context, saveCheckpoint});
        }
        return session.cancelPending;
    }, [enabled, session, base, baseLockVersion, edited, context, contextRevision, hasChangesWithoutContext,
        saveCheckpoint, hasChanged, resumed]);

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

    const beginManualSave = useCallback(() => session?.beginManualSave() ?? Promise.resolve(null), [session]);
    const isDraftRecoveryCurrent = useCallback(() => session?.isRecoveryCurrent() ?? Promise.resolve(false), [session]);
    const confirmManualSave = useCallback(async (token: EditorDraftManualSave) => {
        if (await session?.confirmManualSave(token)) onResume();
    }, [session]);

    return {
        loadDraft,
        clearDraft,
        discardForNavigation,
        draftLogoutSignal: session?.draftLogoutSignal,
        isDraftRecoveryCurrent,
        beginManualSave,
        confirmManualSave,
        isReady: userId != null,
    };
}
