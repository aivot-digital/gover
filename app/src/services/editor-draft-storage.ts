/**
 * Unsaved editor changes kept in the browser, so that they survive an expired session, a reload or a closed tab.
 * `base` is the stored state the changes started from; it is needed to merge them with a newer stored state.
 */
export interface EditorDraft<T> {
    userId: string;
    key: string;
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

let database: Promise<IDBDatabase> | null = null;

function openDatabase(): Promise<IDBDatabase> {
    if (database == null) {
        database = new Promise<IDBDatabase>((resolve, reject) => {
            const request = indexedDB.open(DATABASE_NAME, 1);
            request.onupgradeneeded = () => {
                request.result.createObjectStore(STORE_NAME, {keyPath: ['userId', 'key']});
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

export const EditorDraftStorage = {
    async load<T>(userId: string, key: string): Promise<EditorDraft<T> | null> {
        const draft = await run<EditorDraft<T> | undefined>('readonly', (store) => store.get([userId, key]));
        if (draft == null) {
            return null;
        }
        if (Date.now() - draft.savedAt > DRAFT_MAX_AGE) {
            await EditorDraftStorage.remove(userId, key);
            return null;
        }
        return draft;
    },

    async save<T>(draft: EditorDraft<T>): Promise<void> {
        await run('readwrite', (store) => store.put(draft));
    },

    async remove(userId: string, key: string): Promise<void> {
        await run('readwrite', (store) => store.delete([userId, key]));
    },

    /** Removes the drafts of all users, e.g. on an explicit logout, so that nothing stays behind on a shared computer. */
    async clear(): Promise<void> {
        await run('readwrite', (store) => store.clear());
    },
};
