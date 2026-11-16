/**
 * Unsaved editor changes kept in the browser, so that they survive an expired session, a reload or a closed tab.
 * `base` is the stored state the changes started from; it is needed to merge them with a newer stored state.
 */
export interface EditorDraft<T> {
    userId: string;
    key: string;
    /** The tab that edits the draft. Each tab keeps its own draft, so tabs editing the same object cannot replace or remove each other's. */
    tabId: string;
    base: T;
    /** The lock version of the stored object that `base` belongs to. */
    baseLockVersion: number | null;
    mine: T;
    savedAt: number;
}

const DATABASE_NAME = 'prosuna_editor_drafts';
const STORE_NAME = 'drafts';
// Drafts are a safety net for interruptions, not a place to park work, so forgotten ones are removed eventually.
const DRAFT_MAX_AGE = 14 * 24 * 60 * 60_000;
const TAB_LOCK_PREFIX = 'prosuna_editor_draft_tab/';

// A new ID per page load: after a reload or in a duplicated tab, the drafts of the previous page are recovered as drafts of a closed tab.
const tabId = typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function' ?
    crypto.randomUUID() :
    `${Date.now()}-${Math.random().toString(36).slice(2)}`;

// Held until the page is closed or reloaded, so that other tabs can tell that its drafts are still being edited.
if (typeof navigator.locks !== 'undefined') {
    navigator.locks
        .request(TAB_LOCK_PREFIX + tabId, () => new Promise<void>(() => undefined))
        .catch(console.error);
}

let database: Promise<IDBDatabase> | null = null;

function openDatabase(): Promise<IDBDatabase> {
    if (database == null) {
        database = new Promise<IDBDatabase>((resolve, reject) => {
            const request = indexedDB.open(DATABASE_NAME, 2);
            request.onupgradeneeded = () => {
                // Version 1 kept one draft per object for all tabs.
                if (request.result.objectStoreNames.contains(STORE_NAME)) {
                    request.result.deleteObjectStore(STORE_NAME);
                }
                request.result.createObjectStore(STORE_NAME, {keyPath: ['userId', 'key', 'tabId']});
            };
            request.onsuccess = () => resolve(request.result);
            request.onerror = () => reject(request.error);
        });
        // A failed open, e.g. in a private window, is retried with the next access.
        database.catch(() => {
            database = null;
        });
    }
    return database;
}

async function run<R>(mode: IDBTransactionMode, operation: (store: IDBObjectStore) => IDBRequest<R>): Promise<R> {
    const db = await openDatabase();
    return await new Promise<R>((resolve, reject) => {
        const request = operation(db.transaction(STORE_NAME, mode).objectStore(STORE_NAME));
        request.onsuccess = () => resolve(request.result);
        request.onerror = () => reject(request.error);
    });
}

/** The IDs of the tabs that are still open, or `null` if the browser cannot tell. */
async function getOpenTabIds(): Promise<Set<string> | null> {
    if (typeof navigator.locks === 'undefined') {
        return null;
    }
    const {held = []} = await navigator.locks.query();
    return new Set(held
        .map((lock) => lock.name ?? '')
        .filter((name) => name.startsWith(TAB_LOCK_PREFIX))
        .map((name) => name.slice(TAB_LOCK_PREFIX.length)));
}

function deleteDraft(draft: EditorDraft<unknown>): Promise<undefined> {
    return run('readwrite', (store) => store.delete([draft.userId, draft.key, draft.tabId]));
}

export const EditorDraftStorage = {
    /**
     * Loads the draft of this tab or, if there is none, takes over the newest draft of a closed tab, e.g. one left
     * behind by an expired session. Drafts of tabs that are still open stay theirs.
     */
    async load<T>(userId: string, key: string): Promise<EditorDraft<T> | null> {
        const all = await run<EditorDraft<T>[]>('readonly', (store) => store.getAll());
        const drafts: EditorDraft<T>[] = [];
        for (const draft of all) {
            if (draft.userId !== userId || draft.key !== key) {
                continue;
            }
            if (Date.now() - draft.savedAt > DRAFT_MAX_AGE) {
                await deleteDraft(draft);
            } else {
                drafts.push(draft);
            }
        }

        const own = drafts.find((draft) => draft.tabId === tabId);
        if (own != null) {
            return own;
        }

        // shortcut: without the Web Locks API every other draft counts as closed, so open tabs may take over each other's draft; all supported browsers have it.
        const openTabIds = await getOpenTabIds();
        const orphan = drafts
            .filter((draft) => openTabIds == null || !openTabIds.has(draft.tabId))
            .sort((a, b) => b.savedAt - a.savedAt)[0];
        if (orphan == null) {
            return null;
        }

        const adopted = {...orphan, tabId};
        await EditorDraftStorage.save<T>(adopted);
        await deleteDraft(orphan);
        return adopted;
    },

    async save<T>(draft: Omit<EditorDraft<T>, 'tabId'>): Promise<void> {
        await run('readwrite', (store) => store.put({...draft, tabId}));
    },

    /** Removes the draft of this tab only. */
    async remove(userId: string, key: string): Promise<void> {
        await run('readwrite', (store) => store.delete([userId, key, tabId]));
    },

    /** Removes the drafts of all users, e.g. on an explicit logout, so that nothing stays behind on a shared computer. */
    async clear(): Promise<void> {
        await run('readwrite', (store) => store.clear());
    },
};
