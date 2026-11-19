import type {EditorSaveCheckpoint} from '../utils/editor-save-rebase';

/**
 * Unsaved editor changes kept in the browser, so that they survive an expired session, a reload or a closed tab.
 * `base` is the stored state the changes started from; it is needed to merge them with a newer stored state.
 */
export interface EditorDraft<T, Context = unknown> {
    userId: string;
    key: string;
    /** The tab that edits the draft. Each tab keeps its own draft, so tabs editing the same object cannot replace or remove each other's. */
    tabId: string;
    base: T;
    /** The lock version of the stored object that `base` belongs to. */
    baseLockVersion: number | null;
    mine: T;
    /** The submitted snapshot may already be stored by the time this document returns. */
    saveCheckpoint?: EditorSaveCheckpoint<T>;
    /** Work still open in a nested editor, saved together with its enclosing draft. */
    context?: Context;
    savedAt: number;
}

const DATABASE_NAME = 'prosuna_editor_drafts';
const STORE_NAME = 'drafts';
// Drafts are a safety net for interruptions, not a place to park work, so forgotten ones are removed eventually.
const DRAFT_MAX_AGE = 14 * 24 * 60 * 60_000;
// Same lifetime as the backend's OIDC login flow.
const RESERVATION_MAX_AGE = 10 * 60_000;
const TAB_LOCK_PREFIX = 'prosuna_editor_draft_tab/';

interface StoredDraft<T, Context = unknown> extends EditorDraft<T, Context> {
    // Existing v2 records predate this flag and follow the normal owner-lock convention.
    shareable?: boolean;
    reservation?: {id: string; expiresAt: number};
}

// Each document, including a duplicated tab, owns a separate working copy.
const tabId = typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function' ?
    crypto.randomUUID() :
    `${Date.now()}-${Math.random().toString(36).slice(2)}`;

// Only publish adoptable drafts once their owner actually holds its lock.
const ownerLocks = new Promise<LockManager | null>((resolve) => {
    const locks = typeof navigator !== 'undefined' ? navigator.locks : undefined;
    if (!locks) {
        resolve(null);
        return;
    }
    locks.request(TAB_LOCK_PREFIX + tabId, {ifAvailable: true}, (lock) => {
        resolve(lock ? locks : null);
        if (lock) {
            return new Promise<void>(() => undefined);
        }
    }).catch(() => resolve(null));
}).catch(() => null);

let database: Promise<IDBDatabase> | null = null;

function openDatabase(): Promise<IDBDatabase> {
    if (database == null) {
        const request = indexedDB.open(DATABASE_NAME, 2);
        let blocked = false;
        database = new Promise<IDBDatabase>((resolve, reject) => {
            request.onblocked = () => {
                // Further opens would queue behind this request. Fail promptly until the blocking connection closes.
                blocked = true;
                reject(new DOMException('Draft database upgrade blocked by another tab', 'InvalidStateError'));
            };
            request.onupgradeneeded = () => {
                // Version 1 kept one draft per object for all tabs.
                if (request.result.objectStoreNames.contains(STORE_NAME)) {
                    request.result.deleteObjectStore(STORE_NAME);
                }
                request.result.createObjectStore(STORE_NAME, {keyPath: ['userId', 'key', 'tabId']});
            };
            request.onsuccess = () => {
                const db = request.result;
                db.onversionchange = () => {
                    db.close();
                    database = null;
                };
                db.onclose = () => { database = null; };
                if (blocked) {
                    database = Promise.resolve(db);
                }
                resolve(db);
            };
            request.onerror = () => {
                database = null;
                reject(request.error);
            };
        });
    }
    return database;
}

/** Request success can still be followed by a transaction abort. Publish results only after commit. */
async function write<R>(operation: (store: IDBObjectStore, setResult: (value: R) => void) => void): Promise<R> {
    const db = await openDatabase();
    return new Promise<R>((resolve, reject) => {
        const transaction = db.transaction(STORE_NAME, 'readwrite');
        let result: R;
        transaction.oncomplete = () => resolve(result);
        transaction.onabort = () => reject(transaction.error ?? new DOMException('Draft transaction aborted', 'AbortError'));
        try {
            operation(transaction.objectStore(STORE_NAME), (value) => {
                result = value;
            });
        } catch (error) {
            transaction.abort();
            reject(error);
        }
    });
}

function isExpired(draft: StoredDraft<unknown>): boolean {
    return Date.now() - draft.savedAt > DRAFT_MAX_AGE;
}

function canAdopt(draft: StoredDraft<unknown>, returnId?: string): boolean {
    const reservation = draft.reservation;
    return draft.shareable !== false && (
        !reservation || reservation.expiresAt <= Date.now() || reservation.id === returnId
    );
}

function listDrafts<T, Context>(userId: string, key: string): Promise<StoredDraft<T, Context>[]> {
    return write((store, setResult) => {
        // The final key component is always a string; an array sorts after every string in IndexedDB.
        const request = store.getAll(IDBKeyRange.bound([userId, key], [userId, key, []]));
        request.onsuccess = () => {
            const drafts: StoredDraft<T, Context>[] = [];
            for (const draft of request.result as StoredDraft<T, Context>[]) {
                if (isExpired(draft)) {
                    store.delete([userId, key, draft.tabId]);
                } else {
                    drafts.push(draft);
                }
            }
            setResult(drafts);
        };
    });
}

function adopt<T, Context>(userId: string, key: string, sourceTab: string, returnId?: string): Promise<EditorDraft<T, Context> | null> {
    return write((store, setResult) => {
        const destination = store.get([userId, key, tabId]);
        const sourceKey = [userId, key, sourceTab];
        const source = store.get(sourceKey);
        // IDB delivers both results in request order within this single transaction.
        source.onsuccess = () => {
            const own: StoredDraft<T, Context> | undefined = destination.result;
            if (own && !isExpired(own)) {
                setResult(own);
                return;
            }
            const current: StoredDraft<T, Context> | undefined = source.result;
            if (!current || isExpired(current) || !canAdopt(current, returnId)) {
                setResult(null);
                return;
            }

            // The source may have been deleted, reserved, or edited since it was listed.
            // Move its current contents and consume the reservation in the same commit.
            const adopted: StoredDraft<T, Context> = {...current, tabId, shareable: true};
            delete adopted.reservation;
            store.put(adopted);
            store.delete(sourceKey);
            setResult(adopted);
        };
    });
}

async function loadDraft<T, Context>(userId: string, key: string, returnId?: string): Promise<EditorDraft<T, Context> | null> {
    const locks = await ownerLocks;
    const drafts = await listDrafts<T, Context>(userId, key);
    const own = drafts.find((draft) => draft.tabId === tabId);
    if (own || !locks) {
        return own ?? null;
    }

    const candidates = drafts.filter((draft) => canAdopt(draft, returnId)).sort((a, b) => b.savedAt - a.savedAt);
    const reserved = returnId && candidates.find((draft) =>
        draft.reservation?.id === returnId && draft.reservation.expiresAt > Date.now());
    if (reserved) {
        candidates.splice(candidates.indexOf(reserved), 1);
        candidates.unshift(reserved);
    }
    for (const candidate of candidates) {
        const draft = await locks.request(TAB_LOCK_PREFIX + candidate.tabId, {ifAvailable: true}, (lock) =>
            lock ? adopt<T, Context>(userId, key, candidate.tabId, returnId) : null);
        if (draft) {
            return draft;
        }
    }
    return null;
}

const pendingLoads = new Map<string, Promise<EditorDraft<unknown> | null>>();

export const EditorDraftStorage = {
    /**
     * Loads this tab's draft, then a matching login reservation, then the newest eligible draft of a closed tab.
     */
    async load<T, Context = unknown>(userId: string, key: string, returnId?: string): Promise<EditorDraft<T, Context> | null> {
        // Overlapping mounts in the same document would compete for the source lock and one could miss its draft.
        const scope = JSON.stringify([userId, key]);
        const previous = pendingLoads.get(scope);
        const read = () => loadDraft<T, Context>(userId, key, returnId);
        const pending = previous ? previous.then(read, read) : read();
        pendingLoads.set(scope, pending);
        try {
            return await pending;
        } finally {
            if (pendingLoads.get(scope) === pending) {
                pendingLoads.delete(scope);
            }
        }
    },

    async save<T, Context = unknown>(draft: Omit<EditorDraft<T, Context>, 'tabId'>): Promise<void> {
        const locks = await ownerLocks;
        await write<void>((store) => {
            const request = store.get([draft.userId, draft.key, tabId]);
            request.onsuccess = () => {
                // A late write from this editor must preserve a reservation made before navigating to login.
                const previous: StoredDraft<T> | undefined = request.result;
                const reservation = previous?.reservation;
                store.put({
                    ...draft, tabId, shareable: locks != null,
                    reservation: reservation && reservation.expiresAt > Date.now() ? reservation : undefined,
                });
            };
        });
    },

    async reserve(userId: string, key: string, returnId: string): Promise<boolean> {
        if (!await ownerLocks || !returnId) {
            return false;
        }
        return write((store, setResult) => {
            const request = store.get([userId, key, tabId]);
            request.onsuccess = () => {
                const draft: StoredDraft<unknown> | undefined = request.result;
                if (!draft || isExpired(draft) || draft.shareable === false) {
                    setResult(false);
                    return;
                }
                store.put({...draft, reservation: {id: returnId, expiresAt: Date.now() + RESERVATION_MAX_AGE}});
                setResult(true);
            };
        });
    },

    /** Removes the draft of this tab only. */
    async remove(userId: string, key: string): Promise<void> {
        await ownerLocks;
        await write<void>((store) => {
            store.delete([userId, key, tabId]);
        });
    },

    /** Removes the drafts of all users, e.g. on an explicit logout, so that nothing stays behind on a shared computer. */
    async clear(): Promise<void> {
        await ownerLocks;
        await write<void>((store) => {
            store.clear();
        });
    },
};
